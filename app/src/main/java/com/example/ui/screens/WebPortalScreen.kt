package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.EmeraldPulse
import java.lang.ref.WeakReference

private class ZipPlayJsBridge(
    private val onScoreReported: (String, Int) -> Unit
) {
    @JavascriptInterface
    fun reportScore(gameTitle: String, score: Int) {
        onScoreReported(gameTitle, score)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPortalScreen(
    initialTitle: String,
    externalUrl: String?,
    bundledAssetFile: String?,
    onWebGameScore: (String, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isLocalHtml5Game = !bundledAssetFile.isNullOrBlank()
    val safeTargetUrl = remember(externalUrl, bundledAssetFile) {
        if (isLocalHtml5Game) {
            val cleanAsset = bundledAssetFile!!.substringAfterLast('/')
            "file:///android_asset/webgames/$cleanAsset"
        } else {
            externalUrl ?: "https://poki.com"
        }
    }

    var pageTitle by remember { mutableStateOf(initialTitle) }
    var currentUrl by remember { mutableStateOf(safeTargetUrl) }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var rendererErrorMessage by remember { mutableStateOf<String?>(null) }
    var useSoftwareLayer by remember { mutableStateOf(false) }
    var webViewInstanceKey by remember { mutableIntStateOf(0) }

    val webViewRef = remember { mutableStateOf<WeakReference<WebView>?>(null) }

    fun destroyCurrentWebView() {
        val wv = webViewRef.value?.get()
        webViewRef.value = null
        if (wv != null) {
            wv.stopLoading()
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
    }

    BackHandler {
        val wv = webViewRef.value?.get()
        if (rendererErrorMessage == null && wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val wv = webViewRef.value?.get() ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wv.onPause()
                    wv.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    wv.onResume()
                    wv.resumeTimers()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            destroyCurrentWebView()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(visible = !isFullscreen) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("web_portal_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar ao portal de sites"
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = pageTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = EmeraldPulse,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isLocalHtml5Game) {
                                        "arcade://web.zipplay/$bundledAssetFile"
                                    } else {
                                        currentUrl
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!isLocalHtml5Game) {
                            IconButton(
                                onClick = { webViewRef.value?.get()?.goBack() },
                                enabled = canGoBack && rendererErrorMessage == null,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Página anterior"
                                )
                            }
                            IconButton(
                                onClick = { webViewRef.value?.get()?.goForward() },
                                enabled = canGoForward && rendererErrorMessage == null,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Avançar página"
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (rendererErrorMessage != null) {
                                    rendererErrorMessage = null
                                    webViewInstanceKey++
                                } else {
                                    webViewRef.value?.get()?.reload()
                                }
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Recarregar"
                            )
                        }

                        if (!isLocalHtml5Game) {
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                        )
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInBrowser,
                                    contentDescription = "Abrir no navegador externo"
                                )
                            }
                        }

                        IconButton(
                            onClick = { isFullscreen = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Tela cheia"
                            )
                        }
                    }

                    if (isLoading && progress < 100 && rendererErrorMessage == null) {
                        LinearProgressIndicator(
                            progress = { (progress / 100f).coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (rendererErrorMessage == null) {
                key(webViewInstanceKey, useSoftwareLayer) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.parseColor("#070B16"))
                                setLayerType(
                                    if (useSoftwareLayer) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_HARDWARE,
                                    null
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    setRendererPriorityPolicy(
                                        WebView.RENDERER_PRIORITY_IMPORTANT,
                                        false
                                    )
                                }
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = false
                                    displayZoomControls = false
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = false
                                    mediaPlaybackRequiresUserGesture = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    allowFileAccess = isLocalHtml5Game
                                    allowContentAccess = false
                                }

                                if (isLocalHtml5Game) {
                                    addJavascriptInterface(
                                        ZipPlayJsBridge { title, score ->
                                            post { onWebGameScore(title, score) }
                                        },
                                        "ZipPlayBridge"
                                    )
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank() && !title.startsWith("file://")) {
                                            pageTitle = title
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        isLoading = true
                                        if (!url.isNullOrBlank()) currentUrl = url
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        if (!url.isNullOrBlank()) currentUrl = url
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            rendererErrorMessage =
                                                "Não foi possível carregar este site agora (${error?.description ?: "erro de conexão"})."
                                        }
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        isLoading = false
                                        webViewRef.value = null
                                        if (view != null) {
                                            (view.parent as? ViewGroup)?.removeView(view)
                                            view.destroy()
                                        }
                                        rendererErrorMessage =
                                            "O motor web foi reiniciado por limite de memória ou aceleração gráfica."
                                        return true
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val target = request?.url ?: return false
                                        val scheme = target.scheme?.lowercase() ?: ""
                                        if (isLocalHtml5Game) {
                                            return !target.toString()
                                                .startsWith("file:///android_asset/webgames/")
                                        }
                                        return when (scheme) {
                                            "https" -> false
                                            "http" -> {
                                                val upgraded = target.buildUpon()
                                                    .scheme("https")
                                                    .build()
                                                    .toString()
                                                view?.loadUrl(upgraded)
                                                true
                                            }
                                            else -> true
                                        }
                                    }
                                }

                                loadUrl(safeTargetUrl)
                                webViewRef.value = WeakReference(this)
                            }
                        },
                        onRelease = { view ->
                            if (webViewRef.value?.get() === view) {
                                webViewRef.value = null
                            }
                            view.stopLoading()
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.removeAllViews()
                            view.destroy()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("web_portal_webview")
                    )
                }
            } else {
                WebRecoveryPanel(
                    message = rendererErrorMessage.orEmpty(),
                    showExternalBrowser = !isLocalHtml5Game,
                    useSoftwareLayer = useSoftwareLayer,
                    onReload = {
                        rendererErrorMessage = null
                        webViewInstanceKey++
                    },
                    onToggleCompatibilityAndReload = {
                        useSoftwareLayer = !useSoftwareLayer
                        rendererErrorMessage = null
                        webViewInstanceKey++
                    },
                    onOpenExternalBrowser = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                            )
                        }
                    }
                )
            }

            if (isFullscreen) {
                FloatingActionButton(
                    onClick = { isFullscreen = false },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.FullscreenExit,
                        contentDescription = "Mostrar barra de navegação"
                    )
                }
            }
        }
    }
}

@Composable
private fun WebRecoveryPanel(
    message: String,
    showExternalBrowser: Boolean,
    useSoftwareLayer: Boolean,
    onReload: () -> Unit,
    onToggleCompatibilityAndReload: () -> Unit,
    onOpenExternalBrowser: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = ElectricAmber,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Sessão Web Interrompida",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onReload,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recarregar Página")
            }
            OutlinedButton(
                onClick = onToggleCompatibilityAndReload,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
            ) {
                Text(
                    if (useSoftwareLayer) "Tentar Modo Hardware" else "Recarregar em Modo Compatibilidade"
                )
            }
            if (showExternalBrowser) {
                OutlinedButton(
                    onClick = onOpenExternalBrowser,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir no Navegador")
                }
            }
        }
    }
}
