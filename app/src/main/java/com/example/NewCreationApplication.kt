package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.util.FirestoreTtlCleanupManager
import com.example.util.NotificationChannelHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NewCreationApplication : Application(), ImageLoaderFactory {

    private var customImageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        com.example.util.ProfileManager.getActiveProfile(this)
        NotificationChannelHelper.createAllNotificationChannels(this)
        FirestoreTtlCleanupManager.schedulePeriodicTtlCleanup(this)
        FirestoreTtlCleanupManager.performImmediateCleanup(this)
    }

    override fun newImageLoader(): ImageLoader {
        return customImageLoader ?: synchronized(this) {
            customImageLoader ?: buildOptimizedImageLoader().also { customImageLoader = it }
        }
    }

    private fun buildOptimizedImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Max 25% of available app memory for thumbnails
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("youtube_image_cache"))
                    .maxSizeBytes(120L * 1024 * 1024) // 120MB disk cache for offline & fast rendering
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // Favor local disk cache for video thumbnails
            .allowHardware(true) // Fast hardware bitmap decoding
            .crossfade(150)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            customImageLoader?.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        customImageLoader?.memoryCache?.clear()
    }
}
