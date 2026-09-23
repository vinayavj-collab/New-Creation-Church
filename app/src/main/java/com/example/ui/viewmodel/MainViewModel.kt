package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.bible.local.BibleDatabase
import com.example.data.bible.model.BibleBook
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.local.RecentlyViewedEntity
import com.example.data.local.SavedItemEntity
import com.example.data.model.*
import com.example.data.repository.BloggerRepository
import com.example.data.repository.RecentlyViewedRepository
import com.example.data.repository.SavedItemRepository
import com.example.data.repository.YouTubeRepository
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.bible.model.ActiveReadingPlanItem
import com.example.data.bible.model.VerseOfTheDay
import com.example.data.bible.model.parsePlanColor
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    val bloggerRepository: BloggerRepository,
    val youtubeRepository: YouTubeRepository,
    val savedItemRepository: SavedItemRepository,
    val recentlyViewedRepository: RecentlyViewedRepository,
    val preferencesManager: PreferencesManager,
    val bibleDatabase: BibleDatabase,
    val readingPlanRepository: com.example.data.bible.repository.ReadingPlanRepository,
    val studyNotesRepository: com.example.data.bible.repository.StudyNotesRepository,
    val lyricsRepository: com.example.data.bible.repository.LyricsRepository,
    val backupRepository: com.example.data.bible.repository.BackupRepository,
    val syncCenterRepository: com.example.data.repository.SyncCenterRepository,
    val bibleRepository: com.example.data.bible.repository.BibleRepository
) : AndroidViewModel(application) {

    val settings: StateFlow<UserSettings> = preferencesManager.settings

    val appUpdateManager = com.example.util.AppUpdateManager.getInstance(application)
    val updateState = appUpdateManager.updateState

    val welcomeSpeechManager = com.example.util.WelcomeSpeechManager.getInstance(application)

    val notificationRepository = com.example.data.repository.NotificationRepository(application)
    val adminNoticeRepository = com.example.data.repository.AdminNoticeRepository(application)
    val adminRepository = com.example.data.repository.AdminRepository(application)

    val currentAdmin: StateFlow<AdminUser?> = adminRepository.currentAdmin
    val allAdmins: StateFlow<List<AdminUser>> = adminRepository.allAdmins
    val allDesignations: StateFlow<List<DesignationAuthority>> = adminRepository.allDesignations
    val adminSpecialAnnouncements: StateFlow<List<SpecialAnnouncement>> = adminRepository.specialAnnouncements
    val adminTodayScripture: StateFlow<AdminTodayScripture> = adminRepository.todayScripture
    val adminLiveStreamConfig: StateFlow<AdminLiveStreamConfig> = adminRepository.liveStreamConfig
    val adminNavigationConfig: StateFlow<List<NavigationTabConfig>> = adminRepository.navigationConfig
    val adminAuditLogs: StateFlow<List<AdminAuditLog>> = adminRepository.auditLogs
    val churchMembers: StateFlow<List<ChurchMember>> = adminRepository.churchMembers
    val appUserProfiles: StateFlow<List<com.example.data.model.UserProfileData>> = adminRepository.appUserProfiles
    val attendanceRecords: StateFlow<List<ChurchAttendanceRecord>> = adminRepository.attendanceRecords
    val accountTransactions: StateFlow<List<ChurchAccountTransaction>> = adminRepository.accountTransactions
    val adminPushNotifications: StateFlow<List<AdminPushNotification>> = adminRepository.pushNotifications
    val adminPolls: StateFlow<List<AdminPollItem>> = adminRepository.polls
    val adminReminderScheduleConfig: StateFlow<AdminReminderScheduleConfig> = adminRepository.reminderScheduleConfig
    val churchPrefixes: StateFlow<List<ChurchPrefixRecord>> = adminRepository.churchPrefixes
    val activeP2Sessions: StateFlow<List<ActiveP2Session>> = adminRepository.activeP2Sessions
    val prefixTransferLogs: StateFlow<List<PrefixTransferAuditLog>> = adminRepository.prefixTransferLogs
    val qrAuditLogs: StateFlow<List<com.example.data.model.QrAuditLogEntry>> = adminRepository.qrAuditLogs
    val sessionExpiredEvent: StateFlow<String?> = adminRepository.sessionExpiredEvent
    val isAdminAuthRequired: StateFlow<Boolean> = adminRepository.isAdminAuthRequired
    val isAdminRefreshing: StateFlow<Boolean> = adminRepository.isRefreshingData
    val adminDataFetchError: StateFlow<String?> = adminRepository.dataFetchError

    fun registerNewChurchPrefix(prefix: ChurchPrefixRecord, onResult: (Boolean, String?) -> Unit) {
        adminRepository.registerNewPrefix(prefix, onResult)
    }

    fun generateNextSerialForPrefix(prefixId: String): String {
        return adminRepository.generateNextSerial(prefixId)
    }

    fun generateLiveP2Otp(
        serialNumber: String,
        targetUserId: String,
        targetUserName: String,
        authorityId: String,
        authorityName: String
    ): ActiveP2Session {
        return adminRepository.generateLiveP2Otp(serialNumber, targetUserId, targetUserName, authorityId, authorityName)
    }

    fun verifyAndConsumeP2Otp(
        serialNumber: String,
        targetUserId: String,
        otpInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        adminRepository.verifyAndConsumeP2Otp(serialNumber, targetUserId, otpInput, onResult)
    }

    fun requestBishopTransferAuthorizationOtp(prefixId: String, bishopId: String): String {
        return adminRepository.requestBishopTransferAuthorizationOtp(prefixId, bishopId)
    }

    fun executePastoralTransferWithBishopOtp(
        prefixId: String,
        newPastorId: String,
        newPastorName: String,
        bishopId: String,
        bishopName: String,
        otpInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        adminRepository.executePastoralTransferWithBishopOtp(
            prefixId, newPastorId, newPastorName, bishopId, bishopName, otpInput, onResult
        )
    }

    fun getDemographicAnalyticsSummary(): DemographicAnalyticsSummary {
        return adminRepository.getDemographicAnalyticsSummary()
    }

    fun mintAuthoritySerial(
        targetRoleTier: String,
        selectedPrefix: String,
        creatorAdmin: AdminUser,
        onComplete: (Boolean, String?, String?) -> Unit
    ) {
        adminRepository.mintAuthoritySerial(targetRoleTier, selectedPrefix, creatorAdmin, onComplete)
    }

    fun requestLiveP2ForSerial(
        serialNumber: String,
        onComplete: (Boolean, String?, String?) -> Unit
    ) {
        adminRepository.requestLiveP2ForSerial(serialNumber, onComplete)
    }

    fun activateUserSelfService(
        serialNumber: String,
        p1PasswordInput: String,
        p2OtpInput: String,
        onComplete: (Boolean, UserProfileData?, String?) -> Unit
    ) {
        adminRepository.activateUserSelfService(serialNumber, p1PasswordInput, p2OtpInput, onComplete)
    }

    fun submitSelfServiceProfile(
        updatedProfile: UserProfileData,
        onComplete: (Boolean, String?) -> Unit
    ) {
        adminRepository.submitSelfServiceProfile(updatedProfile, onComplete)
    }

    fun requestPasswordResetP2(
        serialNumber: String,
        onComplete: (Boolean, String?, String?) -> Unit
    ) {
        adminRepository.requestPasswordResetP2(serialNumber, onComplete)
    }

    fun confirmPasswordResetWithP2(
        serialNumber: String,
        newP1Password: String,
        p2OtpInput: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        adminRepository.confirmPasswordResetWithP2(serialNumber, newP1Password, p2OtpInput, onComplete)
    }

    fun mergePrefixes(
        sourcePrefix: String,
        targetPrefix: String,
        masterP1Pin: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        adminRepository.mergePrefixes(sourcePrefix, targetPrefix, masterP1Pin, onComplete)
    }

    fun transferMember(
        targetUserIdOrSerial: String,
        toPrefix: String,
        adminP1Pin: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        adminRepository.transferMember(targetUserIdOrSerial, toPrefix, adminP1Pin, onComplete)
    }


    fun clearSessionExpiredEvent() {
        adminRepository.clearSessionExpiredEvent()
    }

    fun refreshAdminData(onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val result = adminRepository.refreshAllData()
            result.onSuccess {
                onResult?.invoke(true, null)
            }.onFailure { err ->
                onResult?.invoke(false, err.localizedMessage)
            }
        }
    }

    fun clearAdminDataError() {
        adminRepository.clearDataFetchError()
    }

    fun setAdminAuthRequired(required: Boolean, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = adminRepository.setAdminAuthRequired(required)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun directLoginAsAdmin(admin: AdminUser, onResult: (Boolean, AdminUser?, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.directLoginAsAdmin(admin)
            result.onSuccess { user -> onResult(true, user, null) }
                .onFailure { err -> onResult(false, null, err.localizedMessage) }
        }
    }

    fun loginAdminWithPin(pin: String, secondaryPin: String = "", profileName: String = "", onResult: (Boolean, AdminUser?, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.loginWithPin(pin, secondaryPin, profileName)
            result.onSuccess { admin ->
                val currentProfile = userProfile.value
                val newDisplayName = if (admin.name.isNotBlank()) admin.name else currentProfile.displayName
                userProfileRepository.updateProfile(
                    currentProfile.copy(
                        role = admin.designation,
                        displayName = if (currentProfile.displayName.isBlank() || currentProfile.displayName == "अतिथि विश्वासी" || currentProfile.displayName == "विश्वासी") admin.name.ifBlank { admin.designation } else currentProfile.displayName
                    )
                )
                onResult(true, admin, null)
            }.onFailure { err ->
                onResult(false, null, err.localizedMessage)
            }
        }
    }

    fun renameAdmin(adminId: String, newName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.renameAdmin(adminId, newName)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun toggleBlockDevice(adminId: String, isBlocked: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.toggleBlockDevice(adminId, isBlocked)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun applyBulkPermissionsToCategory(
        categoryRank: Int,
        categoryName: String,
        assignedFunctions: List<String>,
        onResult: (Boolean, Int, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.applyBulkPermissionsToCategory(categoryRank, categoryName, assignedFunctions)
            result.onSuccess { count -> onResult(true, count, null) }
                .onFailure { err -> onResult(false, 0, err.localizedMessage) }
        }
    }

    fun updateProfileSwitchPassword(newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            firebaseDataRepository.updatePrivateProfilePassword(newPassword, onComplete)
        }
    }

    fun regenerateSecondaryPin(adminId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.regenerateSecondaryPin(adminId)
            result.onSuccess { newOtp ->
                onResult(true, newOtp)
            }.onFailure { err ->
                onResult(false, err.localizedMessage)
            }
        }
    }

    fun logoutAdmin() {
        adminRepository.clearSession()
    }

    fun createNewAdmin(
        designation: String,
        name: String,
        pin: String,
        isAutoPin: Boolean,
        linkedGmail: String = "",
        assignedFunctions: List<String> = emptyList(),
        customSecondaryPin: String = "",
        permissions: List<String> = emptyList(),
        onResult: (Boolean, AdminUser?, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.createNewAdmin(
                designation = designation,
                name = name,
                pin = pin,
                isAutoPin = isAutoPin,
                linkedGmail = linkedGmail,
                assignedFunctions = assignedFunctions,
                customSecondaryPin = customSecondaryPin,
                permissions = permissions
            )
            result.onSuccess { newAdmin ->
                onResult(true, newAdmin, null)
            }.onFailure { err ->
                onResult(false, null, err.localizedMessage)
            }
        }
    }

    fun regenerateSecondaryPin(adminId: String, customOtp: String? = null, onResult: (Boolean, String?, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.regenerateSecondaryPin(adminId, customOtp)
            result.onSuccess { newPin -> onResult(true, newPin, null) }
                .onFailure { err -> onResult(false, null, err.localizedMessage) }
        }
    }

    fun regenerateAllSubordinateOtps(adminIds: List<String>, onResult: (Boolean, Int, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.regenerateAllSubordinateOtps(adminIds)
            result.onSuccess { count -> onResult(true, count, null) }
                .onFailure { err -> onResult(false, 0, err.localizedMessage) }
        }
    }

    fun overwriteCategoryPermissions(
        categoryRank: Int,
        categoryName: String,
        assignedFunctions: List<String>,
        onResult: (Boolean, Int, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.overwriteCategoryPermissions(categoryRank, categoryName, assignedFunctions)
            result.onSuccess { count -> onResult(true, count, null) }
                .onFailure { err -> onResult(false, 0, err.localizedMessage) }
        }
    }

    fun updateDesignationFunctions(designationId: String, newFunctions: List<String>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.updateDesignationFunctions(designationId, newFunctions)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun createCustomDesignation(
        name: String,
        rank: Int,
        allowedFunctions: List<String>,
        description: String,
        onResult: (Boolean, DesignationAuthority?, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.createCustomDesignation(name, rank, allowedFunctions, description)
            result.onSuccess { desig -> onResult(true, desig, null) }
                .onFailure { err -> onResult(false, null, err.localizedMessage) }
        }
    }

    fun deleteCustomDesignation(designationId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteCustomDesignation(designationId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateAdminAssignedFunctions(adminId: String, functions: List<String>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.updateAdminAssignedFunctions(adminId, functions)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateAdminPermissions(
        adminId: String,
        permissions: List<String>,
        functions: List<String> = emptyList(),
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateAdminPermissions(adminId, permissions, functions)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateAdminPin(adminId: String, newPin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.updateAdminPin(adminId, newPin)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun assignOrUpdateHierarchicalRole(
        targetUserId: String,
        name: String,
        phone: String,
        roleTier: String,
        reportsToSeniorId: String,
        reportsToSeniorName: String,
        customOverrides: List<String>,
        p1PasswordInput: String,
        p2OtpInput: String,
        expectedP2Otp: String,
        isVerifiedBeliever: Boolean = false,
        existingAdminId: String? = null,
        onResult: (Boolean, AdminUser?, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.assignOrUpdateHierarchicalRole(
                targetUserId = targetUserId,
                name = name,
                phone = phone,
                roleTier = roleTier,
                reportsToSeniorId = reportsToSeniorId,
                reportsToSeniorName = reportsToSeniorName,
                customOverrides = customOverrides,
                p1PasswordInput = p1PasswordInput,
                p2OtpInput = p2OtpInput,
                expectedP2Otp = expectedP2Otp,
                isVerifiedBeliever = isVerifiedBeliever,
                existingAdminId = existingAdminId
            )
            result.onSuccess { assigned ->
                onResult(true, assigned, null)
            }.onFailure { err ->
                onResult(false, null, err.localizedMessage)
            }
        }
    }

    fun changeP1SecurityPassword(
        adminId: String,
        currentP1Input: String,
        newP1Input: String,
        isMasterOverride: Boolean = false,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.changeP1SecurityPassword(
                adminId = adminId,
                currentP1Input = currentP1Input,
                newP1Input = newP1Input,
                isMasterOverride = isMasterOverride
            )
            result.onSuccess {
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.localizedMessage)
            }
        }
    }

    fun blockDevicePermanently(adminId: String, deviceId: String = "", reason: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.blockDevicePermanently(adminId, deviceId, reason)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun demoteAdminToBeliever(adminId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.demoteAdminToBeliever(adminId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateMasterAdminSecurity(
        passwordEnabled: Boolean,
        pin: String,
        dualAuthEnabled: Boolean,
        secondaryPin: String,
        biometricTimeoutDays: Int = 30,
        biometricEnabled: Boolean = true,
        globalAuthBypass: Boolean = false,
        requireP2EveryLogin: Boolean = true,
        trustedDevicesList: List<String> = emptyList(),
        reminderIntervalDays: Int = 7,
        notificationMethod: String = "Local Notification",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateMasterAdminSecurity(
                passwordEnabled = passwordEnabled,
                pin = pin,
                dualAuthEnabled = dualAuthEnabled,
                secondaryPin = secondaryPin,
                biometricTimeoutDays = biometricTimeoutDays,
                biometricEnabled = biometricEnabled,
                globalAuthBypass = globalAuthBypass,
                requireP2EveryLogin = requireP2EveryLogin,
                trustedDevicesList = trustedDevicesList,
                reminderIntervalDays = reminderIntervalDays,
                notificationMethod = notificationMethod
            )
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateChatSettings(
        isChatEnabled: Boolean,
        chatAllowOnlyVerified: Boolean,
        chatWhitelistedUserIds: List<String>,
        chatAllowedRoles: List<String>
    ) {
        preferencesManager.updateChatSettings(
            isChatEnabled = isChatEnabled,
            chatAllowOnlyVerified = chatAllowOnlyVerified,
            chatWhitelistedUserIds = chatWhitelistedUserIds,
            chatAllowedRoles = chatAllowedRoles
        )
    }

    fun updateDelegatedGlobalEventCreators(creators: List<String>) {
        preferencesManager.updateDelegatedGlobalEventCreators(creators)
    }

    fun toggleAdminStatus(adminId: String, isEnabled: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.toggleAdminStatus(adminId, isEnabled)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun deleteAdmin(adminId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteAdmin(adminId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun linkAdminGmail(adminId: String, gmail: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.linkGmail(adminId, gmail)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun postSpecialAnnouncement(
        title: String,
        message: String,
        actionUrl: String = "",
        isPermanent: Boolean = true,
        durationHours: Int = 0,
        durationDays: Int = 0,
        expiresAtTimestamp: Long = 0L,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.postSpecialAnnouncement(
                title, message, actionUrl, isPermanent, durationHours, durationDays, expiresAtTimestamp
            )
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun deleteAnnouncement(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteAnnouncement(id)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateAdminTodayScripture(
        bookAndVerse: String,
        hindiText: String,
        referenceText: String,
        reflectionThought: String,
        isPermanent: Boolean = true,
        durationHours: Int = 0,
        durationDays: Int = 0,
        expiresAtTimestamp: Long = 0L,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateTodayScripture(
                bookAndVerse, hindiText, referenceText, reflectionThought,
                isPermanent, durationHours, durationDays, expiresAtTimestamp
            )
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateAdminLiveStream(
        isLive: Boolean,
        title: String,
        subtitle: String,
        url: String,
        scheduledTime: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateLiveStream(isLive, title, subtitle, url, scheduledTime)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateNavigationConfig(
        tabs: List<NavigationTabConfig>,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateNavigationConfig(tabs)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun updateReminderScheduleConfig(
        config: AdminReminderScheduleConfig,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = adminRepository.updateReminderScheduleConfig(config)
            result.onSuccess {
                applyReminderScheduleLocally(config)
                onResult(true, null)
            }.onFailure { err ->
                onResult(false, err.localizedMessage)
            }
        }
    }

    fun applyReminderScheduleLocally(config: AdminReminderScheduleConfig) {
        val currentProfile = userProfile.value
        val shouldApply = when (config.targetScope) {
            ReminderTargetScope.ALL_USERS -> true
            ReminderTargetScope.ONLY_UNMODIFIED_DEFAULTS -> {
                !preferencesManager.isPrayerTimeCustomizedByUser() ||
                !preferencesManager.isVerseAlarmTimeCustomizedByUser() ||
                !preferencesManager.isReadingReminderTimeCustomizedByUser()
            }
            ReminderTargetScope.SPECIFIC_PROFILE -> {
                config.targetProfileId.isNotBlank() && (
                    config.targetProfileId == currentProfile.deviceId ||
                    config.targetProfileId == currentProfile.phoneNumber ||
                    config.targetProfileId == currentProfile.displayName
                )
            }
        }

        if (!shouldApply) return

        val context = getApplication<Application>()
        val forceAll = config.targetScope == ReminderTargetScope.ALL_USERS ||
                config.targetScope == ReminderTargetScope.SPECIFIC_PROFILE

        // 1. Prayer time
        if (forceAll || !preferencesManager.isPrayerTimeCustomizedByUser()) {
            preferencesManager.updateDailyPrayerReminderTime(config.prayerHour, config.prayerMinute, isManual = false)
            preferencesManager.updateDailyPrayerReminderEnabled(config.prayerEnabled)
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                context,
                config.prayerHour,
                config.prayerMinute,
                config.prayerEnabled
            )
        }

        // 2. Verse alarm time
        if (forceAll || !preferencesManager.isVerseAlarmTimeCustomizedByUser()) {
            preferencesManager.updateVerseAlarmTime(config.verseAlarmHour, config.verseAlarmMinute, isManual = false)
            preferencesManager.updateVerseAlarmEnabled(config.verseAlarmEnabled)
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(context, preferencesManager.settings.value)
        }

        // 3. Bible reading reminder time
        if (forceAll || !preferencesManager.isReadingReminderTimeCustomizedByUser()) {
            preferencesManager.updateReadingPlanReminderTime(config.readingMorningHour, config.readingMorningMinute, isManual = false)
            preferencesManager.updateReadingPlanReminderEveningTime(config.readingEveningHour, config.readingEveningMinute, isManual = false)
            preferencesManager.updateReadingPlanReminderEnabled(config.readingPlanEnabled)
            com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(context, preferencesManager.settings.value)
        }
    }

    // --- Church Members ---
    fun addOrUpdateChurchMember(member: ChurchMember, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.addOrUpdateMember(member)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun deleteChurchMember(memberId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteMember(memberId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    // --- Attendance Tracker ---
    fun recordChurchAttendance(record: ChurchAttendanceRecord, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.recordAttendance(record)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun deleteChurchAttendance(recordId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteAttendance(recordId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    // --- Church Accounts ---
    fun addAccountTransaction(transaction: ChurchAccountTransaction, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.addAccountTransaction(transaction)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    fun deleteAccountTransaction(transactionId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deleteAccountTransaction(transactionId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    // --- Push Notifications ---
    fun sendAdminPushNotification(notification: AdminPushNotification, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.sendPushNotification(notification)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    // --- Fellowship Events Management ---
    fun addOrUpdateFellowshipEvent(event: com.example.data.model.FellowshipEvent, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            firebaseDataRepository.addOrUpdateFellowshipEvent(
                event = event,
                onSuccess = { saved ->
                    adminRepository.logActivity(
                        actionType = "EVENT_SAVED",
                        description = "कार्यक्रम सहेजा गया: ${saved.title} (${saved.dateString})",
                        targetId = saved.id
                    )
                    onResult(true, null)
                },
                onError = { err -> onResult(false, err) }
            )
        }
    }

    fun deleteFellowshipEvent(eventId: String, eventTitle: String = "", onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            firebaseDataRepository.deleteFellowshipEvent(
                eventId = eventId,
                onSuccess = {
                    adminRepository.logActivity(
                        actionType = "EVENT_DELETED",
                        description = "कार्यक्रम हटाया गया: ${if (eventTitle.isNotBlank()) eventTitle else eventId}",
                        targetId = eventId
                    )
                    onResult(true, null)
                },
                onError = { err -> onResult(false, err) }
            )
        }
    }

    fun deleteAdminPushNotification(notificationId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deletePushNotification(notificationId)
            result.onSuccess { onResult(true, null) }
                .onFailure { err -> onResult(false, err.localizedMessage) }
        }
    }

    // --- Export / Report Helpers ---
    fun getAttendanceReport(): String = adminRepository.generateAttendanceReport()
    fun getAccountsReport(): String = adminRepository.generateAccountsReport()
    fun getMembersReport(): String = adminRepository.generateMembersReport()

    // Firebase Realtime Database: Single String Values & Dynamic Controls
    val firebaseDataRepository = com.example.data.repository.FirebaseDataRepository.getInstance()
    val songSpreadsheetUrl: StateFlow<String> = firebaseDataRepository.songSpreadsheetUrl
    val todayScripture: StateFlow<String> = firebaseDataRepository.todayScripture

    val userProfileRepository = com.example.data.repository.UserProfileRepository(
        application,
        AppDatabase.getInstance(application),
        bibleDatabase,
        firebaseDataRepository,
        preferencesManager
    )
    val userProfile: StateFlow<UserProfileData> = userProfileRepository.userProfile

    fun updateUserProfile(profile: UserProfileData) {
        userProfileRepository.updateProfile(profile)
    }

    suspend fun saveUserAvatar(uri: android.net.Uri): String {
        return userProfileRepository.saveImageFromUri(uri)
    }

    suspend fun saveCroppedAvatarBitmap(bitmap: android.graphics.Bitmap): String {
        return userProfileRepository.saveCroppedBitmap(bitmap)
    }

    fun loginVinayKumarAutomatic(onResult: (Boolean, AdminUser?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = adminRepository.loginVinayKumarAutomatic()
            result.onSuccess { admin ->
                val currentProfile = userProfile.value
                userProfileRepository.updateProfile(
                    currentProfile.copy(
                        role = admin.designation,
                        displayName = if (currentProfile.displayName.isBlank() || currentProfile.displayName == "अतिथि विश्वासी" || currentProfile.displayName == "विश्वासी") admin.name else currentProfile.displayName
                    )
                )
                onResult(true, admin)
            }.onFailure {
                onResult(false, null)
            }
        }
    }

    fun getActivityHistory(filterType: UserActivityType = UserActivityType.ALL): Flow<List<UserActivityItem>> {
        return userProfileRepository.getActivityHistoryFlow(filterType)
    }

    val allNotifications: StateFlow<List<com.example.data.local.NotificationEntity>> = notificationRepository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeAdminNotice: StateFlow<com.example.data.model.AdminNotice?> = adminNoticeRepository.activeNotice

    // Remote Config dynamic parameters & Vlog Master Override
    val isVlogServerEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigHelper.isVlogServerEnabled
    val isSearchEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigManager.isSearchEnabled
    val appNoticeHeading: StateFlow<String> = com.example.util.RemoteConfigManager.appNoticeHeading
    val dailyGreetingText: StateFlow<String> = com.example.util.RemoteConfigHelper.dailyGreetingText
    val dailyGreetingConfig: StateFlow<com.example.data.model.DailyGreetingConfig> = firebaseDataRepository.dailyGreetingConfig
    val verseOfTheDayText: StateFlow<String> = com.example.util.RemoteConfigHelper.verseOfTheDayText
    val specialAnnouncementText: StateFlow<String> = com.example.util.RemoteConfigHelper.specialAnnouncementText
    val themePrimaryColor: StateFlow<String> = com.example.util.RemoteConfigHelper.themePrimaryColor
    val themeSecondaryColor: StateFlow<String> = com.example.util.RemoteConfigHelper.themeSecondaryColor

    fun updateGlobalThemeColors(primaryHex: String, secondaryHex: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                com.example.util.RemoteConfigHelper.updateGlobalThemeColors(primaryHex, secondaryHex)
                onResult?.invoke(true)
            } catch (e: Exception) {
                onResult?.invoke(false)
            }
        }
    }
    val isLiveStreamEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigHelper.isLiveStreamEnabled
    val isGalleryEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigHelper.isGalleryEnabled
    val isPrayerRequestEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigHelper.isPrayerRequestEnabled
    val promoBannerImageUrl: StateFlow<String> = com.example.util.RemoteConfigHelper.promoBannerImageUrl
    val promoBannerTitle: StateFlow<String> = com.example.util.RemoteConfigHelper.promoBannerTitle
    val promoBannerLinkUrl: StateFlow<String> = com.example.util.RemoteConfigHelper.promoBannerLinkUrl
    val isPromoBannerEnabled: StateFlow<Boolean> = com.example.util.RemoteConfigHelper.isPromoBannerEnabled

    // Live Stream, Prayer Requests, Banners & Devotional
    val liveStreamInfo = firebaseDataRepository.liveStreamInfo
    val prayerRequests = firebaseDataRepository.prayerRequests
    val prayerRequestsConfig = firebaseDataRepository.prayerRequestsConfig
    val featuredBanners = firebaseDataRepository.featuredBanners
    val dailyAudioDevotional = firebaseDataRepository.dailyAudioDevotional
    val adminPin = firebaseDataRepository.adminPin
    val quickAccessConfig = firebaseDataRepository.quickAccessConfig
    val videoQuickAccessConfig = firebaseDataRepository.videoQuickAccessConfig
    val homeSectionsConfig = firebaseDataRepository.homeSectionsConfig

    fun submitPrayerRequest(
        name: String,
        city: String,
        pastorName: String = "",
        userRole: String = "विश्वासी",
        isUrgent: Boolean = false,
        requestText: String,
        isPrivate: Boolean,
        senderDeviceId: String = "",
        category: String = "अन्य",
        tags: List<String> = listOf("अन्य"),
        onResult: (Boolean, Int?, String?) -> Unit
    ) {
        firebaseDataRepository.submitPrayerRequest(
            name = name,
            city = city,
            pastorName = pastorName,
            userRole = userRole,
            isUrgent = isUrgent,
            requestText = requestText,
            isPrivate = isPrivate,
            senderDeviceId = senderDeviceId,
            category = category,
            tags = tags,
            onSuccess = { _, serial -> onResult(true, serial, null) },
            onError = { err -> onResult(false, null, err) }
        )
    }

    fun submitPrayerRequest(name: String, city: String, requestText: String, isPrivate: Boolean, onResult: (Boolean, String?) -> Unit) {
        firebaseDataRepository.submitPrayerRequest(
            name = name,
            city = city,
            pastorName = "",
            requestText = requestText,
            isPrivate = isPrivate,
            senderDeviceId = "",
            onSuccess = { _, _ -> onResult(true, null) },
            onError = { err -> onResult(false, err) }
        )
    }

    fun markPrayerAsAnswered(requestId: String, testimonyText: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        firebaseDataRepository.markPrayerAsAnswered(
            requestId = requestId,
            testimonyText = testimonyText,
            onSuccess = { onResult(true, null) },
            onError = { err -> onResult(false, err) }
        )
    }

    fun replyToPrayerRequest(
        requestId: String,
        replyText: String,
        authorName: String,
        authorDesignation: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        firebaseDataRepository.replyToPrayerRequest(
            requestId = requestId,
            replyText = replyText,
            authorName = authorName,
            authorDesignation = authorDesignation,
            onSuccess = { onResult(true, null) },
            onError = { err -> onResult(false, err) }
        )
    }

    fun deletePrayerRequest(
        item: com.example.data.model.PrayerRequestItem,
        deletedByRole: String,
        deletedByDeviceId: String,
        adminPinUsed: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        firebaseDataRepository.deletePrayerRequest(
            requestId = item.id,
            deletedItem = item,
            deletedByRole = deletedByRole,
            deletedByDeviceId = deletedByDeviceId,
            adminPinUsed = adminPinUsed,
            onSuccess = { onResult(true, null) },
            onError = { err -> onResult(false, err) }
        )
    }

    fun deletePrayerRequest(requestId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val cached = prayerRequests.value.find { it.id == requestId }
        firebaseDataRepository.deletePrayerRequest(
            requestId = requestId,
            deletedItem = cached,
            deletedByRole = "AUTHOR",
            deletedByDeviceId = "",
            adminPinUsed = "",
            onSuccess = { onResult(true, null) },
            onError = { err -> onResult(false, err) }
        )
    }


    fun revertAnsweredToPrayer(requestId: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        firebaseDataRepository.revertAnsweredToPrayer(
            requestId = requestId,
            onSuccess = { onResult(true, null) },
            onError = { err -> onResult(false, err) }
        )
    }

    fun incrementPrayingCount(requestId: String) {
        firebaseDataRepository.incrementPrayingCount(requestId)
    }

    fun incrementPrayingCountWithLimit(
        context: android.content.Context,
        requestId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (requestId.isBlank()) {
            onResult(false, "अमान्य प्रार्थना आईडी")
            return
        }
        val config = prayerRequestsConfig.value
        val maxLimit = config.maxDailyPrayTapsPerUser.coerceAtLeast(1)
        val currentTaps = com.example.util.UserDeviceHelper.getTodayPrayTaps(context, requestId)
        if (currentTaps >= maxLimit) {
            onResult(false, "आज के लिए आपकी प्रार्थना दर्ज हो चुकी है! (मास्टर एडमिन सीमा: $maxLimit बार/दिन)")
        } else {
            val newCount = com.example.util.UserDeviceHelper.incrementTodayPrayTaps(context, requestId)
            incrementPrayingCount(requestId)
            onResult(true, "आपकी प्रार्थना दर्ज की गई! 🙏 ($newCount/$maxLimit आज)")
        }
    }

    fun updatePrayerRequestsConfig(config: com.example.data.model.PrayerRequestsConfig, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.updatePrayerRequestsConfig(config, onComplete)
    }

    // Dual-Layer Vlog evaluation: Condition A (Local) && Condition B (Server DB & RemoteConfig)
    // EXCEPTION: In "Vinay Kumar Avj" profile, all content is always allowed even if Firebase has set the switch to OFF.
    val isPersonalVlogAllowed: StateFlow<Boolean> = combine(
        settings,
        com.example.util.RemoteConfigHelper.isVlogServerEnabled,
        firebaseDataRepository.isPersonalVlogEnabled,
        com.example.util.ProfileManager.activeProfileFlow
    ) { currentSettings, remoteConfigEnabled, firebaseDbEnabled, activeProfile ->
        if (activeProfile == com.example.data.model.AppProfile.VINAY) {
            true
        } else {
            val conditionA = currentSettings.showPersonalVlog || currentSettings.personalVlogMode != com.example.data.model.PersonalVlogMode.HIDDEN
            val conditionB = remoteConfigEnabled && firebaseDbEnabled
            conditionA && conditionB
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun isSearchFeatureEnabled(): Boolean = com.example.util.RemoteConfigManager.isSearchEnabled()
    fun getRemoteNoticeHeading(): String = com.example.util.RemoteConfigManager.getAppNoticeHeading()
    fun getActiveTodayScripture(): String = com.example.data.repository.FirebaseDataRepository.getInstance().getTodayScripture()
    fun getActiveSongSpreadsheetUrl(): String = com.example.data.repository.FirebaseDataRepository.getInstance().getSongSpreadsheetUrl()

    init {
        preferencesManager.enableAdminLockSystem()
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(force = false)
            adminRepository.setAdminAuthRequired(true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            lyricsRepository.initializePreloadedLyrics()
            com.example.service.AppFirebaseMessagingService.initialize(getApplication())
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOffline = MutableStateFlow(!isOnline())
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Posts stream responding immediately to dual-layer Personal Vlog status
    val allPosts: StateFlow<List<BlogPost>> = isPersonalVlogAllowed
        .flatMapLatest { isAllowed ->
            bloggerRepository.getPostsFlow(isAllowed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fellowshipPosts: StateFlow<List<BlogPost>> = bloggerRepository
        .getPostsBySourceFlow(BlogSourceType.FELLOWSHIP_EVENTS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalVlogPosts: StateFlow<List<BlogPost>> = combine(
        bloggerRepository.getPostsBySourceFlow(BlogSourceType.PERSONAL_VLOG),
        isPersonalVlogAllowed
    ) { posts, isAllowed ->
        if (isAllowed) posts else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Photos derived from visible posts
    val galleryPhotos: StateFlow<List<GalleryPhoto>> = allPosts
        .map { posts -> bloggerRepository.extractPhotos(posts) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Asynchronously fetched and bound active reading plans with progress and custom tint colors
    val activeReadingPlans: StateFlow<List<ActiveReadingPlanItem>> = combine(
        settings,
        readingPlanRepository.getAllProgress()
    ) { currentSettings, progressList ->
        withContext(Dispatchers.IO) {
            val activeIds = currentSettings.activePlanIds
            if (activeIds.isEmpty()) {
                return@withContext emptyList()
            }

            val predefined = readingPlanRepository.getAllPlans()
            val manualJson = preferencesManager.getManualPlansJson()
            val manual = if (manualJson.isNotBlank()) {
                try {
                    com.example.data.bible.model.ManualPlanData.deserializeList(manualJson).map { it.toReadingPlanInfo() }
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            val allPlans = predefined + manual
            val plansMap = allPlans.associateBy { it.id }

            val progressByPlan = progressList
                .filter { it.isCompleted }
                .groupBy { it.planId }

            val behindColor = parsePlanColor(currentSettings.planBehindColorHex, Color(0xFFEF4444))
            val onTrackColor = parsePlanColor(currentSettings.planOnTrackColorHex, Color(0xFFEAB308))
            val completedColor = parsePlanColor(currentSettings.planCompletedColorHex, Color(0xFF10B981))

            activeIds.mapNotNull { planId ->
                val plan = plansMap[planId] ?: return@mapNotNull null
                val completedCount = progressByPlan[planId]?.size ?: 0
                val fraction = if (plan.totalDays > 0) {
                    (completedCount.toFloat() / plan.totalDays.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val percent = (fraction * 100).toInt()

                val tint = when {
                    completedCount >= plan.totalDays && plan.totalDays > 0 -> completedColor
                    completedCount > 0 -> onTrackColor
                    else -> behindColor
                }

                val completedDaysSet = progressByPlan[planId]?.map { it.dayNumber }?.toSet() ?: emptySet()
                val nextDayNumber = if (plan.totalDays > 0) {
                    ((1..plan.totalDays) - completedDaysSet).minOrNull() ?: plan.totalDays
                } else 1
                val dayPortion = plan.days.find { it.dayNumber == nextDayNumber }?.portions?.firstOrNull()
                val targetBookId = dayPortion?.bookId ?: 43
                val targetChapter = dayPortion?.startChapter ?: 1
                val targetStartChapter = dayPortion?.startChapter ?: 1
                val targetEndChapter = dayPortion?.endChapter ?: targetStartChapter
                val targetStartVerse = dayPortion?.startVerse ?: 1
                val totalChapterVerses = com.example.data.bible.model.BibleVerseCounts.getVerseCount(targetBookId, targetEndChapter)
                val targetEndVerse = dayPortion?.endVerse ?: (if (totalChapterVerses > 0) totalChapterVerses else 30)

                ActiveReadingPlanItem(
                    planId = plan.id,
                    title = plan.titleHindi.ifBlank { plan.titleEnglish },
                    totalDays = plan.totalDays,
                    completedDays = completedCount,
                    progressPercent = percent,
                    progressFraction = fraction,
                    tintColor = tint,
                    currentDayNumber = nextDayNumber,
                    targetBookId = targetBookId,
                    targetChapter = targetChapter,
                    targetStartChapter = targetStartChapter,
                    targetEndChapter = targetEndChapter,
                    targetStartVerse = targetStartVerse,
                    targetEndVerse = targetEndVerse
                )
            }
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Real-time Reading Insights & Streaks calculated reactively from Room SQLite progress
    val readingInsights: StateFlow<com.example.data.bible.model.ReadingInsightsData> = combine(
        settings,
        readingPlanRepository.getAllProgress()
    ) { currentSettings, progressList ->
        withContext(Dispatchers.IO) {
            val predefined = readingPlanRepository.getAllPlans()
            val manualJson = preferencesManager.getManualPlansJson()
            val manual = if (manualJson.isNotBlank()) {
                try {
                    com.example.data.bible.model.ManualPlanData.deserializeList(manualJson).map { it.toReadingPlanInfo() }
                } catch (_: Exception) {
                    emptyList()
                }
            } else emptyList()

            val allPlans = predefined + manual
            val activeIds = currentSettings.activePlanIds
            com.example.data.bible.model.ReadingInsightsCalculator.computeInsights(
                progressList = progressList,
                allPlans = allPlans,
                activePlanIds = activeIds
            )
        }
    }.flowOn(Dispatchers.IO)
    .stateIn(viewModelScope, SharingStarted.Lazily, com.example.data.bible.model.ReadingInsightsData())

    // Dynamic categories extracted from current posts
    val fellowshipCategories: StateFlow<List<String>> = fellowshipPosts
        .map { posts ->
            posts.flatMap { it.labels }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalVlogCategories: StateFlow<List<String>> = personalVlogPosts
        .map { posts ->
            posts.flatMap { it.labels }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // YouTube channel videos (Raw)
    private val rawYoutubeVideos: StateFlow<List<YouTubeVideo>> = youtubeRepository
        .getAllVideosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered YouTube channel videos
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = rawYoutubeVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic YouTube Playlists
    val youtubePlaylists: StateFlow<List<YouTubePlaylist>> = youtubeRepository
        .playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PredefinedPlaylists.items)

    // YouTube Channel Default: AVJ Worship (MANDATORY REQUIREMENT)
    private val _selectedChannel = MutableStateFlow(PredefinedPlaylists.channelWorship)
    val selectedChannel: StateFlow<YouTubeChannelInfo> = _selectedChannel.asStateFlow()

    // Active playlist videos state
    private val _currentPlaylistVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val currentPlaylistVideos: StateFlow<List<YouTubeVideo>> = _currentPlaylistVideos.asStateFlow()

    private val _isLoadingPlaylist = MutableStateFlow(false)
    val isLoadingPlaylist: StateFlow<Boolean> = _isLoadingPlaylist.asStateFlow()

    // Endless scrolling / Pagination state for Video RecyclerView & Feeds
    private val _isLoadingMoreVideos = MutableStateFlow(false)
    val isLoadingMoreVideos: StateFlow<Boolean> = _isLoadingMoreVideos.asStateFlow()

    private val channelPageTokens = java.util.concurrent.ConcurrentHashMap<String, String?>()
    private val channelHasMoreMap = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private var hasMoreYouTube = true

    // Upcoming Events: Extracted only from Fellowship Events with label "Upcoming"
    val upcomingEvents: StateFlow<List<UpcomingEvent>> = fellowshipPosts
        .map { posts ->
            posts.mapNotNull { EventExtractor.extractUpcomingEvent(it) }
                .sortedBy { it.startTimestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct Firebase Realtime Database Fellowship Events (Calendar & Meetings)
    val fellowshipEvents: StateFlow<List<com.example.data.model.FellowshipEvent>> = firebaseDataRepository
        .fellowshipEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.local.PredefinedData.hardcodedFellowshipEvents)

    // User's active RSVPs stored locally in state
    private val _userRsvps = MutableStateFlow<Map<String, com.example.data.model.EventRsvp>>(emptyMap())
    val userRsvps: StateFlow<Map<String, com.example.data.model.EventRsvp>> = _userRsvps.asStateFlow()

    // Latest 2 YouTube videos across channels (sorted newest first, no duplicates)
    val latestYouTubeVideos: StateFlow<List<YouTubeVideo>> = youtubeVideos
        .map { videos ->
            videos.sortedByDescending { it.publishedTimestamp }.take(2)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Items
    val savedItems: StateFlow<List<SavedItemEntity>> = savedItemRepository
        .getAllSavedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Intentional Randomized Feed (Blogs + Videos)
    private val _mixedRandomFeed = MutableStateFlow<List<MixedFeedItem>>(emptyList())
    val mixedRandomFeed: StateFlow<List<MixedFeedItem>> = _mixedRandomFeed.asStateFlow()

    // Recently Viewed
    val recentlyViewed: StateFlow<List<RecentlyViewedEntity>> = recentlyViewedRepository
        .getRecentItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Global Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSearchFilter = MutableStateFlow("ALL") // ALL, BLOG, VIDEO, PLAYLIST, BIBLE
    val selectedSearchFilter: StateFlow<String> = _selectedSearchFilter.asStateFlow()

    val globalSearchResults: StateFlow<List<SearchResultItem>> = combine(
        searchQuery.debounce(500L),
        selectedSearchFilter,
        allPosts,
        youtubeVideos
    ) { query, filter, posts, videos ->
        if (query.isBlank()) {
            return@combine emptyList<SearchResultItem>()
        }
        val q = query.trim().lowercase()
        val results = mutableListOf<SearchResultItem>()

        // 1. Blogs (Fellowship + Personal Vlog if enabled)
        if (filter == "ALL" || filter == "BLOG") {
            posts.filter { post ->
                post.title.lowercase().contains(q) ||
                post.plainTextExcerpt.lowercase().contains(q) ||
                post.labels.any { it.lowercase().contains(q) }
            }.take(20).forEach { post ->
                results.add(
                    SearchResultItem(
                        id = "POST_${post.id}",
                        type = SearchResultType.BLOG,
                        title = post.title,
                        snippet = post.plainTextExcerpt.take(120),
                        imageUrl = post.featuredImageUrl,
                        post = post
                    )
                )
            }
        }

        // 2. YouTube Videos
        if (filter == "ALL" || filter == "VIDEO") {
            videos.filter { vid ->
                vid.title.lowercase().contains(q) ||
                vid.description.lowercase().contains(q) ||
                vid.channelTitle.lowercase().contains(q)
            }.take(15).forEach { vid ->
                results.add(
                    SearchResultItem(
                        id = "VID_${vid.id}",
                        type = SearchResultType.VIDEO,
                        title = vid.title,
                        snippet = "${vid.channelTitle} • ${vid.publishedAt}",
                        imageUrl = vid.thumbnailUrl,
                        video = vid
                    )
                )
            }
        }

        // 3. Playlists
        if (filter == "ALL" || filter == "PLAYLIST") {
            PredefinedPlaylists.items.filter { pl ->
                pl.title.lowercase().contains(q) ||
                pl.channelTitle.lowercase().contains(q)
            }.forEach { pl ->
                results.add(
                    SearchResultItem(
                        id = "PL_${pl.id}",
                        type = SearchResultType.PLAYLIST,
                        title = pl.title,
                        snippet = "Playlist • ${pl.channelTitle}",
                        playlist = pl
                    )
                )
            }
        }

        // 4. Bible Verses (Simple lookup from predefined books or match)
        if (filter == "ALL" || filter == "BIBLE") {
            com.example.data.bible.model.BibleBookDefinitions.books.filter { book ->
                book.nameEnglish.lowercase().contains(q) ||
                book.nameHindi.contains(q)
            }.take(5).forEach { book ->
                results.add(
                    SearchResultItem(
                        id = "BIBLE_${book.id}",
                        type = SearchResultType.BIBLE,
                        title = "${book.nameEnglish} (${book.nameHindi})",
                        snippet = "Holy Bible • ${book.testament.name} • ${book.chapterCount} Chapters",
                        bibleBookId = book.id.toString(),
                        bibleChapter = 1,
                        bibleVerse = 1
                    )
                )
            }
        }

        results
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshAll()
        // Synchronized feed generator from local Room database flow
        viewModelScope.launch {
            combine(allPosts, youtubeVideos, personalVlogPosts) { posts, videos, vlogs ->
                Triple(posts, videos, vlogs)
            }.collect { (posts, videos, vlogs) ->
                if (_mixedRandomFeed.value.isEmpty() && (posts.isNotEmpty() || videos.isNotEmpty() || vlogs.isNotEmpty())) {
                    buildMixedRandomFeed()
                }
            }
        }
        // Schedule alarms on launch if enabled
        val s = preferencesManager.settings.value
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
        if (s.readingPlanReminderEnabled) {
            com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(getApplication(), s)
        }
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                s.dailyPrayerReminderHour,
                s.dailyPrayerReminderMinute,
                s.dailyPrayerReminderEnabled
            )
        }

        viewModelScope.launch {
            adminRepository.reminderScheduleConfig.collect { config ->
                applyReminderScheduleLocally(config)
            }
        }

        viewModelScope.launch {
            if (currentAdmin.value == null) {
                val masterPasswordReq = preferencesManager.settings.value.masterAdminPasswordEnabled
                if (!masterPasswordReq || com.example.util.ProfileManager.isVinayProfile()) {
                    adminRepository.loginVinayKumarAutomatic()
                }
            }
        }
    }

    fun isOnline(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun buildMixedRandomFeed() {
        val basePosts = if (allPosts.value.isNotEmpty()) allPosts.value else fellowshipPosts.value
        val isPersonalVlogOn = isPersonalVlogAllowed.value && (preferencesManager.settings.value.personalVlogMode != com.example.data.model.PersonalVlogMode.HIDDEN || preferencesManager.settings.value.showPersonalVlog)
        val vlogPosts = if (isPersonalVlogOn) personalVlogPosts.value else emptyList()
        val posts = (basePosts + vlogPosts).distinctBy { it.id }
        val videos = if (youtubeVideos.value.isNotEmpty()) youtubeVideos.value else latestYouTubeVideos.value

        val merged = mutableListOf<MixedFeedItem>()
        posts.forEach { merged.add(MixedFeedItem.BlogPostItem(it)) }
        videos.forEach { merged.add(MixedFeedItem.VideoItem(it)) }

        // True random shuffle of the unified merged list
        _mixedRandomFeed.value = merged.shuffled(kotlin.random.Random(System.currentTimeMillis()))
    }

    fun shuffleMixedFeed() {
        buildMixedRandomFeed()
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            _isOffline.value = !isOnline()
            _errorMessage.value = null

            // 1. Synchronized Fetching: Wait for BOTH Blogger and YouTube data to fetch completely in the background to avoid UI jumping and race conditions
            val isVlogAllowed = isPersonalVlogAllowed.value
            val bloggerDeferred = async { bloggerRepository.refreshPosts(isVlogAllowed) }
            val ytDeferred = async { youtubeRepository.refreshChannelVideos() }
            val (bloggerRes, ytRes) = awaitAll(bloggerDeferred, ytDeferred)

            if (bloggerRes.isFailure && ytRes.isFailure && !isOnline()) {
                _isOffline.value = true
                _errorMessage.value = "You're offline. Showing saved content."
            } else if (bloggerRes.isFailure) {
                _errorMessage.value = "Fellowship Events feed is temporarily unavailable."
            }

            // 2. Intentional Shuffling: Merge both result lists into a single unified list and apply true random shuffle
            buildMixedRandomFeed()

            _isRefreshing.value = false
        }
    }

    fun setSelectedChannel(channel: YouTubeChannelInfo) {
        _selectedChannel.value = channel
    }

    fun loadPlaylistVideos(playlist: YouTubePlaylist) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingPlaylist.value = true
            val list = youtubeRepository.getPlaylistVideos(playlist)
            _currentPlaylistVideos.value = list
            _isLoadingPlaylist.value = false
        }
    }

    /**
     * Endless Scroll / Pagination Logic:
     * Initiates a background fetch for the next set of videos (YouTube channels, Firebase, & Dailymotion).
     * Saves new items into Room Database / Merged Feed and invokes [onAppended].
     */
    fun loadMoreVideos(targetChannelId: String? = null, onAppended: ((List<YouTubeVideo>) -> Unit)? = null) {
        if (_isLoadingMoreVideos.value) return
        if (!hasMoreYouTube) return

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMoreVideos.value = true
            val newlyFetched = mutableListOf<YouTubeVideo>()

            try {
                if (hasMoreYouTube) {
                    val channelsToFetch = if (targetChannelId == null) {
                        listOf(
                            PredefinedPlaylists.channelWorship,
                            PredefinedPlaylists.channelMain,
                            PredefinedPlaylists.channelNewCreationChurch
                        )
                    } else {
                        val single = when (targetChannelId) {
                            PredefinedPlaylists.channelWorship.id -> PredefinedPlaylists.channelWorship
                            PredefinedPlaylists.channelNewCreationChurch.id -> PredefinedPlaylists.channelNewCreationChurch
                            PredefinedPlaylists.channelMain.id -> PredefinedPlaylists.channelMain
                            else -> _selectedChannel.value
                        }
                        listOf(single)
                    }

                    for (ch in channelsToFetch) {
                        val canFetchChannel = channelHasMoreMap[ch.id] ?: true
                        if (canFetchChannel) {
                            val currentToken = channelPageTokens[ch.id]
                            val ytResult = youtubeRepository.fetchMoreChannelVideos(
                                channelId = ch.id,
                                channelTitle = ch.name,
                                pageToken = currentToken
                            )
                            channelPageTokens[ch.id] = ytResult.nextPageToken
                            channelHasMoreMap[ch.id] = ytResult.hasMore
                            if (ytResult.videos.isNotEmpty()) {
                                newlyFetched.addAll(ytResult.videos)
                            }
                        }
                    }

                    val anyChannelHasMore = if (targetChannelId == null) {
                        listOf(
                            PredefinedPlaylists.channelWorship.id,
                            PredefinedPlaylists.channelMain.id,
                            PredefinedPlaylists.channelNewCreationChurch.id
                        ).any { channelHasMoreMap[it] ?: true }
                    } else {
                        channelHasMoreMap[targetChannelId] ?: true
                    }
                    hasMoreYouTube = anyChannelHasMore
                }

                if (newlyFetched.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onAppended?.invoke(newlyFetched)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingMoreVideos.value = false
            }
        }
    }

    fun resetVideoPagination() {
        channelPageTokens.clear()
        channelHasMoreMap.clear()
        hasMoreYouTube = true
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSearchFilter(filter: String) {
        _selectedSearchFilter.value = filter
    }

    // Saved Items operations
    fun isSaved(id: String): Flow<Boolean> = savedItemRepository.isSavedFlow(id)

    fun toggleSaveItem(
        id: String,
        type: String,
        title: String,
        subtitle: String,
        imageUrl: String? = null,
        url: String? = null,
        extraDataJson: String? = null,
        isCurrentlySaved: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            savedItemRepository.toggleSave(
                SavedItemEntity(
                    id = id,
                    type = type,
                    title = title,
                    subtitle = subtitle,
                    imageUrl = imageUrl,
                    url = url,
                    extraDataJson = extraDataJson
                ),
                isCurrentlySaved
            )
        }
    }

    fun removeSavedItem(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            savedItemRepository.remove(id)
            if (id.startsWith("SONG_")) {
                val songId = id.removePrefix("SONG_").toLongOrNull()
                if (songId != null) {
                    lyricsRepository.setFavorite(songId, false)
                }
            }
        }
    }

    // Recently Viewed operations
    fun recordRecentlyViewed(
        id: String,
        type: String,
        title: String,
        subtitle: String,
        imageUrl: String? = null,
        extraDataJson: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recentlyViewedRepository.record(id, type, title, subtitle, imageUrl, extraDataJson)
        }
    }

    fun clearRecentlyViewed() {
        viewModelScope.launch(Dispatchers.IO) {
            recentlyViewedRepository.clearHistory()
        }
    }

    // Settings operations
    fun updateThemeMode(mode: ThemeMode) {
        preferencesManager.updateThemeMode(mode)
    }

    fun updateAppLanguage(lang: AppLanguage) {
        preferencesManager.updateAppLanguage(lang)
    }

    fun toggleFavoriteCategory(category: String) {
        val current = settings.value.favoriteCategories.toMutableSet()
        if (current.contains(category)) current.remove(category) else current.add(category)
        preferencesManager.updateFavoriteCategories(current)
    }

    fun toggleHomeSection(section: HomeSectionType, enabled: Boolean) {
        preferencesManager.toggleHomeSection(section, enabled)
    }

    fun updateHomeSectionsOrder(order: List<HomeSectionType>) {
        preferencesManager.updateHomeSectionsOrder(order)
    }

    fun updateShowFellowshipEvents(enabled: Boolean) {
        preferencesManager.updateShowFellowshipEvents(enabled)
    }

    fun updateShowPersonalVlog(enabled: Boolean) {
        preferencesManager.updateShowPersonalVlog(enabled)
        if (enabled) {
            viewModelScope.launch(Dispatchers.IO) {
                bloggerRepository.refreshPosts(showPersonalVlog = true)
            }
        }
    }

    fun updateShowYouTube(enabled: Boolean) {
        preferencesManager.updateShowYouTube(enabled)
    }

    fun updateShowShorts(enabled: Boolean) {
        preferencesManager.updateShowShorts(enabled)
    }

    fun updateNotifyFellowshipEvents(enabled: Boolean) {
        preferencesManager.updateNotifyFellowshipEvents(enabled)
    }

    fun updateNotifyYouTube(enabled: Boolean) {
        preferencesManager.updateNotifyYouTube(enabled)
    }

    fun updateNotifyPersonalVlog(enabled: Boolean) {
        preferencesManager.updateNotifyPersonalVlog(enabled)
    }

    fun updateNotifyUpcomingReminders(enabled: Boolean) {
        preferencesManager.updateNotifyUpcomingReminders(enabled)
    }

    val isPersonalVlogServerEnabled: StateFlow<Boolean> = firebaseDataRepository.isPersonalVlogEnabled

    fun updatePersonalVlogMode(mode: PersonalVlogMode) {
        preferencesManager.updatePersonalVlogMode(mode)
        if (mode != PersonalVlogMode.HIDDEN) {
            viewModelScope.launch(Dispatchers.IO) {
                bloggerRepository.refreshPosts(showPersonalVlog = true)
            }
        }
    }

    fun setGlobalPersonalVlogServerEnabled(enabled: Boolean) {
        firebaseDataRepository.setGlobalPersonalVlogServerEnabled(enabled)
    }

    fun updateDailyGreetingText(greeting: String) {
        firebaseDataRepository.updateDailyGreetingText(greeting)
    }

    fun updateDailyGreetingConfig(config: com.example.data.model.DailyGreetingConfig, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.updateDailyGreetingConfig(config, onComplete)
    }

    fun updateQuickAccessConfig(config: com.example.data.model.QuickAccessConfig) {
        firebaseDataRepository.updateQuickAccessConfig(config)
    }

    fun updateVideoQuickAccessConfig(config: com.example.data.model.VideoQuickAccessConfig, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.updateVideoQuickAccessConfig(config, onComplete)
    }

    fun resetVideoQuickAccessConfig(onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.updateVideoQuickAccessConfig(com.example.data.model.VideoQuickAccessConfig(), onComplete)
    }

    fun updateHomeSectionsConfig(config: com.example.data.model.HomeSectionsConfig, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.updateHomeSectionsConfig(config, onComplete)
    }

    fun resetHomeSectionsConfig(onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.updateHomeSectionsConfig(com.example.data.model.HomeSectionsConfig(), onComplete)
    }

    fun addOrUpdateYouTubePlaylist(playlist: com.example.data.model.YouTubePlaylist, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.addOrUpdateYouTubePlaylist(playlist, onComplete)
    }

    fun deleteYouTubePlaylist(playlistId: String, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.deleteYouTubePlaylist(playlistId, onComplete)
    }

    fun addOrUpdateCustomVideo(video: com.example.data.model.YouTubeVideo, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.addOrUpdateCustomVideo(video, onComplete)
    }

    fun deleteCustomVideo(videoId: String, onComplete: ((Boolean) -> Unit)? = null) {
        firebaseDataRepository.deleteCustomVideo(videoId, onComplete)
    }

    val pinnedVideoId: StateFlow<String?> = firebaseDataRepository.pinnedVideoId

    fun togglePinVideo(videoId: String, onComplete: ((Boolean, Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val currentPinned = firebaseDataRepository.pinnedVideoId.value
            val isCurrentlyPinned = (currentPinned == videoId)
            val newPinnedId = if (isCurrentlyPinned) null else videoId
            firebaseDataRepository.setPinnedVideoId(newPinnedId) { success ->
                onComplete?.invoke(success, !isCurrentlyPinned)
            }
        }
    }

    fun updatePersonalBlogPassword(password: String) {
        preferencesManager.updatePersonalBlogPassword(password)
    }

    fun updateInactiveAdminAutoDisableDays(days: Int) {
        preferencesManager.updateInactiveAdminAutoDisableDays(days)
    }

    fun updateGlobalAdminEmergencyLock(locked: Boolean) {
        preferencesManager.updateGlobalAdminEmergencyLock(locked)
        if (locked) {
            val admin = currentAdmin.value
            if (admin != null && admin.rank < com.example.data.model.AdminHierarchy.RANK_VINAY_KUMAR) {
                logoutAdmin()
            }
        }
    }

    fun hasSeenProfileAdminPrompt(): Boolean {
        return preferencesManager.settings.value.hasSeenProfileAdminPrompt
    }

    fun markProfileAdminPromptSeen() {
        preferencesManager.markProfileAdminPromptSeen()
    }

    fun clearActivityHistory() {
        clearRecentlyViewed()
    }

    fun updateBibleReadingStyle(style: BibleReadingStyle) {
        preferencesManager.updateBibleReadingStyle(style)
    }

    fun updateYouTubeDefaultTab(tab: YouTubeDefaultTab) {
        preferencesManager.updateYouTubeDefaultTab(tab)
    }

    fun updateCustomFourthTab(tab: CustomFourthTab) {
        preferencesManager.updateCustomFourthTab(tab)
    }

    fun updateBloggerPhotoLayout(layout: BloggerPhotoLayout) {
        preferencesManager.updateBloggerPhotoLayout(layout)
    }

    fun updateIsDrawerEnabled(enabled: Boolean) {
        preferencesManager.updateIsDrawerEnabled(enabled)
    }

    fun updateDrawerPosition(position: String) {
        preferencesManager.updateDrawerPosition(position)
    }

    fun updateNavTabsOrder(order: List<String>) {
        preferencesManager.updateNavTabsOrder(order)
    }

    fun updateActivePlanIds(ids: Set<String>) {
        preferencesManager.updateActivePlanIds(ids)
    }

    fun updateReadingPlanColors(behindHex: String, onTrackHex: String, completedHex: String) {
        preferencesManager.updateReadingPlanColors(behindHex, onTrackHex, completedHex)
    }

    fun updateDataSaver(enabled: Boolean) {
        preferencesManager.updateDataSaverEnabled(enabled)
    }

    fun scheduleEventReminder(
        context: Context,
        event: UpcomingEvent,
        offset: ReminderScheduler.ReminderOffset
    ): Boolean {
        return ReminderScheduler.scheduleReminder(
            context = context,
            postId = event.post.id,
            eventTitle = event.title,
            eventDate = event.dateString,
            eventTimestamp = event.startTimestamp,
            offset = offset
        )
    }

    fun submitFellowshipRsvp(
        eventId: String,
        userName: String,
        userContact: String,
        status: String = "GOING",
        attendeesCount: Int = 1,
        notes: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val rsvp = com.example.data.model.EventRsvp(
            eventId = eventId,
            userName = userName,
            userContact = userContact,
            status = status,
            attendeesCount = attendeesCount,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )
        // Store locally
        val current = _userRsvps.value.toMutableMap()
        current[eventId] = rsvp
        _userRsvps.value = current

        // Push to Firebase Realtime Database
        firebaseDataRepository.submitRsvp(
            rsvp = rsvp,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun cancelFellowshipRsvp(eventId: String) {
        val current = _userRsvps.value.toMutableMap()
        current.remove(eventId)
        _userRsvps.value = current
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            bloggerRepository.clearCache()
            youtubeRepository.clearCache()
            refreshAll()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun checkForAppUpdates(force: Boolean = true) {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(force)
        }
    }

    fun downloadAndInstallAppUpdate() {
        viewModelScope.launch {
            appUpdateManager.downloadAndInstallApk()
        }
    }

    fun installDownloadedApk() {
        appUpdateManager.installDownloadedApk()
    }

    fun speakUpdateAnnouncement() {
        val s = settings.value
        welcomeSpeechManager.speakUpdateAnnouncement(s.userName)
    }

    fun updateGitHubRepoPath(newPath: String) {
        appUpdateManager.updateCustomRepoPath(newPath)
    }

    fun triggerWelcomeSpeechOnLaunch() {
        val s = settings.value
        val todayVerse = VerseOfTheDay.getTodayVerse()
        val isUpdateAvail = updateState.value.isUpdateAvailable
        val effectiveSpeechVolume = if (s.syncGreetingVolumeWithAlarm) s.alarmVolume else s.greetingSpeechVolume
        welcomeSpeechManager.speakOnAppOpen(
            userName = s.userName,
            enableWelcomeSpeech = s.enableWelcomeSpeech,
            enableVerseSpeech = s.enableVerseSpeechOnLaunch,
            welcomeOncePerDay = s.welcomeSpeechOncePerDay,
            verseOncePerDay = s.verseSpeechOncePerDay,
            todayVerse = todayVerse,
            todaysVerseText = "${todayVerse.textHindi} - ${todayVerse.referenceHindi}",
            isUpdateAvailable = isUpdateAvail,
            speechPitch = s.greetingSpeechPitch,
            speechSpeed = s.greetingSpeechSpeed,
            speechVolume = effectiveSpeechVolume
        )
    }

    fun testWelcomeSpeech() {
        val s = settings.value
        val todayVerse = VerseOfTheDay.getTodayVerse()
        val effectiveSpeechVolume = if (s.syncGreetingVolumeWithAlarm) s.alarmVolume else s.greetingSpeechVolume
        welcomeSpeechManager.testSpeech(
            userName = s.userName,
            todayVerse = todayVerse,
            todaysVerseText = "${todayVerse.textHindi} - ${todayVerse.referenceHindi}",
            speechPitch = s.greetingSpeechPitch,
            speechSpeed = s.greetingSpeechSpeed,
            speechVolume = effectiveSpeechVolume
        )
    }

    fun updateWidgetAutoChangeIntervalHours(hours: Int) {
        preferencesManager.updateWidgetAutoChangeIntervalHours(hours)
        com.example.widget.BibleVerseWidgetProvider.updateAllWidgets(getApplication())
        com.example.widget.BibleVerseWidgetProvider.scheduleWidgetUpdate(getApplication(), hours)
    }

    fun updateUserName(name: String) = preferencesManager.updateUserName(name)
    fun updateEnableWelcomeSpeech(enabled: Boolean) = preferencesManager.updateEnableWelcomeSpeech(enabled)
    fun updateEnableVerseSpeechOnLaunch(enabled: Boolean) = preferencesManager.updateEnableVerseSpeechOnLaunch(enabled)
    fun updateWelcomeSpeechOncePerDay(oncePerDay: Boolean) = preferencesManager.updateWelcomeSpeechOncePerDay(oncePerDay)
    fun updateVerseSpeechOncePerDay(oncePerDay: Boolean) = preferencesManager.updateVerseSpeechOncePerDay(oncePerDay)
    fun updateWelcomeDialogDismissed(dismissed: Boolean) = preferencesManager.updateWelcomeDialogDismissed(dismissed)

    fun updateVerseAlarmEnabled(enabled: Boolean) {
        preferencesManager.updateVerseAlarmEnabled(enabled)
        val s = settings.value.copy(verseAlarmEnabled = enabled)
        if (enabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        } else {
            com.example.util.VerseAlarmScheduler.cancelAlarm(getApplication())
        }
    }

    fun updateVerseAlarmTime(hour: Int, minute: Int) {
        preferencesManager.updateVerseAlarmTime(hour, minute)
        val s = settings.value.copy(verseAlarmHour = hour, verseAlarmMinute = minute)
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
    }

    fun updateVerseAlarmFrequency(frequency: com.example.data.model.VerseAlarmFrequency) {
        preferencesManager.updateVerseAlarmFrequency(frequency)
        val s = settings.value.copy(verseAlarmFrequency = frequency)
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
    }

    fun updateVerseAlarmIntervalHours(intervalHours: Int) {
        preferencesManager.updateVerseAlarmIntervalHours(intervalHours)
        val s = settings.value.copy(verseAlarmIntervalHours = intervalHours)
        if (s.verseAlarmEnabled) {
            com.example.util.VerseAlarmScheduler.scheduleNextAlarm(getApplication(), s)
        }
    }

    fun updateVerseAlarmMode(mode: com.example.data.model.VerseAlarmMode) {
        preferencesManager.updateVerseAlarmMode(mode)
    }

    fun updateVerseAlarmContent(content: com.example.data.model.VerseAlarmContent) {
        preferencesManager.updateVerseAlarmContent(content)
    }

    fun updateSyncGreetingVolumeWithAlarm(sync: Boolean) {
        preferencesManager.updateSyncGreetingVolumeWithAlarm(sync)
    }

    fun updateGreetingSpeechVolume(volume: Float) {
        preferencesManager.updateGreetingSpeechVolume(volume)
    }

    fun updateGreetingSpeechPitch(pitch: Float) {
        preferencesManager.updateGreetingSpeechPitch(pitch)
    }

    fun updateGreetingSpeechSpeed(speed: Float) {
        preferencesManager.updateGreetingSpeechSpeed(speed)
    }

    fun updateAlarmVolume(volume: Float) {
        preferencesManager.updateAlarmVolume(volume)
    }

    fun updateReadingPlanReminderEnabled(enabled: Boolean) {
        preferencesManager.updateReadingPlanReminderEnabled(enabled)
        val s = settings.value.copy(readingPlanReminderEnabled = enabled)
        com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(getApplication(), s)
    }

    fun updateReadingPlanReminderTime(hour: Int, minute: Int) {
        preferencesManager.updateReadingPlanReminderTime(hour, minute)
        val s = settings.value.copy(readingPlanReminderHour = hour, readingPlanReminderMinute = minute)
        if (s.readingPlanReminderEnabled) {
            com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(getApplication(), s)
        }
    }

    fun updateReadingPlanReminderEveningEnabled(enabled: Boolean) {
        preferencesManager.updateReadingPlanReminderEveningEnabled(enabled)
        val s = settings.value.copy(readingPlanReminderEveningEnabled = enabled)
        com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(getApplication(), s)
    }

    fun updateReadingPlanReminderEveningTime(hour: Int, minute: Int) {
        preferencesManager.updateReadingPlanReminderEveningTime(hour, minute)
        val s = settings.value.copy(readingPlanReminderEveningHour = hour, readingPlanReminderEveningMinute = minute)
        if (s.readingPlanReminderEnabled) {
            com.example.util.ReadingPlanReminderScheduler.scheduleAllReminders(getApplication(), s)
        }
    }

    fun testVerseAlarm() {
        com.example.util.VerseAlarmScheduler.triggerTestAlarm(getApplication())
    }

    fun testVerseNotification() {
        com.example.util.VerseAlarmScheduler.triggerTestNotification(getApplication())
    }

    fun testReadingPlanReminder() {
        com.example.util.ReadingPlanReminderScheduler.triggerTestNotification(getApplication())
    }

    fun updateDailyPrayerReminderEnabled(enabled: Boolean) {
        preferencesManager.updateDailyPrayerReminderEnabled(enabled)
        val s = settings.value.copy(dailyPrayerReminderEnabled = enabled)
        com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
            getApplication(),
            s.dailyPrayerReminderHour,
            s.dailyPrayerReminderMinute,
            enabled
        )
    }

    fun updateDailyPrayerReminderTime(hour: Int, minute: Int) {
        preferencesManager.updateDailyPrayerReminderTime(hour, minute)
        val s = settings.value.copy(dailyPrayerReminderHour = hour, dailyPrayerReminderMinute = minute)
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                hour,
                minute,
                true
            )
        }
    }

    fun updateDailyPrayerReminderSlot(slot: DailyPrayerSlot) {
        preferencesManager.updateDailyPrayerReminderSlot(slot)
        val s = preferencesManager.settings.value
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                s.dailyPrayerReminderHour,
                s.dailyPrayerReminderMinute,
                true
            )
        }
    }

    fun updatePrayerSlotCustomTime(slot: DailyPrayerSlot, hour: Int, minute: Int) {
        preferencesManager.updatePrayerSlotCustomTime(slot, hour, minute)
        val s = preferencesManager.settings.value
        if (s.dailyPrayerReminderEnabled) {
            com.example.util.DailyPrayerReminderScheduler.scheduleDailyReminder(
                getApplication(),
                s.dailyPrayerReminderHour,
                s.dailyPrayerReminderMinute,
                true
            )
        }
    }

    fun testDailyPrayerReminder() {
        com.example.util.DailyPrayerReminderScheduler.triggerTestNotification(getApplication())
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }

    fun dismissAdminNotice(id: String) {
        adminNoticeRepository.dismissNotice(id)
    }

    fun addSampleNotification(title: String, body: String, linkUrl: String? = null) {
        viewModelScope.launch {
            notificationRepository.insertNotification(
                com.example.data.local.NotificationEntity(
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = "admin",
                    linkUrl = linkUrl
                )
            )
        }
    }

    fun isNotificationOnboardingCompleted(): Boolean = preferencesManager.isNotificationOnboardingCompleted()
    fun setNotificationOnboardingCompleted(completed: Boolean = true) = preferencesManager.setNotificationOnboardingCompleted(completed)
    fun isNotificationPromptShownForVersion(versionCode: Int): Boolean = preferencesManager.isNotificationPromptShownForVersion(versionCode)
    fun setNotificationPromptShownForVersion(versionCode: Int, shown: Boolean = true) = preferencesManager.setNotificationPromptShownForVersion(versionCode, shown)

    fun createPoll(question: String, optionsTexts: List<String>, targetAudience: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.createPoll(question, optionsTexts, targetAudience)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "त्रुटि हुई")
            }
        }
    }

    fun votePoll(pollId: String, optionId: String, userId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.votePoll(pollId, optionId, userId)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "त्रुटि हुई")
            }
        }
    }

    fun deletePoll(pollId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = adminRepository.deletePoll(pollId)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "त्रुटि हुई")
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(app)
            val bibleDb = BibleDatabase.getInstance(app)
            val bloggerRepo = BloggerRepository(db)
            val ytRepo = YouTubeRepository(db)
            val savedRepo = SavedItemRepository(db)
            val recentRepo = RecentlyViewedRepository(db)
            val prefs = PreferencesManager(app)
            val bibleDao = bibleDb.bibleDao()
            val readingPlanRepo = com.example.data.bible.repository.ReadingPlanRepository(bibleDao)
            val studyNotesRepo = com.example.data.bible.repository.StudyNotesRepository(bibleDao)
            val lyricsRepo = com.example.data.bible.repository.LyricsRepository(bibleDao, savedItemRepository = savedRepo)
            val backupRepo = com.example.data.bible.repository.BackupRepository(app, bibleDao)
            val localDataSource = com.example.data.bible.local.BibleLocalDataSource(app, bibleDao)
            val remoteDataSource = com.example.data.bible.remote.BibleRemoteDataSource()
            val bibleRepo = com.example.data.bible.repository.BibleRepository(app, localDataSource, remoteDataSource)
            val syncCenterRepo = com.example.data.repository.SyncCenterRepository(app, bloggerRepo, ytRepo, bibleRepo, lyricsRepo)

            return MainViewModel(
                app,
                bloggerRepo,
                ytRepo,
                savedRepo,
                recentRepo,
                prefs,
                bibleDb,
                readingPlanRepo,
                studyNotesRepo,
                lyricsRepo,
                backupRepo,
                syncCenterRepo,
                bibleRepo
            ) as T
        }
    }
}
