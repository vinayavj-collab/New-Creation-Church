package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Dedicated Dailymotion Player View configured with WebChromeClient,
 * hardware acceleration, full-screen custom view callback handling, and responsive embed HTML.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DailymotionPlayerView(
    videoId: String,
    modifier: Modifier = Modifier,
    autoplay: Boolean = true
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val activity = context as? ComponentActivity

    val toggleOrientation: () -> Unit = {
        val act = context as? ComponentActivity
        if (act != null) {
            act.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Unable to load Dailymotion video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        hasError = false
                        isLoading = true
                        webViewRef?.reload()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Retry")
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        setBackgroundColor(android.graphics.Color.BLACK)

                        // 2. Comprehensive WebSettings configuration
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadsImagesAutomatically = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowFileAccess = true
                            allowContentAccess = true
                            databaseEnabled = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                            val defaultUA = userAgentString
                            userAgentString = "$defaultUA (Android Mobile; DailymotionPlayer) Chrome/122.0.0.0"
                        }

                        // 3. WebChromeClient with full-screen custom view support
                        webChromeClient = object : WebChromeClient() {
                            private var customView: View? = null
                            private var customViewCallback: CustomViewCallback? = null

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (customView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                val decorView = activity?.window?.decorView as? ViewGroup
                                if (decorView != null && view != null) {
                                    customView = view
                                    customViewCallback = callback
                                    decorView.addView(
                                        view,
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }

                            override fun onHideCustomView() {
                                val decorView = activity?.window?.decorView as? ViewGroup
                                if (customView != null) {
                                    decorView?.removeView(customView)
                                    customView = null
                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return true
                            }
                        }

                        // 4. WebViewClient handling
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    errorMessage = error?.description?.toString().orEmpty()
                                }
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                view?.let {
                                    try {
                                        val parent = it.parent as? ViewGroup
                                        parent?.removeView(it)
                                        it.destroy()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                hasError = true
                                errorMessage = "Video renderer process encountered an error"
                                return true
                            }
                        }

                        val autoplayVal = if (autoplay) "1" else "0"
                        // Responsive HTML wrapper embedding Dailymotion's official player iframe
                        val embedHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    * { margin: 0; padding: 0; box-sizing: border-box; }
                                    html, body {
                                        width: 100%;
                                        height: 100%;
                                        background-color: #000000;
                                        overflow: hidden;
                                        display: flex;
                                        align-items: center;
                                        justify-content: center;
                                    }
                                    .video-container {
                                        position: relative;
                                        width: 100%;
                                        height: 100%;
                                    }
                                    iframe {
                                        position: absolute;
                                        top: 0;
                                        left: 0;
                                        width: 100%;
                                        height: 100%;
                                        border: 0;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="video-container">
                                    <iframe
                                        src="https://www.dailymotion.com/embed/video/$videoId?autoplay=$autoplayVal&mute=0&queue-enable=0&sharing-enable=1&ui-start-screen-info=0"
                                        allow="autoplay; fullscreen; picture-in-picture"
                                        allowfullscreen>
                                    </iframe>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()

                        loadDataWithBaseURL("https://www.dailymotion.com", embedHtml, "text/html", "UTF-8", "https://www.dailymotion.com")
                    }
                },
                update = {
                    // Update if needed
                },
                onRelease = { webView ->
                    try {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.onPause()
                        webView.webChromeClient = WebChromeClient()
                        webView.webViewClient = WebViewClient()
                        webView.removeAllViews()
                        webView.destroy()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
