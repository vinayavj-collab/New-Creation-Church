package com.example.service

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class DailyWallpaperService(private val context: Context) {

    companion object {
        private const val TAG = "DailyWallpaperService"
    }

    suspend fun applyWallpaperFromUrl(imageUrl: String, targetScreen: String = "both"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.doInput = true
                connection.connect()
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                val wallpaperManager = WallpaperManager.getInstance(context)
                
                when (targetScreen.lowercase()) {
                    "home" -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    }
                    "lock" -> {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                    else -> {
                        // "both" or default
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    }
                }
                Log.i(TAG, "Successfully applied daily AI wallpaper to target: $targetScreen")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply daily AI wallpaper: ${e.message}", e)
                false
            }
        }
    }
}
