package com.example.util

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView

/**
 * Manages a persistent WebView player instance across full-screen VideoPlayerScreen,
 * Picture-in-Picture mode, and the YouTubeMiniPlayer overlay.
 * Reusing the same WebView prevents playback interruption, pauses, and reload delays
 * when collapsing, expanding, or minimizing videos.
 */
object SharedVideoPlayerManager {
    private const val TAG = "SharedVideoPlayerMgr"
    private var activeWebView: WebView? = null
    private var currentVideoKey: String? = null

    fun getOrCreatePlayer(
        context: Context,
        videoKey: String,
        factory: (Context) -> WebView
    ): WebView {
        if (currentVideoKey == videoKey && activeWebView != null) {
            val existing = activeWebView!!
            // Detach from previous parent ViewGroup if still attached
            (existing.parent as? ViewGroup)?.removeView(existing)
            Log.d(TAG, "Reusing existing active WebView for video: $videoKey")
            return existing
        }

        // Different video or first run -> release old player and create new
        releasePlayer()
        Log.d(TAG, "Creating new WebView for video: $videoKey")
        val newPlayer = factory(context)
        activeWebView = newPlayer
        currentVideoKey = videoKey
        return newPlayer
    }

    fun releasePlayer() {
        activeWebView?.let { wv ->
            try {
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.onPause()
                wv.webChromeClient = android.webkit.WebChromeClient()
                wv.webViewClient = android.webkit.WebViewClient()
                wv.removeJavascriptInterface("AndroidApp")
                wv.removeAllViews()
                wv.destroy()
                Log.d(TAG, "Released shared WebView")
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing WebView", e)
            }
        }
        activeWebView = null
        currentVideoKey = null
    }

    fun isCurrentVideo(videoKey: String): Boolean {
        return currentVideoKey == videoKey && activeWebView != null
    }

    fun playVideo() {
        activeWebView?.post {
            try {
                activeWebView?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            if (typeof player !== 'undefined' && player && player.playVideo) {
                                player.playVideo();
                            } else {
                                var v = document.querySelector('video');
                                if (v) v.play();
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(), null
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error playing video", e)
            }
        }
    }

    fun pauseVideo() {
        activeWebView?.post {
            try {
                activeWebView?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            if (typeof player !== 'undefined' && player && player.pauseVideo) {
                                player.pauseVideo();
                            } else {
                                var v = document.querySelector('video');
                                if (v) v.pause();
                            }
                        } catch(e) {}
                    })();
                    """.trimIndent(), null
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error pausing video", e)
            }
        }
    }
}
