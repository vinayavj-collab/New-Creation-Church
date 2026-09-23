package com.example.ui.components

import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.SharedVideoPlayerManager
import com.example.util.VideoPlaybackTracker

/**
 * Dedicated Dailymotion WebView Player component.
 * Integrates with SharedVideoPlayerManager for persistent playback across
 * full screen, mini player, and picture-in-picture modes.
 */
@Composable
fun DailymotionPlayerView(
    videoId: String,
    modifier: Modifier = Modifier,
    autoplay: Boolean = true
) {
    // Clean ID if prefixed with dm_
    val cleanId = when {
        videoId.startsWith("dm_") -> videoId.substringAfterLast("_")
        else -> videoId
    }

    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "वीडियो लोड करने में समस्या आई",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        hasError = false
                        reloadTrigger++
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("पुनः प्रयास करें (Retry)")
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SharedVideoPlayerManager.getOrCreatePlayer(ctx, cleanId) { createCtx ->
                        WebView(createCtx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.BLACK)
                            isNestedScrollingEnabled = false
                            overScrollMode = View.OVER_SCROLL_NEVER
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                allowFileAccess = true
                                allowContentAccess = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }

                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onPlaybackProgress(seconds: Float, isPlaying: Boolean) {
                                    if (seconds >= 0f) {
                                        VideoPlaybackTracker.setPosition(cleanId, seconds)
                                        VideoPlaybackTracker.setPlaying(cleanId, isPlaying)
                                    }
                                }

                                @JavascriptInterface
                                fun onPlaybackProgress(seconds: Float, duration: Float, isPlaying: Boolean) {
                                    if (seconds >= 0f) {
                                        VideoPlaybackTracker.setPosition(cleanId, seconds)
                                        if (duration > 0f) {
                                            VideoPlaybackTracker.setDuration(cleanId, duration)
                                        }
                                        VideoPlaybackTracker.setPlaying(cleanId, isPlaying)
                                    }
                                }

                                @JavascriptInterface
                                fun onPlaybackError(msg: String) {
                                    post {
                                        hasError = true
                                    }
                                }

                                @JavascriptInterface
                                fun onFullScreenChanged(isFull: Boolean) {
                                    post {
                                        val activity = ctx as? ComponentActivity
                                        if (activity != null) {
                                            activity.requestedOrientation = if (isFull) {
                                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            }
                                        }
                                    }
                                }
                            }, "AndroidApp")

                            webChromeClient = object : WebChromeClient() {
                                private var customView: View? = null
                                private var customViewCallback: CustomViewCallback? = null

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    if (customView != null) {
                                        callback?.onCustomViewHidden()
                                        return
                                    }
                                    val activity = ctx as? ComponentActivity
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
                                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                }

                                override fun onHideCustomView() {
                                    val activity = ctx as? ComponentActivity
                                    val decorView = activity?.window?.decorView as? ViewGroup
                                    if (customView != null) {
                                        decorView?.removeView(customView)
                                        customView = null
                                        customViewCallback?.onCustomViewHidden()
                                        customViewCallback = null
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            }

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

                            val savedTime = VideoPlaybackTracker.getPosition(cleanId).toInt()
                            val startParam = if (savedTime > 2) "&start=$savedTime" else ""

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
                                        iframe {
                                            width: 100%;
                                            height: 100%;
                                            border: 0;
                                            position: absolute;
                                            top: 0;
                                            left: 0;
                                        }
                                        .dmp_queue, .dmp_QueueToggle, [class*="queue"], [class*="Queue"], 
                                        [aria-label*="Queue"], [aria-label*="Playlist"], [title*="Queue"], [title*="Playlist"],
                                        .dmp_ControlBar-queueButton {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            pointer-events: none !important;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <iframe 
                                        id="dm_player"
                                        src="https://www.dailymotion.com/embed/video/$cleanId?autoplay=${if (autoplay) "1" else "0"}&mute=0&queue-enable=0&queue-autoplay-next=0&ui-show-queue=0&ui-logo=0&ui-start-screen-info=0&sharing-enable=0&api=postMessage$startParam"
                                        frameborder="0"
                                        allow="autoplay; fullscreen; picture-in-picture"
                                        allowfullscreen>
                                    </iframe>
                                    <script>
                                        // Listen for postMessage from Dailymotion Player API
                                        window.addEventListener('message', function(e) {
                                            try {
                                                if (typeof e.data === 'string') {
                                                    var data = e.data;
                                                    if (data.indexOf('timeupdate') !== -1 && window.AndroidApp) {
                                                        var match = data.match(/time=([0-9.]+)/);
                                                        if (match && match[1]) {
                                                            window.AndroidApp.onPlaybackProgress(parseFloat(match[1]), true);
                                                        }
                                                    }
                                                }
                                            } catch(err){}
                                        });

                                        document.addEventListener("fullscreenchange", function() {
                                            var isFull = !!(document.fullscreenElement);
                                            if (window.AndroidApp && window.AndroidApp.onFullScreenChanged) {
                                                window.AndroidApp.onFullScreenChanged(isFull);
                                            }
                                        });
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()

                            loadDataWithBaseURL("https://www.dailymotion.com", embedHtml, "text/html", "UTF-8", "https://www.dailymotion.com")
                        }
                    }
                },
                update = { webView ->
                    webView.onResume()
                    webView.resumeTimers()
                },
                onRelease = { webView ->
                    if (!SharedVideoPlayerManager.isCurrentVideo(cleanId)) {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        try {
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.onPause()
                            webView.webChromeClient = WebChromeClient()
                            webView.webViewClient = WebViewClient()
                            webView.removeJavascriptInterface("AndroidApp")
                            webView.removeAllViews()
                            webView.destroy()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        }

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
