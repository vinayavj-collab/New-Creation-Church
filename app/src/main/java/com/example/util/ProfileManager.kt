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
    private const val PREFS_NAME = "vinay_app_prefs"
    private const val KEY_ACTIVE_PROFILE = "active_app_profile"

    private var runtimeProfile: AppProfile = AppProfile.DEFAULT
    private val _activeProfileFlow: MutableStateFlow<AppProfile> = MutableStateFlow(AppProfile.DEFAULT)
    val activeProfileFlow: StateFlow<AppProfile> = _activeProfileFlow.asStateFlow()

    private var isLoaded = false

    private fun loadProfileIfNeeded(context: Context) {
        if (!isLoaded) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedProfileId = prefs.getString(KEY_ACTIVE_PROFILE, AppProfile.DEFAULT.id)
            val profile = AppProfile.fromId(savedProfileId)
            runtimeProfile = profile
            _activeProfileFlow.value = profile
            isLoaded = true
        }
    }

    fun isVinayProfile(): Boolean {
        return runtimeProfile == AppProfile.VINAY
    }

    fun getProfileFlow(context: Context): StateFlow<AppProfile> {
        loadProfileIfNeeded(context)
        return _activeProfileFlow.asStateFlow()
    }

    fun getActiveProfile(context: Context): AppProfile {
        loadProfileIfNeeded(context)
        return runtimeProfile
    }

    fun setActiveProfile(context: Context, profile: AppProfile) {
        runtimeProfile = profile
        _activeProfileFlow.value = profile
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_PROFILE, profile.id).apply()
        isLoaded = true
    }

    fun resetToDefault(context: Context) {
        runtimeProfile = AppProfile.DEFAULT
        _activeProfileFlow.value = AppProfile.DEFAULT
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_PROFILE, AppProfile.DEFAULT.id).apply()
        isLoaded = true
    }

    fun isPrivateProfileEnabled(): Boolean {
        val serverDbEnabled = com.example.data.repository.FirebaseDataRepository.getInstance().isPrivateProfileEnabled.value
        val remoteConfigEnabled = RemoteConfigHelper.isPrivateProfileEnabled()
        return serverDbEnabled && remoteConfigEnabled
    }

    fun verifyPasswordForPrivateProfile(input: String): Boolean {
        if (isVinayProfile()) return true
        val fbPass = com.example.data.repository.FirebaseDataRepository.getInstance().privateProfilePassword.value.trim()
        val rcPass = RemoteConfigHelper.getPrivateProfilePassword().trim()
        val expectedPassword = when {
            fbPass.isNotBlank() -> fbPass
            rcPass.isNotBlank() -> rcPass
            else -> AppProfile.PROFILE_B_PASSWORD
        }
        return input.trim() == expectedPassword
    }

    fun restartApp(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity.startActivity(intent)
        activity.finish()
    }
}
