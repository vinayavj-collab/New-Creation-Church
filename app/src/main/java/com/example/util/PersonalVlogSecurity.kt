package com.example.util

import com.example.data.repository.FirebaseDataRepository

/**
 * Manages access control and password verification for Personal Vlog (Dailymotion & Blog feeds).
 * Default Password: "9479"
 * Allows Firebase Realtime Database and Firebase Remote Config to dynamically change password
 * or override and turn ON/OFF Personal Vlog.
 */
object PersonalVlogSecurity {
    const val DEFAULT_VLOG_PASSWORD = "9479"

    /**
     * Checks if Firebase (Server) allows Personal Vlog to be displayed.
     * Firebase has master override authority over local app settings.
     * EXCEPTION: In "Vinay Kumar Avj" profile, all content is always shown
     * even if Firebase has set the switch to OFF.
     */
    fun isVlogServerAllowed(): Boolean {
        if (ProfileManager.isVinayProfile()) {
            return true
        }
        val serverDbEnabled = FirebaseDataRepository.getInstance().isPersonalVlogEnabled.value
        val remoteConfigEnabled = RemoteConfigHelper.isVlogServerEnabled()
        return serverDbEnabled && remoteConfigEnabled
    }

    /**
     * Checks if user has locally enabled/unlocked the Personal Vlog via password.
     */
    fun isPersonalVlogLocallyEnabled(): Boolean {
        if (ProfileManager.isVinayProfile()) {
            return true
        }
        val settings = com.example.data.local.PreferencesManager.getInstanceOrNull()?.settings?.value ?: return false
        return settings.showPersonalVlog || settings.personalVlogMode != com.example.data.model.PersonalVlogMode.HIDDEN
    }

    /**
     * Complete dual-layer check: True ONLY IF user entered password and turned ON Personal Vlog
     * AND server allows it (or if in Vinay profile).
     */
    fun isPersonalVlogAllowed(): Boolean {
        if (ProfileManager.isVinayProfile()) {
            return true
        }
        return isPersonalVlogLocallyEnabled() && isVlogServerAllowed()
    }

    /**
     * Verifies the entered password against Firebase Realtime DB, Remote Config, or default 9479.
     */
    fun verifyPassword(input: String): Boolean {
        if (ProfileManager.isVinayProfile()) {
            return true
        }
        val fbPass = FirebaseDataRepository.getInstance().personalVlogPassword.value.trim()
        val rcPass = RemoteConfigHelper.getPersonalVlogPassword().trim()
        val expectedPassword = when {
            fbPass.isNotBlank() -> fbPass
            rcPass.isNotBlank() -> rcPass
            else -> DEFAULT_VLOG_PASSWORD
        }
        return input.trim() == expectedPassword
    }
}
