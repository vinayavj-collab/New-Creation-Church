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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
    onFallbackClick: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val ytSettings by com.example.util.YouTubeSettingsManager.settings.collectAsState()
    var hasError by remember { mutableStateOf(false) }
    var errorCode by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(ytSettings.selectedQuality, webViewRef) {
        val qual = ytSettings.selectedQuality
        webViewRef?.evaluateJavascript("if (typeof setQuality === 'function') { setQuality('$qual'); }", null)
    }

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
                    com.example.util.SharedVideoPlayerManager.getOrCreatePlayer(ctx, videoId) { createCtx ->
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
                                fun onPlaybackError(code: Int) {
                                    post {
                                        errorCode = code
                                        hasError = true
                                    }
                                }

                                @JavascriptInterface
                                fun onPlaybackProgress(seconds: Float, duration: Float, isPlaying: Boolean) {
                                    VideoPlaybackTracker.setPosition(videoId, seconds)
                                    if (duration > 0f) {
                                        VideoPlaybackTracker.setDuration(videoId, duration)
                                    }
                                    VideoPlaybackTracker.setPlaying(videoId, isPlaying)
                                }

                                @JavascriptInterface
                                fun onSwipeDown() {
                                    post {
                                        onSwipeDown?.invoke()
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
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: ""
                                    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("intent://") || url.startsWith("vnd.youtube:")) {
                                        if (!url.contains("youtube.com/embed") && !url.contains("youtube-nocookie.com") && !url.startsWith("data:")) {
                                            return true // Block external YouTube page/app navigation
                                        }
                                    }
                                    return false
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    val targetUrl = url ?: ""
                                    if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://") || targetUrl.startsWith("intent://") || targetUrl.startsWith("vnd.youtube:")) {
                                        if (!targetUrl.contains("youtube.com/embed") && !targetUrl.contains("youtube-nocookie.com") && !targetUrl.startsWith("data:")) {
                                            return true // Block external YouTube page/app navigation
                                        }
                                    }
                                    return false
                                }

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
                            val shouldHideTitle = ytSettings.hideTitleAndShare

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
                                        .ytp-endscreen-content, .ytp-ce-element, .ytp-endscreen-paginate, .ytp-videowall-still, .ytp-show-tiles,
                                        .ytp-pause-overlay, .ytp-pause-overlay-container, .ytp-suggestion-set, .ytp-ce-covering-overlay,
                                        .ytp-ce-element-show, .ytp-ce-video, .ytp-ce-channel, .ytp-scroll-min {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            pointer-events: none !important;
                                        }

                                        .ytp-title, .ytp-title-text, .ytp-title-link, .ytp-title-channel, .ytp-chrome-top, .ytp-show-cards-title, 
                                        .ytp-share-button, .ytp-button-share, .ytp-copy-link-button, .ytp-share-panel, .ytp-share-panel-link,
                                        .ytp-fullscreen-button, .ytp-watermark, .ytp-youtube-button,
                                        .ytp-chrome-bottom, .ytp-progress-bar-container, .ytp-progress-bar,
                                        .ytp-gradient-bottom, .ytp-gradient-top, .ytp-play-button, .ytp-large-play-button,
                                        button[aria-label*="Share"], button[title*="Share"], button[aria-label*="शेयर"], button[title*="शेयर"],
                                        button[aria-label*="Full screen"], button[title*="Full screen"],
                                        button[aria-label*="Settings"], button[title*="Settings"] {
                                            display: ${if (shouldHideTitle) "none !important" else "block"} ;
                                            visibility: ${if (shouldHideTitle) "hidden !important" else "visible"} ;
                                            opacity: ${if (shouldHideTitle) "0 !important" else "1"} ;
                                            pointer-events: ${if (shouldHideTitle) "none !important" else "auto"} ;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div id="player"></div>
                                    <script>
                                        var tag = document.createElement('script');
                                        tag.src = "https://www.youtube.com/iframe_api";
                                        var firstScriptTag = document.getElementsByTagName('script')[0];
                                        firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                                        var player;
                                        var progressTimer = null;

                                        function hideTitleElements() {
                                            var endscreenSelectors = ['.ytp-endscreen-content', '.ytp-ce-element', '.ytp-endscreen-paginate', '.ytp-videowall-still', '.ytp-show-tiles', '.ytp-pause-overlay', '.ytp-pause-overlay-container', '.ytp-suggestion-set', '.ytp-ce-covering-overlay', '.ytp-ce-element-show', '.ytp-ce-video', '.ytp-ce-channel'];
                                            endscreenSelectors.forEach(function(s) {
                                                var els = document.querySelectorAll(s);
                                                for (var i = 0; i < els.length; i++) {
                                                    els[i].style.display = 'none';
                                                    els[i].style.opacity = '0';
                                                    els[i].style.pointerEvents = 'none';
                                                }
                                            });

                                            if ($shouldHideTitle) {
                                                var selectors = ['.ytp-title', '.ytp-title-text', '.ytp-title-link', '.ytp-title-channel', '.ytp-chrome-top', '.ytp-share-button', '.ytp-button-share', '.ytp-copy-link-button', '.ytp-share-panel', '.ytp-show-cards-title', '.ytp-watermark'];
                                                selectors.forEach(function(s) {
                                                    var els = document.querySelectorAll(s);
                                                    for (var i = 0; i < els.length; i++) {
                                                        els[i].style.display = 'none';
                                                        els[i].style.opacity = '0';
                                                        els[i].style.pointerEvents = 'none';
                                                    }
                                                });
                                            }
                                        }
                                        setInterval(hideTitleElements, 250);

                                        function seekTo(sec) {
                                            try {
                                                if (player && player.seekTo) {
                                                    player.seekTo(sec, true);
                                                }
                                            } catch(e){}
                                        }

                                        function setQuality(qual) {
                                            try {
                                                if (player) {
                                                    if (qual && qual !== 'auto' && qual !== 'default') {
                                                        if (player.setPlaybackQuality) player.setPlaybackQuality(qual);
                                                        if (player.setSuggestedQuality) player.setSuggestedQuality(qual);
                                                    } else {
                                                        if (player.setPlaybackQuality) player.setPlaybackQuality('default');
                                                        if (player.setSuggestedQuality) player.setSuggestedQuality('default');
                                                    }
                                                }
                                            } catch(e){}
                                        }

                                        function setupProgressTracking() {
                                            if (progressTimer) clearInterval(progressTimer);
                                            progressTimer = setInterval(function() {
                                                try {
                                                    if (player && player.getCurrentTime) {
                                                        var curr = player.getCurrentTime() || 0;
                                                        var dur = player.getDuration ? (player.getDuration() || 0) : 0;
                                                        var st = player.getPlayerState ? player.getPlayerState() : 1;
                                                        if (window.AndroidApp && window.AndroidApp.onPlaybackProgress) {
                                                            window.AndroidApp.onPlaybackProgress(curr, dur, st === 1 || st === 3);
                                                        }
                                                    }
                                                } catch(e){}
                                            }, 500);
                                        }

                                        function onYouTubeIframeAPIReady() {
                                            player = new YT.Player('player', {
                                                width: '100%',
                                                height: '100%',
                                                videoId: '$videoId',
                                                playerVars: {
                                                    'autoplay': 1,
                                                    'controls': 0,
                                                    'rel': 0,
                                                    'modestbranding': 1,
                                                    'playsinline': 1,
                                                    'fs': 0,
                                                    'disablekb': 1,
                                                    'iv_load_policy': 3,
                                                    'cc_load_policy': 0,
                                                    'showinfo': 0,
                                                    'autohide': 1,
                                                    'start': $startSecInt,
                                                    'origin': 'https://www.youtube.com'
                                                },
                                                events: {
                                                    'onReady': function(e) {
                                                        try {
                                                            if ($startSecInt > 2) {
                                                                e.target.seekTo($startSecInt, true);
                                                            }
                                                            e.target.playVideo();
                                                            setupProgressTracking();
                                                        } catch(err){}
                                                    },
                                                    'onStateChange': function(e) {
                                                        try {
                                                            if (player && player.getCurrentTime) {
                                                                var curr = player.getCurrentTime() || 0;
                                                                var dur = player.getDuration ? (player.getDuration() || 0) : 0;
                                                                var isPlaying = (e.data === 1 || e.data === 3);
                                                                if (window.AndroidApp && window.AndroidApp.onPlaybackProgress) {
                                                                    window.AndroidApp.onPlaybackProgress(curr, dur, isPlaying);
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

                                        // Swipe down gesture detection inside web view
                                        var touchStartY = 0;
                                        var touchStartX = 0;
                                        window.addEventListener('touchstart', function(e) {
                                            if (e.touches && e.touches.length === 1) {
                                                touchStartY = e.touches[0].clientY;
                                                touchStartX = e.touches[0].clientX;
                                            }
                                        }, {passive: true});

                                        window.addEventListener('touchend', function(e) {
                                            if (e.changedTouches && e.changedTouches.length === 1) {
                                                var dy = e.changedTouches[0].clientY - touchStartY;
                                                var dx = Math.abs(e.changedTouches[0].clientX - touchStartX);
                                                if (dy > 45 && dy > dx * 1.1) {
                                                    if (window.AndroidApp && window.AndroidApp.onSwipeDown) {
                                                        window.AndroidApp.onSwipeDown();
                                                    }
                                                }
                                            }
                                        }, {passive: true});
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()

                            var startTouchY = 0f
                            var startTouchX = 0f
                            setOnTouchListener { _, event ->
                                when (event.actionMasked) {
                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        startTouchY = event.rawY
                                        startTouchX = event.rawX
                                        false
                                    }
                                    android.view.MotionEvent.ACTION_UP -> {
                                        val dy = event.rawY - startTouchY
                                        val dx = kotlin.math.abs(event.rawX - startTouchX)
                                        if (dy > 60f && dy > dx * 1.1f) {
                                            onSwipeDown?.invoke()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            }

                            loadDataWithBaseURL("https://www.youtube-nocookie.com", embedHtml, "text/html", "UTF-8", "https://www.youtube-nocookie.com")
                        }
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    webView.onResume()
                    webView.resumeTimers()
                },
                onRelease = { webView ->
                    if (!com.example.util.SharedVideoPlayerManager.isCurrentVideo(videoId)) {
                        (webView.parent as? ViewGroup)?.removeView(webView)
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
                }
            )
        }

        val currentPos by VideoPlaybackTracker.currentPosition.collectAsState()
        val currentDur by VideoPlaybackTracker.currentDuration.collectAsState()
        var isSeeking by remember { mutableStateOf(false) }
        var seekProgress by remember { mutableFloatStateOf(0f) }
        val displayProgress = if (isSeeking) seekProgress else (if (currentDur > 0f) (currentPos / currentDur).coerceIn(0f, 1f) else 0f)

        if (currentDur > 0f || currentPos > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.BottomCenter)
                    .pointerInput(currentDur, webViewRef) {
                        detectTapGestures(
                            onPress = { offset ->
                                if (currentDur > 0f) {
                                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    isSeeking = true
                                    seekProgress = fraction
                                    val targetSeconds = fraction * currentDur
                                    VideoPlaybackTracker.setPosition(videoId, targetSeconds)
                                    webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($targetSeconds); }", null)
                                    tryAwaitRelease()
                                    isSeeking = false
                                }
                            }
                        )
                    }
                    .pointerInput(currentDur, webViewRef) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                if (currentDur > 0f) {
                                    isSeeking = true
                                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    seekProgress = fraction
                                }
                            },
                            onDragEnd = {
                                if (currentDur > 0f) {
                                    val targetSeconds = seekProgress * currentDur
                                    VideoPlaybackTracker.setPosition(videoId, targetSeconds)
                                    webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($targetSeconds); }", null)
                                    isSeeking = false
                                }
                            },
                            onDragCancel = {
                                isSeeking = false
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                if (currentDur > 0f) {
                                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    seekProgress = fraction
                                    val targetSeconds = fraction * currentDur
                                    VideoPlaybackTracker.setPosition(videoId, targetSeconds)
                                    webViewRef?.evaluateJavascript("if (typeof seekTo === 'function') { seekTo($targetSeconds); }", null)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                LinearProgressIndicator(
                    progress = { displayProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSeeking) 5.dp else 3.5.dp),
                    color = Color(0xFFDC2626),
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}
