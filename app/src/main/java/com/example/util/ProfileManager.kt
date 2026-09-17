package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.MainActivity
import com.example.data.model.AppProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProfileManager {
    // In-memory runtime active profile that clears completely when the app is closed
    private var runtimeProfile: AppProfile = AppProfile.DEFAULT
    private val _activeProfileFlow: MutableStateFlow<AppProfile> = MutableStateFlow(AppProfile.DEFAULT)

    fun getProfileFlow(context: Context): StateFlow<AppProfile> {
        return _activeProfileFlow.asStateFlow()
    }

    fun getActiveProfile(context: Context): AppProfile {
        return runtimeProfile
    }

    fun setActiveProfile(context: Context, profile: AppProfile) {
        runtimeProfile = profile
        _activeProfileFlow.value = profile
    }

    fun resetToDefault(context: Context) {
        runtimeProfile = AppProfile.DEFAULT
        _activeProfileFlow.value = AppProfile.DEFAULT
    }

    fun verifyPasswordForPrivateProfile(input: String): Boolean {
        return input.trim() == AppProfile.PROFILE_B_PASSWORD
    }

    fun restartApp(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity.startActivity(intent)
        activity.finish()
    }
}
