package com.example.data.zip

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.GameEntryEntity
import com.example.data.local.PackageArchiveEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipEntrySummary(
    val path: String,
    val uncompressedBytes: Long,
    val compressedBytes: Long,
    val isDirectory: Boolean
)

data class InspectedPackage(
    val fileName: String,
    val format: String,
    val totalEntries: Int,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val hasManifest: Boolean,
    val dexCount: Int,
    val nativeAbis: List<String>,
    val entries: List<ZipEntrySummary>,
    val containsZipPlayBackup: Boolean
) {
    val compressionRatioPercent: Int
        get() = if (uncompressedBytes > 0L) {
            (((uncompressedBytes - compressedBytes).coerceAtLeast(0L) * 100.0) / uncompressedBytes).toInt()
        } else {
            0
        }

    fun toArchiveEntity(): PackageArchiveEntity {
        val preview = entries
            .filter { !it.isDirectory }
            .take(18)
            .joinToString("\n") { "${it.path}|${it.uncompressedBytes}" }
        return PackageArchiveEntity(
            fileName = fileName,
            format = format,
            totalEntries = totalEntries,
            compressedBytes = compressedBytes,
            uncompressedBytes = uncompressedBytes,
            hasManifest = hasManifest,
            dexCount = dexCount,
            nativeAbis = nativeAbis.joinToString(", "),
            entriesPreview = preview,
            importedAt = System.currentTimeMillis()
        )
    }
}

data class BackupGameState(
    val id: String,
    val isFavorite: Boolean,
    val highScore: Int,
    val playCount: Int
)

object ZipPackageInspector {

    private const val BACKUP_JSON_ENTRY = "zipplay_backup.json"

    suspend fun inspectUri(context: Context, uri: Uri): Result<InspectedPackage> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = resolveFileName(context, uri)
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Não foi possível abrir o arquivo selecionado.")
            stream.use { input ->
                inspectStream(fileName, input)
            }
        }
    }

    fun inspectBytes(fileName: String, bytes: ByteArray): InspectedPackage {
        return ByteArrayInputStream(bytes).use { input ->
            inspectStream(fileName, input)
        }
    }

    private fun inspectStream(fileName: String, inputStream: InputStream): InspectedPackage {
        val buffered = BufferedInputStream(inputStream, 65536)
        buffered.mark(65536)
        val peek = ByteArray(2)
        val peekRead = buffered.read(peek)
        buffered.reset()

        val isMzExecutable = peekRead == 2 && peek[0] == 0x4D.toByte() && peek[1] == 0x5A.toByte()
        if (isMzExecutable || fileName.endsWith(".exe", ignoreCase = true)) {
            return inspectExeStream(fileName, buffered)
        }

        val entries = mutableListOf<ZipEntrySummary>()
        var totalCompressed = 0L
        var totalUncompressed = 0L
        var hasManifest = false
        var dexCount = 0
        val abiSet = linkedSetOf<String>()
        var hasBackupJson = false
        val buffer = ByteArray(8192)

        ZipInputStream(buffered).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val safeName = entry.name.replace('\\', '/')
                var actualUncompressed = 0L
                if (!entry.isDirectory) {
                    var read = zis.read(buffer)
                    while (read != -1) {
                        actualUncompressed += read
                        read = zis.read(buffer)
                    }
                }
                val finalUncompressed = if (actualUncompressed > 0L) actualUncompressed else entry.size.coerceAtLeast(0L)
                val finalCompressed = entry.compressedSize.takeIf { it >= 0L } ?: finalUncompressed

                totalUncompressed += finalUncompressed
                totalCompressed += finalCompressed

                if (safeName.equals("AndroidManifest.xml", ignoreCase = true)) {
                    hasManifest = true
                }
                if (safeName.endsWith(".dex", ignoreCase = true) && !safeName.contains('/')) {
                    dexCount++
                }
                if (safeName.startsWith("lib/") && !entry.isDirectory) {
                    val segments = safeName.split('/')
                    if (segments.size >= 3 && segments[1].isNotBlank()) {
                        abiSet.add(segments[1])
                    }
                }
                if (safeName == BACKUP_JSON_ENTRY) {
                    hasBackupJson = true
                }

                if (entries.size < 250) {
                    entries.add(
                        ZipEntrySummary(
                            path = safeName,
                            uncompressedBytes = finalUncompressed,
                            compressedBytes = finalCompressed,
                            isDirectory = entry.isDirectory
                        )
                    )
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (entries.isEmpty()) {
            error("O arquivo não possui entradas ZIP, APK ou EXE válidas.")
        }

        val format = when {
            fileName.endsWith(".apk", ignoreCase = true) || hasManifest || dexCount > 0 -> "APK"
            else -> "ZIP"
        }

        return InspectedPackage(
            fileName = fileName,
            format = format,
            totalEntries = entries.size,
            compressedBytes = totalCompressed,
            uncompressedBytes = totalUncompressed,
            hasManifest = hasManifest,
            dexCount = dexCount,
            nativeAbis = abiSet.toList(),
            entries = entries,
            containsZipPlayBackup = hasBackupJson
        )
    }

    private fun inspectExeStream(fileName: String, inputStream: InputStream): InspectedPackage {
        val headerBuf = ByteArray(65536)
        var headerLen = 0
        var totalSize = 0L
        val chunk = ByteArray(8192)
        while (true) {
            val read = inputStream.read(chunk)
            if (read <= 0) break
            if (headerLen < headerBuf.size) {
                val copyLen = minOf(read, headerBuf.size - headerLen)
                System.arraycopy(chunk, 0, headerBuf, headerLen, copyLen)
                headerLen += copyLen
            }
            totalSize += read
        }

        if (headerLen < 64 || headerBuf[0] != 0x4D.toByte() || headerBuf[1] != 0x5A.toByte()) {
            error("Cabeçalho MZ/PE inválido para executável .exe.")
        }

        val bb = ByteBuffer.wrap(headerBuf, 0, headerLen).order(ByteOrder.LITTLE_ENDIAN)
        val peOffset = bb.getInt(0x3C)
        if (peOffset < 0 || peOffset + 24 > headerLen) {
            error("Arquivo .exe não possui cabeçalho PE legível nos primeiros 64 KB.")
        }

        val isPe = headerBuf[peOffset] == 'P'.code.toByte() &&
            headerBuf[peOffset + 1] == 'E'.code.toByte() &&
            headerBuf[peOffset + 2] == 0.toByte() &&
            headerBuf[peOffset + 3] == 0.toByte()
        if (!isPe) {
            error("Assinatura PE\\0\\0 ausente no executável.")
        }

        val machine = bb.getShort(peOffset + 4).toInt() and 0xFFFF
        val numSections = bb.getShort(peOffset + 6).toInt() and 0xFFFF
        val optionalHeaderSize = bb.getShort(peOffset + 20).toInt() and 0xFFFF
        val archLabel = when (machine) {
            0x8664 -> "x86_64 (PE32+)"
            0x014C -> "x86 (PE32)"
            0xAA64 -> "ARM64 (PE32+)"
            else -> "Arch 0x${machine.toString(16).uppercase()}"
        }

        val sectionsStart = peOffset + 24 + optionalHeaderSize
        val sectionEntries = mutableListOf<ZipEntrySummary>()
        sectionEntries.add(
            ZipEntrySummary(
                path = "PE_HEADER ($archLabel)",
                uncompressedBytes = optionalHeaderSize.toLong(),
                compressedBytes = optionalHeaderSize.toLong(),
                isDirectory = false
            )
        )

        var idx = 0
        while (idx < numSections && sectionsStart + (idx + 1) * 40 <= headerLen) {
            val offset = sectionsStart + idx * 40
            val rawName = headerBuf.copyOfRange(offset, offset + 8)
                .takeWhile { it != 0.toByte() }
                .toByteArray()
                .toString(Charsets.US_ASCII)
                .ifBlank { "section_$idx" }
            val virtualSize = bb.getInt(offset + 8).toLong() and 0xFFFFFFFFL
            val rawSize = bb.getInt(offset + 16).toLong() and 0xFFFFFFFFL
            sectionEntries.add(
                ZipEntrySummary(
                    path = rawName,
                    uncompressedBytes = maxOf(virtualSize, rawSize),
                    compressedBytes = rawSize,
                    isDirectory = false
                )
            )
            idx++
        }

        return InspectedPackage(
            fileName = fileName,
            format = "EXE",
            totalEntries = sectionEntries.size,
            compressedBytes = totalSize,
            uncompressedBytes = totalSize,
            hasManifest = false,
            dexCount = 0,
            nativeAbis = listOf(archLabel),
            entries = sectionEntries,
            containsZipPlayBackup = false
        )
    }

    fun createDemoGameBundleBytes(games: List<GameEntryEntity>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val manifest = JSONObject().apply {
                put("bundle", "ZipPlay_Arcade_Pack_v1")
                put("platform", "Android")
                put("gamesCount", games.size)
                put("generatedAt", System.currentTimeMillis())
            }.toString(2)
            writeZipEntry(zos, "manifest.json", manifest.toByteArray(Charsets.UTF_8))

            val readme = buildString {
                appendLine("ZipPlay Arcade - Pacote de Metadados & Jogos Mobile")
                appendLine("Modo de leitura segura: sem execução dinâmica de código.")
                appendLine()
                games.forEach { game ->
                    appendLine("- ${game.title} (${game.genre}) | Recorde: ${game.highScore} | Favorito: ${game.isFavorite}")
                }
            }
            writeZipEntry(zos, "README.txt", readme.toByteArray(Charsets.UTF_8))

            games.forEach { game ->
                val cfg = JSONObject().apply {
                    put("id", game.id)
                    put("title", game.title)
                    put("version", game.versionLabel)
                    put("favorite", game.isFavorite)
                    put("highScore", game.highScore)
                }.toString(2)
                writeZipEntry(zos, "games/${game.id}.json", cfg.toByteArray(Charsets.UTF_8))
            }
        }
        return baos.toByteArray()
    }

    suspend fun exportArcadeBackupToUri(
        context: Context,
        uri: Uri,
        games: List<GameEntryEntity>,
        archives: List<PackageArchiveEntity>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val out = context.contentResolver.openOutputStream(uri)
                ?: error("Não foi possível criar o arquivo ZIP de backup.")
            out.use { stream ->
                ZipOutputStream(stream).use { zos ->
                    val root = JSONObject()
                    root.put("app", "ZipPlay Arcade")
                    root.put("exportedAt", System.currentTimeMillis())
                    val gamesArray = JSONArray()
                    games.forEach { game ->
                        gamesArray.put(
                            JSONObject().apply {
                                put("id", game.id)
                                put("title", game.title)
                                put("isFavorite", game.isFavorite)
                                put("highScore", game.highScore)
                                put("playCount", game.playCount)
                            }
                        )
                    }
                    root.put("games", gamesArray)
                    writeZipEntry(zos, BACKUP_JSON_ENTRY, root.toString(2).toByteArray(Charsets.UTF_8))

                    val summary = buildString {
                        appendLine("ZipPlay Arcade - Backup de Jogos e Pacotes")
                        appendLine("Total de jogos: ${games.size}")
                        appendLine("Favoritos: ${games.count { it.isFavorite }}")
                        appendLine("Pacotes inspecionados: ${archives.size}")
                        appendLine()
                        games.forEach {
                            val favTag = if (it.isFavorite) "[FAVORITO]" else "[NORMAL]"
                            appendLine("$favTag ${it.title} - Recorde: ${it.highScore} (${it.playCount} partidas)")
                        }
                    }
                    writeZipEntry(zos, "resumo_arcade.txt", summary.toByteArray(Charsets.UTF_8))
                }
            }
            games.size
        }
    }

    suspend fun parseBackupFromZipUri(context: Context, uri: Uri): Result<List<BackupGameState>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: error("Não foi possível ler o arquivo ZIP.")
                var jsonPayload: String? = null
                stream.use { input ->
                    ZipInputStream(input).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            if (entry.name == BACKUP_JSON_ENTRY) {
                                jsonPayload = zis.bufferedReader(Charsets.UTF_8).readText()
                                break
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
                val raw = jsonPayload
                    ?: error("Este ZIP não contém um arquivo de backup do ZipPlay Arcade ($BACKUP_JSON_ENTRY).")
                val obj = JSONObject(raw)
                val arr = obj.getJSONArray("games")
                buildList {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        add(
                            BackupGameState(
                                id = item.getString("id"),
                                isFavorite = item.optBoolean("isFavorite", false),
                                highScore = item.optInt("highScore", 0),
                                playCount = item.optInt("playCount", 0)
                            )
                        )
                    }
                }
            }
        }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(data)
        zos.closeEntry()
    }

    private fun resolveFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "pacote_desconhecido.zip"
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
