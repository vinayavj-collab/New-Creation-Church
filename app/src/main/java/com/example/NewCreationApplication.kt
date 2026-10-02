package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

open class NewCreationApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase(this)
    }

    companion object {
        private const val TAG = "NewCreationApp"

        @Synchronized
        fun initFirebase(context: Context): FirebaseApp? {
            return try {
                val appContext = context.applicationContext ?: context
                val hasDefaultApp = try {
                    FirebaseApp.getInstance()
                    true
                } catch (_: Exception) {
                    false
                }

                if (hasDefaultApp) {
                    FirebaseApp.getInstance()
                } else {
                    val app = try {
                        FirebaseApp.initializeApp(appContext)
                    } catch (e: Exception) {
                        Log.w(TAG, "Standard FirebaseApp.initializeApp failed, trying fallback options: ${e.message}")
                        null
                    }
                    app ?: initFallback(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase initialization error: ${e.message}", e)
                null
            }
        }

        private fun initFallback(context: Context): FirebaseApp? {
            return try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:121740459385:android:c39415891d4e0821")
                    .setApiKey("AIzaSyDummyKeyForNewCreationChurchApp123456")
                    .setProjectId("newcreationchurch-ncck")
                    .setStorageBucket("newcreationchurch-ncck.appspot.com")
                    .setDatabaseUrl("https://newcreationchurch-ncck-default-rtdb.firebaseio.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
            } catch (e: Exception) {
                Log.e(TAG, "Fallback Firebase initialization failed: ${e.message}", e)
                null
            }
        }
    }
}

