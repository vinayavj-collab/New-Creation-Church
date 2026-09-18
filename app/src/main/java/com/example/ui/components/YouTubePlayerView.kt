package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
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
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.VideoPlaybackTracker

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerView(
    videoId: String,
    modifier: Modifier = Modifier,
    onFallbackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var errorCode by remember { mutableStateOf(0) }

    val openInYouTube = {
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (errorCode == 150 || errorCode == 152 || errorCode == 101) {
                        "⚠️ यह वीडियो YouTube ऐप में सीधे चलने के लिए अधिकृत है (Error 152)"
                    } else {
                        "यह वीडियो सीधे एम्बेड नहीं हो सका"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = openInYouTube,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626) // YouTube Red
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "YouTube ऐप में देखें (Watch on YouTube)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
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
                            offscreenPreRaster = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onPlaybackError(code: Int) {
                                post {
                                    errorCode = code
                                    hasError = true
                                }
                            }

                            @JavascriptInterface
                            fun onPlaybackProgress(seconds: Float, isPlaying: Boolean) {
                                if (seconds > 0f) {
                                    VideoPlaybackTracker.setPosition(videoId, seconds)
                                    VideoPlaybackTracker.setPlaying(videoId, isPlaying)
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
                                return true
                            }
                        }

                        val savedTime = VideoPlaybackTracker.getPosition(videoId)
                        val startSecInt = savedTime.toInt()
                        val startParam = if (startSecInt > 2) "&start=$startSecInt" else ""

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
                                    iframe, #player {
                                        width: 100%;
                                        height: 100%;
                                        border: 0;
                                        position: absolute;
                                        top: 0;
                                        left: 0;
                                    }
                                </style>
                            </head>
                            <body>
                                <iframe 
                                    id="player"
                                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&playsinline=1&enablejsapi=1&rel=0&modestbranding=1&fs=1&iv_load_policy=3$startParam"
                                    frameborder="0"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
                                    allowfullscreen="true"
                                    webkitallowfullscreen="true"
                                    mozallowfullscreen="true"
                                    referrerpolicy="strict-origin-when-cross-origin">
                                </iframe>
                                <script>
                                    var tag = document.createElement('script');
                                    tag.src = "https://www.youtube.com/iframe_api";
                                    var firstScriptTag = document.getElementsByTagName('script')[0];
                                    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                                    var player;
                                    var progressTimer = null;

                                    function setupProgressTracking() {
                                        if (progressTimer) clearInterval(progressTimer);
                                        progressTimer = setInterval(function() {
                                            try {
                                                if (player && player.getCurrentTime) {
                                                    var curr = player.getCurrentTime();
                                                    var st = player.getPlayerState ? player.getPlayerState() : 1;
                                                    if (window.AndroidApp && window.AndroidApp.onPlaybackProgress) {
                                                        window.AndroidApp.onPlaybackProgress(curr, st === 1 || st === 3);
                                                    }
                                                }
                                            } catch(e){}
                                        }, 500);
                                    }

                                    function onYouTubeIframeAPIReady() {
                                        player = new YT.Player('player', {
                                            events: {
                                                'onReady': function(e) {
                                                    try {
                                                        var startSec = $savedTime;
                                                        if (startSec > 2) {
                                                            e.target.seekTo(startSec, true);
                                                        }
                                                        e.target.playVideo();
                                                        setupProgressTracking();
                                                    } catch(err){}
                                                },
                                                'onStateChange': function(e) {
                                                    try {
                                                        if (player && player.getCurrentTime) {
                                                            var curr = player.getCurrentTime();
                                                            var isPlaying = (e.data === 1 || e.data === 3);
                                                            if (window.AndroidApp && window.AndroidApp.onPlaybackProgress) {
                                                                window.AndroidApp.onPlaybackProgress(curr, isPlaying);
                                                            }
                                                        }
                                                    } catch(err){}
                                                },
                                                'onError': function(e) {
                                                    if (window.AndroidApp && window.AndroidApp.onPlaybackError) {
                                                        if (e.data === 150 || e.data === 152 || e.data === 101) {
                                                            // IFrame stream handles it
                                                        } else {
                                                            window.AndroidApp.onPlaybackError(e.data);
                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    }

                                    // Listen to Fullscreen changes
                                    document.addEventListener("fullscreenchange", function() {
                                        var isFull = !!(document.fullscreenElement);
                                        if (window.AndroidApp && window.AndroidApp.onFullScreenChanged) {
                                            window.AndroidApp.onFullScreenChanged(isFull);
                                        }
                                    });
                                    document.addEventListener("webkitfullscreenchange", function() {
                                        var isFull = !!(document.webkitFullscreenElement);
                                        if (window.AndroidApp && window.AndroidApp.onFullScreenChanged) {
                                            window.AndroidApp.onFullScreenChanged(isFull);
                                        }
                                    });
                                </script>
                            </body>
                            </html>
                        """.trimIndent()

                        loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", "https://www.youtube-nocookie.com")
                    }
                },
                update = { webView ->
                    // Keep instance active
                },
                onRelease = { webView ->
                    try {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.onPause()
                        webView.webChromeClient = android.webkit.WebChromeClient()
                        webView.webViewClient = android.webkit.WebViewClient()
                        webView.removeJavascriptInterface("AndroidApp")
                        webView.removeAllViews()
                        webView.destroy()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }
    }
}
