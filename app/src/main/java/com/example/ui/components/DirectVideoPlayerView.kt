package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Dedicated Direct Stream & Firebase Storage Video Player View.
 * Hardware-accelerated HTML5 Video Player supporting MP4, HLS, WebM, and Firebase Video URLs.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DirectVideoPlayerView(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoplay: Boolean = true
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

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
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "वीडियो लोड करने में त्रुटि (Unable to play video)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        hasError = false
                        webViewRef?.reload()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("पुनः प्रयास करें (Retry)")
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    com.example.util.SharedVideoPlayerManager.getOrCreatePlayer(ctx, videoUrl) { createCtx ->
                        WebView(createCtx).apply {
                            webViewRef = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            isNestedScrollingEnabled = false
                            overScrollMode = View.OVER_SCROLL_NEVER

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                allowFileAccess = true
                                allowContentAccess = true
                            }

                            webChromeClient = object : WebChromeClient() {}

                            webViewClient = object : WebViewClient() {
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                    }
                                }
                            }

                            val htmlContent = """
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
                                        video {
                                            width: 100%;
                                            height: 100%;
                                            object-fit: contain;
                                            background-color: #000000;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <video 
                                        src="$videoUrl" 
                                        controls 
                                        playsinline 
                                        ${if (autoplay) "autoplay" else ""} 
                                        controlsList="nodownload" 
                                        preload="auto">
                                        Your browser does not support the video tag.
                                    </video>
                                </body>
                                </html>
                            """.trimIndent()

                            loadDataWithBaseURL("https://example.com", htmlContent, "text/html", "UTF-8", null)
                        }
                    }
                },
                update = { webView ->
                    webView.onResume()
                    webView.resumeTimers()
                },
                onRelease = { webView ->
                    if (!com.example.util.SharedVideoPlayerManager.isCurrentVideo(videoUrl)) {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        try {
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.onPause()
                            webView.removeAllViews()
                            webView.destroy()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }
    }
}
