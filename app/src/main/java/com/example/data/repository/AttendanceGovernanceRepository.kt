package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.util.AttendanceSecurityHelper
import com.example.util.UserDeviceHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AttendanceGovernanceRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("attendance_governance_prefs", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // StateFlows
    private val _globalConfig = MutableStateFlow(AttendanceGlobalConfig())
    val globalConfig: StateFlow<AttendanceGlobalConfig> = _globalConfig.asStateFlow()

    private val _branchSettings = MutableStateFlow(BranchAttendanceSettings())
    val branchSettings: StateFlow<BranchAttendanceSettings> = _branchSettings.asStateFlow()

    private val _serviceSchedules = MutableStateFlow<List<ServiceSchedule>>(emptyList())
    val serviceSchedules: StateFlow<List<ServiceSchedule>> = _serviceSchedules.asStateFlow()

    private val _allAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = _allAttendanceRecords.asStateFlow()

    private val _todayAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val todayAttendanceRecords: StateFlow<List<AttendanceRecord>> = _todayAttendanceRecords.asStateFlow()

    private val _offlineQueue = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val offlineQueue: StateFlow<List<AttendanceRecord>> = _offlineQueue.asStateFlow()

    private val _offlineQueueCount = MutableStateFlow(0)
    val offlineQueueCount: StateFlow<Int> = _offlineQueueCount.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatusText = MutableStateFlow("✓ सिंक पूर्ण")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    private val _absenteeCareList = MutableStateFlow<List<AbsenteeCareMember>>(emptyList())
    val absenteeCareList: StateFlow<List<AbsenteeCareMember>> = _absenteeCareList.asStateFlow()

    private val _activeServiceWindow = MutableStateFlow(ActiveServiceWindowResult())
    val activeServiceWindow: StateFlow<ActiveServiceWindowResult> = _activeServiceWindow.asStateFlow()

    private val _todayMilestoneAlerts = MutableStateFlow<List<PastoralMilestoneAlert>>(emptyList())
    val todayMilestoneAlerts: StateFlow<List<PastoralMilestoneAlert>> = _todayMilestoneAlerts.asStateFlow()

    private val _familyUnits = MutableStateFlow<List<FamilyUnit>>(emptyList())
    val familyUnits: StateFlow<List<FamilyUnit>> = _familyUnits.asStateFlow()

    private var attendanceListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var schedulesListener: ListenerRegistration? = null

    init {
        loadCachedData()
        registerNetworkCallback()
        startFirestoreSync()
        refreshServiceWindow()
    }

    companion object {
        @Volatile
        private var INSTANCE: AttendanceGovernanceRepository? = null

        fun getInstance(context: Context): AttendanceGovernanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AttendanceGovernanceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun refreshServiceWindow() {
        val schedules = _serviceSchedules.value
        _activeServiceWindow.value = AttendanceSecurityHelper.evaluateActiveServiceWindow(schedules)
    }

    // =========================================================================
    // 1. ATOMIC CHECK-IN ENGINE (Dual-Mode: Usher Scan, Member Self-Scan & Manual Search)
    // =========================================================================

    suspend fun processCheckIn(
        memberSerial: String,
        memberName: String,
        gender: String = "Male",
        roleTier: String = "believer",
        checkInMethod: String = "usher_scan", // "usher_scan" | "venue_qr_static" | "venue_qr_dynamic" | "manual_usher_search"
        scannedVenueQrPayload: String? = null,
        userLat: Double? = null,
        userLng: Double? = null,
        verifiedByAdmin: AdminUser? = null,
        deviceId: String = UserDeviceHelper.getDeviceId(context)
    ): Result<AttendanceRecord> {
        val config = _globalConfig.value
        if (!config.isAttendanceServiceActive) {
            return Result.failure(Exception("उपस्थिति सेवा मास्टर एडमिन द्वारा वर्तमान में स्थगित है।"))
        }

        val branch = _branchSettings.value
        val schedules = _serviceSchedules.value
        val windowResult = AttendanceSecurityHelper.evaluateActiveServiceWindow(schedules)

        // Strict Time Window Check
        val activeService = windowResult.activeService ?: getDefaultSundayService()
        if (!windowResult.isWindowActive && !branch.isOfflineCheckInEnabled && verifiedByAdmin == null) {
            return Result.failure(Exception(windowResult.message))
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cleanSerial = AttendanceSecurityHelper.extractMemberSerialFromScan(memberSerial).ifBlank { "MEMBER" }
        val compositeRecordId = "${branch.branchId}_${activeService.serviceId}_${todayDate}_$cleanSerial"

        // Duplicate Check-in Prevention
        val existing = _todayAttendanceRecords.value.find { it.recordId == compositeRecordId || (it.memberSerial == cleanSerial && it.serviceDate == todayDate) }
        if (existing != null) {
            return Result.failure(Exception("उपस्थिति पहले ही दर्ज है: [ $cleanSerial ] ${existing.memberName} (${existing.serviceName})"))
        }

        // Multi-device anti-fraud check
        if (config.preventDeviceMultiCheckIn && checkInMethod != "usher_scan" && checkInMethod != "manual_usher_search") {
            val deviceCheckedIn = _todayAttendanceRecords.value.any { it.deviceId == deviceId && it.serviceDate == todayDate && it.memberSerial != cleanSerial }
            if (deviceCheckedIn) {
                return Result.failure(Exception("इस डिवाइस से पहले ही अन्य सदस्य की हाजिरी दर्ज की जा चुकी है।"))
            }
        }

        // Mode-Specific Validations
        var calculatedDistance: Double? = null
        when (checkInMethod) {
            "venue_qr_static" -> {
                // Geo-Fencing Perimeter Check
                if (config.enforceGeoFencingGlobally) {
                    val (isInside, distance) = AttendanceSecurityHelper.isWithinGeoFence(
                        userLat, userLng, branch.latitude, branch.longitude, branch.allowedRadiusMeters
                    )
                    calculatedDistance = distance
                    if (!isInside) {
                        return Result.failure(Exception("आप चर्च परिसर में उपस्थित नहीं हैं। (दूरी: ${distance.toInt()}m, अनुमत: ${branch.allowedRadiusMeters}m)"))
                    }
                }
                // Static Venue QR Verification
                if (scannedVenueQrPayload != null && !AttendanceSecurityHelper.verifyStaticWallQrPayload(scannedVenueQrPayload, branch.branchId, branch.secretHmacKey)) {
                    return Result.failure(Exception("अमान्य वेन्यू QR कोड! कृपया कलीसिया के आधिकारिक वॉल पोस्टर को स्कैन करें।"))
                }
            }
            "venue_qr_dynamic" -> {
                if (scannedVenueQrPayload != null && !AttendanceSecurityHelper.verifyDynamicRotatingQrPayload(
                        scannedVenueQrPayload, branch.branchId, branch.secretHmacKey, config.dynamicQrRotationSeconds
                    )) {
                    return Result.failure(Exception("यह QR कोड पुराना या एक्सपायर्ड हो चुका है। कृपया स्क्रीन पर नया कोड स्कैन करें।"))
                }
            }
            "usher_scan", "manual_usher_search" -> {
                // Verified by usher
            }
        }

        val isNetworkAvailable = isOnline()
        val now = System.currentTimeMillis()

        val record = AttendanceRecord(
            recordId = compositeRecordId,
            branchId = branch.branchId,
            churchPrefix = branch.churchPrefix,
            serviceId = activeService.serviceId,
            serviceName = activeService.serviceName,
            serviceDate = todayDate,
            memberId = cleanSerial,
            memberSerial = cleanSerial,
            memberName = memberName.ifBlank { "सदस्य $cleanSerial" },
            gender = gender,
            roleTier = roleTier,
            checkInMethod = checkInMethod,
            verifiedByUsherId = verifiedByAdmin?.id,
            verifiedByUsherName = verifiedByAdmin?.name,
            isOfflineSynced = isNetworkAvailable,
            offlineScannedAt = if (!isNetworkAvailable) now else null,
            deviceId = deviceId,
            checkInTimestamp = now,
            geoLatitude = userLat,
            geoLongitude = userLng,
            distanceFromVenueMeters = calculatedDistance,
            status = "CONFIRMED"
        )

        // Save Locally & Update In-Memory List Immediately
        val updatedToday = listOf(record) + _todayAttendanceRecords.value.filterNot { it.recordId == compositeRecordId }
        val updatedAll = listOf(record) + _allAttendanceRecords.value.filterNot { it.recordId == compositeRecordId }
        _todayAttendanceRecords.value = updatedToday
        _allAttendanceRecords.value = updatedAll
        saveAttendanceToCache(updatedAll)

        if (!isNetworkAvailable) {
            // Queue into offline sync queue
            val currentQueue = _offlineQueue.value + record
            _offlineQueue.value = currentQueue
            _offlineQueueCount.value = currentQueue.size
            _syncStatusText.value = "📡 ऑफलाइन: ${currentQueue.size} रिकॉर्ड्स सिंक हेतु लंबित"
            saveOfflineQueueToCache(currentQueue)
        } else {
            // Push directly to Firestore in background
            scope.launch {
                try {
                    val recordMap = recordToMap(record.copy(isOfflineSynced = true))
                    firestore.collection("attendance_records")
                        .document(compositeRecordId)
                        .set(recordMap, SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    // Fallback to queue if remote write fails
                    val currentQueue = _offlineQueue.value + record.copy(isOfflineSynced = false, offlineScannedAt = now)
                    _offlineQueue.value = currentQueue
                    _offlineQueueCount.value = currentQueue.size
                    _syncStatusText.value = "📡 ऑफलाइन: ${currentQueue.size} रिकॉर्ड्स सिंक हेतु लंबित"
                    saveOfflineQueueToCache(currentQueue)
                }
            }
        }

        // Re-compute absentee care
        refreshAbsenteeAnalytics()

        return Result.success(record)
    }

    // =========================================================================
    // 2. USER ATTENDANCE HISTORY (Member Dashboard)
    // =========================================================================

    fun getUserAttendanceHistory(memberSerial: String, memberId: String = ""): List<AttendanceRecord> {
        val cleanSerial = memberSerial.trim().uppercase()
        val all = _allAttendanceRecords.value
        return all.filter { rec ->
            (cleanSerial.isNotBlank() && rec.memberSerial.equals(cleanSerial, ignoreCase = true)) ||
            (memberId.isNotBlank() && rec.memberId.equals(memberId, ignoreCase = true))
        }.sortedByDescending { it.checkInTimestamp }
    }

    suspend fun refreshUserAttendanceHistoryFromFirestore(memberSerial: String, memberId: String = ""): List<AttendanceRecord> {
        val cleanSerial = memberSerial.trim().uppercase()
        if (cleanSerial.isBlank() && memberId.isBlank()) return getUserAttendanceHistory(memberSerial, memberId)

        return try {
            val query = if (cleanSerial.isNotBlank()) {
                firestore.collection("attendance_records").whereEqualTo("memberSerial", cleanSerial)
            } else {
                firestore.collection("attendance_records").whereEqualTo("memberId", memberId)
            }
            val snapshot = query.get().await()
            val remoteRecords = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { parseRecordFromMap(doc.id, it) }
            }

            if (remoteRecords.isNotEmpty()) {
                val merged = (remoteRecords + _allAttendanceRecords.value).distinctBy { it.recordId }
                _allAttendanceRecords.value = merged
                saveAttendanceToCache(merged)
            }
            getUserAttendanceHistory(cleanSerial, memberId)
        } catch (e: Exception) {
            getUserAttendanceHistory(cleanSerial, memberId)
        }
    }

    // =========================================================================
    // 3. 1-TAP FAMILY PASS (BATCH QR ATTENDANCE ENGINE)
    // =========================================================================

    fun getFamilyUnitByHeadSerialOrId(familyId: String, headSerial: String): FamilyUnit {
        val cleanSerial = AttendanceSecurityHelper.extractMemberSerialFromScan(headSerial)
        val existing = _familyUnits.value.find { 
            (familyId.isNotBlank() && it.familyId == familyId) ||
            (cleanSerial.isNotBlank() && (it.familyHeadSerial.equals(cleanSerial, ignoreCase = true) || it.memberUids.contains(cleanSerial)))
        }
        if (existing != null) return existing

        // Generate synthetic family unit based on head serial
        val branch = _branchSettings.value
        val defaultFamilyName = if (cleanSerial.isNotBlank()) "$cleanSerial परिवार (Family Pass)" else "कलीसिया परिवार"
        return FamilyUnit(
            familyId = if (familyId.isNotBlank()) familyId else "fam_${cleanSerial.ifBlank { "01" }}",
            familyHeadSerial = cleanSerial.ifBlank { "${branch.churchPrefix}01" },
            familyHeadUserId = "user_${cleanSerial.lowercase()}",
            branchId = branch.branchId,
            familyName = defaultFamilyName,
            members = listOf(
                FamilyMemberItem(
                    userId = "user_head",
                    memberSerial = cleanSerial.ifBlank { "${branch.churchPrefix}01" },
                    memberName = "$cleanSerial (परिवार मुखिया)",
                    relationship = "मुखिया / पिता",
                    gender = "Male",
                    roleTier = "believer",
                    isSelected = true
                ),
                FamilyMemberItem(
                    userId = "user_spouse",
                    memberSerial = incrementSerial(cleanSerial, 1),
                    memberName = "श्रीमती ${cleanSerial} (पत्नी)",
                    relationship = "माता / पत्नी",
                    gender = "Female",
                    roleTier = "believer",
                    isSelected = true
                ),
                FamilyMemberItem(
                    userId = "user_child1",
                    memberSerial = incrementSerial(cleanSerial, 2),
                    memberName = "टिमोथी (बेटा)",
                    relationship = "बेटा / संतान",
                    gender = "Child",
                    roleTier = "believer",
                    isSelected = true
                )
            )
        )
    }

    /**
     * Dynamically rebuilds FamilyUnits from live UserProfileData and auto-invalidates stale family cache.
     * Members who married out or transferred to new families immediately appear under the correct Family Pass QR.
     */
    fun refreshFamilyUnitsFromProfiles(profiles: List<UserProfileData>) {
        if (profiles.isEmpty()) return
        val activeProfiles = profiles.filter {
            it.membershipStatus == "active" || it.membershipStatus.isBlank()
        }

        val grouped = activeProfiles
            .filter { it.familyId.isNotBlank() }
            .groupBy { it.familyId }

        val dynamicUnits = mutableListOf<FamilyUnit>()
        for ((fId, membersList) in grouped) {
            val head = membersList.find { it.isFamilyHead || it.familyRole == "head" } ?: membersList.first()
            val branch = _branchSettings.value
            dynamicUnits.add(
                FamilyUnit(
                    familyId = fId,
                    familyHeadUserId = head.userId,
                    familyHeadSerial = head.serialNumber,
                    branchId = head.homeBranchId.ifBlank { branch.branchId },
                    familyName = "${head.fullName.ifBlank { head.displayName.ifBlank { head.serialNumber } }} परिवार (Family Pass)",
                    memberUids = membersList.map { it.userId },
                    members = membersList.map { m ->
                        val relationLabel = when (m.familyRole) {
                            "head" -> "मुखिया / पिता"
                            "spouse" -> "माता / पत्नी"
                            else -> if (m.gender.equals("Female", ignoreCase = true)) "पुत्री / सदस्य" else "पुत्र / सदस्य"
                        }
                        FamilyMemberItem(
                            userId = m.userId,
                            memberSerial = m.serialNumber,
                            memberName = m.fullName.ifBlank { m.displayName.ifBlank { m.serialNumber } },
                            relationship = relationLabel,
                            gender = m.gender,
                            roleTier = m.roleTier,
                            isSelected = true
                        )
                    }
                )
            )
        }

        if (dynamicUnits.isNotEmpty()) {
            _familyUnits.value = dynamicUnits
        }
    }

    private fun incrementSerial(baseSerial: String, increment: Int): String {
        val regex = Regex("""^([A-Za-z]+)(\d+)$""")
        val match = regex.find(baseSerial) ?: return "${baseSerial}_$increment"
        val prefix = match.groupValues[1]
        val num = match.groupValues[2].toIntOrNull() ?: 1
        return "$prefix${num + increment}"
    }

    suspend fun processFamilyBatchCheckIn(
        familyUnit: FamilyUnit,
        selectedMembers: List<FamilyMemberItem>,
        verifiedByAdmin: AdminUser? = null,
        deviceId: String = UserDeviceHelper.getDeviceId(context)
    ): Result<List<AttendanceRecord>> {
        if (selectedMembers.isEmpty()) {
            return Result.failure(Exception("कृपया कम से कम एक पारिवारिक सदस्य चुनें।"))
        }

        val checkedRecords = mutableListOf<AttendanceRecord>()
        val failures = mutableListOf<String>()

        for (member in selectedMembers) {
            val res = processCheckIn(
                memberSerial = member.memberSerial,
                memberName = member.memberName,
                gender = member.gender,
                roleTier = member.roleTier,
                checkInMethod = "family_pass_batch",
                verifiedByAdmin = verifiedByAdmin,
                deviceId = deviceId
            )
            res.onSuccess { rec ->
                checkedRecords.add(rec)
            }.onFailure { err ->
                failures.add("${member.memberName}: ${err.localizedMessage}")
            }
        }

        return if (checkedRecords.isNotEmpty()) {
            Result.success(checkedRecords)
        } else {
            Result.failure(Exception(failures.firstOrNull() ?: "पारिवारिक उपस्थिति दर्ज करने में त्रुटि हुई"))
        }
    }

    // =========================================================================
    // 4. PASTORAL CARE MILESTONE TRIGGERS (BIRTHDAY & ANNIVERSARY TELEMETRY)
    // =========================================================================

    fun refreshTodayMilestones(membersList: List<UserProfileData>): List<PastoralMilestoneAlert> {
        val alerts = mutableListOf<PastoralMilestoneAlert>()
        val cal = Calendar.getInstance()
        val source = if (membersList.isNotEmpty()) membersList else getDefaultDemoMembers()

        for (m in source) {
            val name = m.fullName.ifBlank { m.displayName.ifBlank { "सदस्य" } }
            val serial = m.serialNumber.ifBlank { "NCC" }
            val memberAlerts = AttendanceSecurityHelper.evaluatePastoralMilestones(
                dobStr = m.dateOfBirth,
                anniversaryStr = m.anniversaryDate,
                memberName = name,
                memberSerial = serial,
                memberId = m.userId,
                todayCalendar = cal
            )
            alerts.addAll(memberAlerts)
        }

        // Add demo milestones if empty for realistic ecclesiastical dashboard
        if (alerts.isEmpty()) {
            alerts.add(
                PastoralMilestoneAlert(
                    memberId = "demo_1",
                    memberSerial = "NCC12",
                    memberName = "जॉन डेविड",
                    type = "BIRTHDAY",
                    title = "आज जन्मदिन है! 🎂",
                    dateStr = "1994-09-27",
                    isToday = true,
                    dayOffset = 0,
                    formattedMessage = "🎂 आज जॉन डेविड (NCC12) का जन्मदिन है!"
                )
            )
            alerts.add(
                PastoralMilestoneAlert(
                    memberId = "demo_2",
                    memberSerial = "NCC02",
                    memberName = "पास्टर सैमुअल व पत्नी",
                    type = "ANNIVERSARY",
                    title = "विवाह वर्षगांठ! 💍",
                    dateStr = "2015-09-28",
                    isToday = false,
                    dayOffset = -1,
                    formattedMessage = "💍 कल पास्टर सैमुअल व पत्नी (NCC02) की 11वीं विवाह वर्षगांठ है!"
                )
            )
        }

        _todayMilestoneAlerts.value = alerts
        return alerts
    }

    // =========================================================================
    // 5. PHYSICAL LAMINATED ID CARDS BATCH PDF EXPORTER
    // =========================================================================

    fun exportPhysicalBadgesPdf(membersList: List<UserProfileData>): Uri? {
        val branch = _branchSettings.value
        val source = if (membersList.isNotEmpty()) membersList else getDefaultDemoMembers()
        return com.example.util.BadgesPdfGeneratorService.generateBadgesPdf(
            context = context,
            members = source,
            branchName = branch.branchName,
            churchPrefix = branch.churchPrefix
        )
    }

    // =========================================================================
    // 6. MANUAL DIRECTORY SEARCH & EXCEPTION CHECK-IN FALLBACK
    // =========================================================================

    fun searchChurchDirectory(
        query: String,
        membersList: List<UserProfileData>
    ): List<UserProfileData> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val source = if (membersList.isNotEmpty()) membersList else getDefaultDemoMembers()

        return source.filter { member ->
            val name = (member.displayName + " " + member.fullName).lowercase()
            val serial = member.serialNumber.lowercase()
            val phone = (member.phoneNumber + " " + member.phone).filter { it.isDigit() }
            val cleanQ = q.filter { it.isDigit() }

            name.contains(q) ||
            serial.contains(q) ||
            (cleanQ.isNotBlank() && phone.contains(cleanQ))
        }.take(10)
    }

    // =========================================================================
    // 3. OFFLINE-FIRST BATCH SYNC ENGINE (AttendanceSyncService)
    // =========================================================================

    fun forceSyncOfflineQueue(onComplete: ((Boolean, Int) -> Unit)? = null) {
        scope.launch {
            syncOfflineQueueInternal(onComplete)
        }
    }

    private suspend fun syncOfflineQueueInternal(onComplete: ((Boolean, Int) -> Unit)? = null) {
        val queue = _offlineQueue.value
        if (queue.isEmpty()) {
            _syncStatusText.value = "✓ सिंक पूर्ण"
            onComplete?.invoke(true, 0)
            return
        }

        if (!isOnline()) {
            _syncStatusText.value = "📡 ऑफलाइन: ${queue.size} रिकॉर्ड्स सिंक हेतु लंबित"
            onComplete?.invoke(false, 0)
            return
        }

        _isSyncing.value = true
        _syncStatusText.value = "⏳ बैकग्राउंड सिंक प्रगति पर है..."

        try {
            // Write in batches of up to 500
            val chunks = queue.chunked(500)
            var totalSynced = 0

            for (chunk in chunks) {
                val batch: WriteBatch = firestore.batch()
                chunk.forEach { record ->
                    val docRef = firestore.collection("attendance_records").document(record.recordId)
                    val map = recordToMap(record.copy(isOfflineSynced = true))
                    batch.set(docRef, map, SetOptions.merge())
                }
                batch.commit().await()
                totalSynced += chunk.size
            }

            // Clear offline queue
            _offlineQueue.value = emptyList()
            _offlineQueueCount.value = 0
            _syncStatusText.value = "✓ सिंक पूर्ण ($totalSynced रिकॉर्ड्स अपलोड)"
            saveOfflineQueueToCache(emptyList())

            onComplete?.invoke(true, totalSynced)
        } catch (e: Exception) {
            _syncStatusText.value = "⚠️ सिंक पुनः प्रयास में (${queue.size} लंबित)"
            onComplete?.invoke(false, 0)
        } finally {
            _isSyncing.value = false
        }
    }

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    scope.launch {
                        if (_offlineQueue.value.isNotEmpty()) {
                            syncOfflineQueueInternal()
                        }
                    }
                }
            })
        } catch (_: Exception) {}
    }

    private fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // 4. CONFIGURATION & AUTONOMY MANAGEMENT
    // =========================================================================

    suspend fun updateGlobalConfig(config: AttendanceGlobalConfig): Result<Unit> {
        return try {
            _globalConfig.value = config
            prefs.edit().putString("global_config_json", globalConfigToJson(config).toString()).apply()
            firestore.collection("app_settings")
                .document("attendance_global_config")
                .set(globalConfigToMap(config), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBranchSettings(settings: BranchAttendanceSettings): Result<Unit> {
        return try {
            _branchSettings.value = settings
            prefs.edit().putString("branch_settings_json", branchSettingsToJson(settings).toString()).apply()
            firestore.collection("church_branches")
                .document(settings.branchId)
                .collection("attendance_settings")
                .document("config")
                .set(branchSettingsToMap(settings), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Instantly re-binds active branch attendance engine, gate scanner, wall QR, and session timers.
     */
    fun switchActiveBranch(newBranchId: String, newBranchName: String = "") {
        if (_branchSettings.value.branchId == newBranchId) return
        val current = _branchSettings.value
        val updated = current.copy(
            branchId = newBranchId,
            branchName = newBranchName.ifBlank {
                when (newBranchId) {
                    "branch_ncc_khwr" -> "न्यू क्रिएशन कलीसिया (कुम्हारी शाखा)"
                    "branch_ncc_rpr" -> "न्यू क्रिएशन कलीसिया (रायपुर शाखा)"
                    "branch_ncc_drg" -> "न्यू क्रिएशन कलीसिया (दुर्ग प्रार्थना भवन)"
                    else -> "न्यू क्रिएशन चर्च (मुख्य कलीसिया)"
                }
            }
        )
        _branchSettings.value = updated
        prefs.edit().putString("branch_settings_json", branchSettingsToJson(updated).toString()).apply()

        // Re-attach schedules listener for newly selected branch
        try {
            schedulesListener?.remove()
            schedulesListener = firestore.collection("church_branches")
                .document(newBranchId)
                .collection("service_schedules")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseScheduleFromMap(doc.id, it) }
                        }
                        if (list.isNotEmpty()) {
                            _serviceSchedules.value = list
                            saveSchedulesToCache(list)
                            refreshServiceWindow()
                        }
                    }
                }
        } catch (_: Exception) {}

        refreshServiceWindow()
    }

    suspend fun saveServiceSchedule(schedule: ServiceSchedule): Result<Unit> {
        return try {
            val current = _serviceSchedules.value.toMutableList()
            val index = current.indexOfFirst { it.serviceId == schedule.serviceId }
            if (index >= 0) {
                current[index] = schedule
            } else {
                current.add(schedule)
            }
            _serviceSchedules.value = current
            saveSchedulesToCache(current)
            refreshServiceWindow()

            firestore.collection("church_branches")
                .document(_branchSettings.value.branchId)
                .collection("service_schedules")
                .document(schedule.serviceId)
                .set(scheduleToMap(schedule), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteServiceSchedule(serviceId: String): Result<Unit> {
        return try {
            val current = _serviceSchedules.value.filterNot { it.serviceId == serviceId }
            _serviceSchedules.value = current
            saveSchedulesToCache(current)
            refreshServiceWindow()

            firestore.collection("church_branches")
                .document(_branchSettings.value.branchId)
                .collection("service_schedules")
                .document(serviceId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // 5. ABSENTEE CARE ANALYTICS (सुधि ट्रैकर)
    // =========================================================================

    fun refreshAbsenteeAnalytics(registeredMembers: List<UserProfileData> = emptyList()) {
        scope.launch {
            val allRecords = _allAttendanceRecords.value
            val memberAttendanceMap = allRecords.groupBy { it.memberSerial }
            val flaggedList = mutableListOf<AbsenteeCareMember>()
            val members = if (registeredMembers.isNotEmpty()) registeredMembers else getDefaultDemoMembers()
            val currentBranchId = _branchSettings.value.branchId

            // Exclude relocated/married out members, and only include members belonging to current active branch
            val eligibleMembers = members.filter { m ->
                val isActiveMember = m.membershipStatus == "active" || m.membershipStatus.isBlank()
                val belongsToBranch = m.homeBranchId == currentBranchId || (m.homeBranchId.isBlank() && currentBranchId == "branch_ncc_01")
                isActiveMember && belongsToBranch
            }

            for (m in eligibleMembers) {
                val memberSerial = m.serialNumber.ifBlank { m.userId }
                val records = memberAttendanceMap[memberSerial] ?: emptyList()
                val sortedRecords = records.sortedByDescending { it.serviceDate }
                val lastRecord = sortedRecords.firstOrNull()

                val lastAttendedDate = lastRecord?.serviceDate ?: "उपस्थिति अनुपलब्ध"
                val consecutiveAbsenceCount = calculateConsecutiveAbsenceWeeks(sortedRecords)

                if (consecutiveAbsenceCount >= 2) {
                    val severity = if (consecutiveAbsenceCount >= 3) "CRITICAL" else "FOLLOW_UP"
                    flaggedList.add(
                        AbsenteeCareMember(
                            memberId = m.userId,
                            memberSerial = memberSerial,
                            memberName = m.displayName.ifBlank { m.fullName.ifBlank { "सदस्य $memberSerial" } },
                            phone = m.phoneNumber.ifBlank { m.phone },
                            roleTier = m.roleTier,
                            churchPrefix = _branchSettings.value.churchPrefix,
                            lastAttendedDate = lastAttendedDate,
                            consecutiveAbsenceCount = consecutiveAbsenceCount,
                            severityLevel = severity,
                            followUpNotes = "",
                            isCareContacted = false
                        )
                    )
                }
            }

            _absenteeCareList.value = flaggedList.sortedByDescending { it.consecutiveAbsenceCount }
        }
    }

    private fun calculateConsecutiveAbsenceWeeks(records: List<AttendanceRecord>): Int {
        if (records.isEmpty()) return 3 // New / Never attended
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val latest = records.firstOrNull() ?: return 3
        return try {
            val lastDate = sdf.parse(latest.serviceDate) ?: return 2
            val diffDays = ((System.currentTimeMillis() - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
            (diffDays / 7).coerceAtLeast(0)
        } catch (e: Exception) {
            2
        }
    }

    // =========================================================================
    // 6. CSV & REGISTER EXPORT ENGINE
    // =========================================================================

    fun exportAttendanceRegisterToCsv(filterDate: String? = null): Uri? {
        return try {
            val records = if (filterDate.isNullOrBlank()) _allAttendanceRecords.value else _allAttendanceRecords.value.filter { it.serviceDate == filterDate }
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileName = "Attendance_Register_${System.currentTimeMillis()}.csv"
            val file = File(exportDir, fileName)

            val writer = FileWriter(file)
            writer.append("दिनांक (Date),सभा का नाम (Service),सीरियल नंबर (Serial),सदस्य का नाम (Name),लिंग (Gender),पदनाम (Role),चेक-इन मोड (Method),सत्यापित कर्ता (Usher),समय (Timestamp),सिंक स्थिति (Sync Status),दूरी (Distance Meters)\n")

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            records.forEach { r ->
                val timeStr = timeFormat.format(Date(r.checkInTimestamp))
                val dist = r.distanceFromVenueMeters?.toInt()?.toString() ?: "N/A"
                val usher = r.verifiedByUsherName ?: "स्वयं (Self)"
                val syncStatus = if (r.isOfflineSynced) "ऑनलाइन सिंक" else "ऑफलाइन कैश"
                writer.append("\"${r.serviceDate}\",\"${r.serviceName}\",\"${r.memberSerial}\",\"${r.memberName}\",\"${r.gender}\",\"${r.roleTier}\",\"${r.checkInMethod}\",\"$usher\",\"$timeStr\",\"$syncStatus\",\"$dist\"\n")
            }
            writer.flush()
            writer.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // INTERNAL SYNC & CACHE HELPERS
    // =========================================================================

    private fun startFirestoreSync() {
        // Sync Global Config
        try {
            settingsListener = firestore.collection("app_settings")
                .document("attendance_global_config")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val map = snapshot.data ?: emptyMap()
                        val cfg = parseGlobalConfigFromMap(map)
                        _globalConfig.value = cfg
                        prefs.edit().putString("global_config_json", globalConfigToJson(cfg).toString()).apply()
                    }
                }
        } catch (_: Exception) {}

        // Sync Service Schedules
        try {
            schedulesListener = firestore.collection("church_branches")
                .document(_branchSettings.value.branchId)
                .collection("service_schedules")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseScheduleFromMap(doc.id, it) }
                        }
                        if (list.isNotEmpty()) {
                            _serviceSchedules.value = list
                            saveSchedulesToCache(list)
                            refreshServiceWindow()
                        }
                    }
                }
        } catch (_: Exception) {}

        // Sync Attendance Records for Today
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        try {
            attendanceListener = firestore.collection("attendance_records")
                .whereEqualTo("serviceDate", todayStr)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { parseRecordFromMap(doc.id, it) }
                        }
                        _todayAttendanceRecords.value = list
                        val merged = (list + _allAttendanceRecords.value).distinctBy { it.recordId }
                        _allAttendanceRecords.value = merged
                        saveAttendanceToCache(merged)
                    }
                }
        } catch (_: Exception) {}
    }

    private fun loadCachedData() {
        // Load Global Config
        val globalJson = prefs.getString("global_config_json", null)
        if (globalJson != null) {
            try { _globalConfig.value = parseGlobalConfigFromJson(JSONObject(globalJson)) } catch (_: Exception) {}
        }

        // Load Branch Settings
        val branchJson = prefs.getString("branch_settings_json", null)
        if (branchJson != null) {
            try { _branchSettings.value = parseBranchSettingsFromJson(JSONObject(branchJson)) } catch (_: Exception) {}
        }

        // Load Service Schedules
        val schedulesJson = prefs.getString("schedules_cache_json", null)
        if (schedulesJson != null) {
            try {
                val array = JSONArray(schedulesJson)
                val list = (0 until array.length()).map { parseScheduleFromJson(array.getJSONObject(it)) }
                _serviceSchedules.value = list
            } catch (_: Exception) {}
        } else {
            _serviceSchedules.value = getDefaultSchedules()
            saveSchedulesToCache(_serviceSchedules.value)
        }

        // Load Offline Queue
        val queueJson = prefs.getString("offline_queue_json", null)
        if (queueJson != null) {
            try {
                val array = JSONArray(queueJson)
                val list = (0 until array.length()).map { parseRecordFromJson(array.getJSONObject(it)) }
                _offlineQueue.value = list
                _offlineQueueCount.value = list.size
                if (list.isNotEmpty()) {
                    _syncStatusText.value = "📡 ऑफलाइन: ${list.size} रिकॉर्ड्स सिंक हेतु लंबित"
                }
            } catch (_: Exception) {}
        }

        // Load Attendance Records
        val attendanceJson = prefs.getString("attendance_records_cache_json", null)
        if (attendanceJson != null) {
            try {
                val array = JSONArray(attendanceJson)
                val list = (0 until array.length()).map { parseRecordFromJson(array.getJSONObject(it)) }
                _allAttendanceRecords.value = list
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                _todayAttendanceRecords.value = list.filter { it.serviceDate == todayStr }
            } catch (_: Exception) {}
        } else {
            val sample = getSampleAttendanceRecords()
            _allAttendanceRecords.value = sample
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            _todayAttendanceRecords.value = sample.filter { it.serviceDate == todayStr }
            saveAttendanceToCache(sample)
        }

        refreshAbsenteeAnalytics()
    }

    private fun saveAttendanceToCache(list: List<AttendanceRecord>) {
        val array = JSONArray()
        list.take(200).forEach { array.put(recordToJson(it)) }
        prefs.edit().putString("attendance_records_cache_json", array.toString()).apply()
    }

    private fun saveOfflineQueueToCache(list: List<AttendanceRecord>) {
        val array = JSONArray()
        list.forEach { array.put(recordToJson(it)) }
        prefs.edit().putString("offline_queue_json", array.toString()).apply()
    }

    private fun saveSchedulesToCache(list: List<ServiceSchedule>) {
        val array = JSONArray()
        list.forEach { array.put(scheduleToJson(it)) }
        prefs.edit().putString("schedules_cache_json", array.toString()).apply()
    }

    private fun getDefaultSundayService(): ServiceSchedule {
        return ServiceSchedule(
            serviceId = "sun_morning_main",
            serviceName = "रविवार सुबह की मुख्य आराधना",
            dayOfWeek = 0,
            serviceStartTime = "09:00",
            serviceEndTime = "11:30",
            checkInWindowOpenMinutesBefore = 30,
            checkInWindowCloseMinutesAfter = 60,
            isActive = true
        )
    }

    private fun getDefaultSchedules(): List<ServiceSchedule> {
        return listOf(
            ServiceSchedule(
                serviceId = "sun_morning_main",
                serviceName = "रविवार सुबह की मुख्य आराधना",
                dayOfWeek = 0,
                serviceStartTime = "09:00",
                serviceEndTime = "11:30",
                checkInWindowOpenMinutesBefore = 30,
                checkInWindowCloseMinutesAfter = 60,
                isActive = true
            ),
            ServiceSchedule(
                serviceId = "fri_youth_fellowship",
                serviceName = "शुक्रवार युवा संगति व बाइबल अध्ययन",
                dayOfWeek = 5,
                serviceStartTime = "18:30",
                serviceEndTime = "20:00",
                checkInWindowOpenMinutesBefore = 15,
                checkInWindowCloseMinutesAfter = 45,
                isActive = true
            ),
            ServiceSchedule(
                serviceId = "wed_fasting_prayer",
                serviceName = "बुधवार उपवास प्रार्थना",
                dayOfWeek = 3,
                serviceStartTime = "11:00",
                serviceEndTime = "13:00",
                checkInWindowOpenMinutesBefore = 15,
                checkInWindowCloseMinutesAfter = 30,
                isActive = true
            )
        )
    }

    fun getDefaultDemoMembers(): List<UserProfileData> {
        return listOf(
            UserProfileData(userId = "u1", displayName = "जॉन डेविड", serialNumber = "NCC01", phoneNumber = "9826100001", roleTier = "believer", gender = "Male"),
            UserProfileData(userId = "u2", displayName = "सुनील शर्मा", serialNumber = "NCC02", phoneNumber = "9826100002", roleTier = "believer", gender = "Male"),
            UserProfileData(userId = "u3", displayName = "प्रिया कुजूर", serialNumber = "NCC03", phoneNumber = "9826100003", roleTier = "believer", gender = "Female"),
            UserProfileData(userId = "u4", displayName = "राजेश टोप्पो", serialNumber = "NCC04", phoneNumber = "9826100004", roleTier = "believer", gender = "Male"),
            UserProfileData(userId = "u5", displayName = "अनीता मसीह", serialNumber = "NCC05", phoneNumber = "9826100005", roleTier = "believer", gender = "Female"),
            UserProfileData(userId = "u6", displayName = "सैमसन केरकेट्टा", serialNumber = "NCC06", phoneNumber = "9826100006", roleTier = "believer", gender = "Male"),
            UserProfileData(userId = "u7", displayName = "एस्तेर तिग्गा", serialNumber = "NCC07", phoneNumber = "9826100007", roleTier = "believer", gender = "Female")
        )
    }

    private fun getSampleAttendanceRecords(): List<AttendanceRecord> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return listOf(
            AttendanceRecord(
                recordId = "branch_01_sun_morning_main_${todayStr}_NCC01",
                memberSerial = "NCC01",
                memberName = "जॉन डेविड",
                gender = "Male",
                roleTier = "believer",
                checkInMethod = "usher_scan",
                verifiedByUsherName = "पास्टर थॉमस",
                isOfflineSynced = true,
                serviceDate = todayStr,
                checkInTimestamp = System.currentTimeMillis() - 1000 * 60 * 20
            ),
            AttendanceRecord(
                recordId = "branch_01_sun_morning_main_${todayStr}_NCC03",
                memberSerial = "NCC03",
                memberName = "प्रिया कुजूर",
                gender = "Female",
                roleTier = "believer",
                checkInMethod = "venue_qr_dynamic",
                isOfflineSynced = true,
                serviceDate = todayStr,
                checkInTimestamp = System.currentTimeMillis() - 1000 * 60 * 12
            )
        )
    }

    // JSON / Map Converters
    private fun recordToMap(r: AttendanceRecord): Map<String, Any?> = mapOf(
        "recordId" to r.recordId,
        "branchId" to r.branchId,
        "churchPrefix" to r.churchPrefix,
        "serviceId" to r.serviceId,
        "serviceName" to r.serviceName,
        "serviceDate" to r.serviceDate,
        "memberId" to r.memberId,
        "memberSerial" to r.memberSerial,
        "memberName" to r.memberName,
        "gender" to r.gender,
        "roleTier" to r.roleTier,
        "checkInMethod" to r.checkInMethod,
        "verifiedByUsherId" to r.verifiedByUsherId,
        "verifiedByUsherName" to r.verifiedByUsherName,
        "isOfflineSynced" to r.isOfflineSynced,
        "offlineScannedAt" to r.offlineScannedAt,
        "deviceId" to r.deviceId,
        "checkInTimestamp" to r.checkInTimestamp,
        "geoLatitude" to r.geoLatitude,
        "geoLongitude" to r.geoLongitude,
        "distanceFromVenueMeters" to r.distanceFromVenueMeters,
        "status" to r.status
    )

    private fun parseRecordFromMap(id: String, map: Map<String, Any>): AttendanceRecord = AttendanceRecord(
        recordId = (map["recordId"] as? String) ?: id,
        branchId = (map["branchId"] as? String) ?: "branch_ncc_01",
        churchPrefix = (map["churchPrefix"] as? String) ?: "NCC",
        serviceId = (map["serviceId"] as? String) ?: "sun_morning_main",
        serviceName = (map["serviceName"] as? String) ?: "रविवार मुख्य आराधना",
        serviceDate = (map["serviceDate"] as? String) ?: "",
        memberId = (map["memberId"] as? String) ?: "",
        memberSerial = (map["memberSerial"] as? String) ?: "",
        memberName = (map["memberName"] as? String) ?: "",
        gender = (map["gender"] as? String) ?: "Male",
        roleTier = (map["roleTier"] as? String) ?: "believer",
        checkInMethod = (map["checkInMethod"] as? String) ?: "usher_scan",
        verifiedByUsherId = map["verifiedByUsherId"] as? String,
        verifiedByUsherName = map["verifiedByUsherName"] as? String,
        isOfflineSynced = (map["isOfflineSynced"] as? Boolean) ?: true,
        offlineScannedAt = (map["offlineScannedAt"] as? Number)?.toLong(),
        deviceId = (map["deviceId"] as? String) ?: "",
        checkInTimestamp = (map["checkInTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        geoLatitude = (map["geoLatitude"] as? Number)?.toDouble(),
        geoLongitude = (map["geoLongitude"] as? Number)?.toDouble(),
        distanceFromVenueMeters = (map["distanceFromVenueMeters"] as? Number)?.toDouble(),
        status = (map["status"] as? String) ?: "CONFIRMED"
    )

    private fun recordToJson(r: AttendanceRecord): JSONObject = JSONObject().apply {
        put("recordId", r.recordId)
        put("branchId", r.branchId)
        put("churchPrefix", r.churchPrefix)
        put("serviceId", r.serviceId)
        put("serviceName", r.serviceName)
        put("serviceDate", r.serviceDate)
        put("memberId", r.memberId)
        put("memberSerial", r.memberSerial)
        put("memberName", r.memberName)
        put("gender", r.gender)
        put("roleTier", r.roleTier)
        put("checkInMethod", r.checkInMethod)
        put("verifiedByUsherId", r.verifiedByUsherId)
        put("verifiedByUsherName", r.verifiedByUsherName)
        put("isOfflineSynced", r.isOfflineSynced)
        put("offlineScannedAt", r.offlineScannedAt)
        put("deviceId", r.deviceId)
        put("checkInTimestamp", r.checkInTimestamp)
        put("geoLatitude", r.geoLatitude)
        put("geoLongitude", r.geoLongitude)
        put("distanceFromVenueMeters", r.distanceFromVenueMeters)
        put("status", r.status)
    }

    private fun parseRecordFromJson(json: JSONObject): AttendanceRecord = AttendanceRecord(
        recordId = json.optString("recordId"),
        branchId = json.optString("branchId"),
        churchPrefix = json.optString("churchPrefix"),
        serviceId = json.optString("serviceId"),
        serviceName = json.optString("serviceName"),
        serviceDate = json.optString("serviceDate"),
        memberId = json.optString("memberId"),
        memberSerial = json.optString("memberSerial"),
        memberName = json.optString("memberName"),
        gender = json.optString("gender", "Male"),
        roleTier = json.optString("roleTier", "believer"),
        checkInMethod = json.optString("checkInMethod", "usher_scan"),
        verifiedByUsherId = json.optString("verifiedByUsherId").takeIf { it.isNotBlank() },
        verifiedByUsherName = json.optString("verifiedByUsherName").takeIf { it.isNotBlank() },
        isOfflineSynced = json.optBoolean("isOfflineSynced", true),
        offlineScannedAt = if (json.has("offlineScannedAt") && !json.isNull("offlineScannedAt")) json.optLong("offlineScannedAt") else null,
        deviceId = json.optString("deviceId"),
        checkInTimestamp = json.optLong("checkInTimestamp", System.currentTimeMillis()),
        geoLatitude = if (json.has("geoLatitude") && !json.isNull("geoLatitude")) json.optDouble("geoLatitude") else null,
        geoLongitude = if (json.has("geoLongitude") && !json.isNull("geoLongitude")) json.optDouble("geoLongitude") else null,
        distanceFromVenueMeters = if (json.has("distanceFromVenueMeters") && !json.isNull("distanceFromVenueMeters")) json.optDouble("distanceFromVenueMeters") else null,
        status = json.optString("status", "CONFIRMED")
    )

    private fun scheduleToMap(s: ServiceSchedule): Map<String, Any> = mapOf(
        "serviceId" to s.serviceId,
        "serviceName" to s.serviceName,
        "dayOfWeek" to s.dayOfWeek,
        "serviceStartTime" to s.serviceStartTime,
        "serviceEndTime" to s.serviceEndTime,
        "checkInWindowOpenMinutesBefore" to s.checkInWindowOpenMinutesBefore,
        "checkInWindowCloseMinutesAfter" to s.checkInWindowCloseMinutesAfter,
        "isActive" to s.isActive
    )

    private fun parseScheduleFromMap(id: String, map: Map<String, Any>): ServiceSchedule = ServiceSchedule(
        serviceId = (map["serviceId"] as? String) ?: id,
        serviceName = (map["serviceName"] as? String) ?: "आराधना सभा",
        dayOfWeek = (map["dayOfWeek"] as? Number)?.toInt() ?: 0,
        serviceStartTime = (map["serviceStartTime"] as? String) ?: "09:00",
        serviceEndTime = (map["serviceEndTime"] as? String) ?: "11:30",
        checkInWindowOpenMinutesBefore = (map["checkInWindowOpenMinutesBefore"] as? Number)?.toInt() ?: 30,
        checkInWindowCloseMinutesAfter = (map["checkInWindowCloseMinutesAfter"] as? Number)?.toInt() ?: 60,
        isActive = (map["isActive"] as? Boolean) ?: true
    )

    private fun scheduleToJson(s: ServiceSchedule): JSONObject = JSONObject().apply {
        put("serviceId", s.serviceId)
        put("serviceName", s.serviceName)
        put("dayOfWeek", s.dayOfWeek)
        put("serviceStartTime", s.serviceStartTime)
        put("serviceEndTime", s.serviceEndTime)
        put("checkInWindowOpenMinutesBefore", s.checkInWindowOpenMinutesBefore)
        put("checkInWindowCloseMinutesAfter", s.checkInWindowCloseMinutesAfter)
        put("isActive", s.isActive)
    }

    private fun parseScheduleFromJson(json: JSONObject): ServiceSchedule = ServiceSchedule(
        serviceId = json.optString("serviceId"),
        serviceName = json.optString("serviceName"),
        dayOfWeek = json.optInt("dayOfWeek", 0),
        serviceStartTime = json.optString("serviceStartTime", "09:00"),
        serviceEndTime = json.optString("serviceEndTime", "11:30"),
        checkInWindowOpenMinutesBefore = json.optInt("checkInWindowOpenMinutesBefore", 30),
        checkInWindowCloseMinutesAfter = json.optInt("checkInWindowCloseMinutesAfter", 60),
        isActive = json.optBoolean("isActive", true)
    )

    private fun globalConfigToMap(c: AttendanceGlobalConfig): Map<String, Any> = mapOf(
        "isAttendanceServiceActive" to c.isAttendanceServiceActive,
        "permittedAttendanceModes" to c.permittedAttendanceModes,
        "enforceGeoFencingGlobally" to c.enforceGeoFencingGlobally,
        "preventDeviceMultiCheckIn" to c.preventDeviceMultiCheckIn,
        "maxAllowedRadiusMetersLimit" to c.maxAllowedRadiusMetersLimit,
        "dynamicQrRotationSeconds" to c.dynamicQrRotationSeconds,
        "updatedAt" to c.updatedAt
    )

    private fun parseGlobalConfigFromMap(map: Map<String, Any>): AttendanceGlobalConfig = AttendanceGlobalConfig(
        isAttendanceServiceActive = (map["isAttendanceServiceActive"] as? Boolean) ?: true,
        permittedAttendanceModes = (map["permittedAttendanceModes"] as? List<*>)?.mapNotNull { it?.toString() } ?: listOf("usher_scans_member", "member_scans_venue_qr", "dual_mode"),
        enforceGeoFencingGlobally = (map["enforceGeoFencingGlobally"] as? Boolean) ?: true,
        preventDeviceMultiCheckIn = (map["preventDeviceMultiCheckIn"] as? Boolean) ?: true,
        maxAllowedRadiusMetersLimit = (map["maxAllowedRadiusMetersLimit"] as? Number)?.toInt() ?: 200,
        dynamicQrRotationSeconds = (map["dynamicQrRotationSeconds"] as? Number)?.toInt() ?: 30,
        updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    private fun globalConfigToJson(c: AttendanceGlobalConfig): JSONObject = JSONObject().apply {
        put("isAttendanceServiceActive", c.isAttendanceServiceActive)
        put("permittedAttendanceModes", JSONArray(c.permittedAttendanceModes))
        put("enforceGeoFencingGlobally", c.enforceGeoFencingGlobally)
        put("preventDeviceMultiCheckIn", c.preventDeviceMultiCheckIn)
        put("maxAllowedRadiusMetersLimit", c.maxAllowedRadiusMetersLimit)
        put("dynamicQrRotationSeconds", c.dynamicQrRotationSeconds)
        put("updatedAt", c.updatedAt)
    }

    private fun parseGlobalConfigFromJson(json: JSONObject): AttendanceGlobalConfig = AttendanceGlobalConfig(
        isAttendanceServiceActive = json.optBoolean("isAttendanceServiceActive", true),
        permittedAttendanceModes = json.optJSONArray("permittedAttendanceModes")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: listOf("usher_scans_member", "member_scans_venue_qr", "dual_mode"),
        enforceGeoFencingGlobally = json.optBoolean("enforceGeoFencingGlobally", true),
        preventDeviceMultiCheckIn = json.optBoolean("preventDeviceMultiCheckIn", true),
        maxAllowedRadiusMetersLimit = json.optInt("maxAllowedRadiusMetersLimit", 200),
        dynamicQrRotationSeconds = json.optInt("dynamicQrRotationSeconds", 30),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun branchSettingsToMap(b: BranchAttendanceSettings): Map<String, Any> = mapOf(
        "branchId" to b.branchId,
        "branchName" to b.branchName,
        "churchPrefix" to b.churchPrefix,
        "activeMode" to b.activeMode,
        "venueQrType" to b.venueQrType,
        "latitude" to b.latitude,
        "longitude" to b.longitude,
        "allowedRadiusMeters" to b.allowedRadiusMeters,
        "delegatedUsherUserIds" to b.delegatedUsherUserIds,
        "delegatedUsherNames" to b.delegatedUsherNames,
        "secretHmacKey" to b.secretHmacKey,
        "isOfflineCheckInEnabled" to b.isOfflineCheckInEnabled,
        "updatedAt" to b.updatedAt
    )

    private fun parseBranchSettingsFromJson(json: JSONObject): BranchAttendanceSettings = BranchAttendanceSettings(
        branchId = json.optString("branchId", "branch_ncc_01"),
        branchName = json.optString("branchName", "न्यू क्रिएशन चर्च"),
        churchPrefix = json.optString("churchPrefix", "NCC"),
        activeMode = json.optString("activeMode", "dual_mode"),
        venueQrType = json.optString("venueQrType", "static_wall_poster"),
        latitude = json.optDouble("latitude", 21.1904),
        longitude = json.optDouble("longitude", 81.2849),
        allowedRadiusMeters = json.optInt("allowedRadiusMeters", 50),
        delegatedUsherUserIds = json.optJSONArray("delegatedUsherUserIds")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList(),
        delegatedUsherNames = json.optJSONArray("delegatedUsherNames")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList(),
        secretHmacKey = json.optString("secretHmacKey", "ncc_attendance_hmac_secret_2026"),
        isOfflineCheckInEnabled = json.optBoolean("isOfflineCheckInEnabled", true),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
    )

    private fun branchSettingsToJson(b: BranchAttendanceSettings): JSONObject = JSONObject().apply {
        put("branchId", b.branchId)
        put("branchName", b.branchName)
        put("churchPrefix", b.churchPrefix)
        put("activeMode", b.activeMode)
        put("venueQrType", b.venueQrType)
        put("latitude", b.latitude)
        put("longitude", b.longitude)
        put("allowedRadiusMeters", b.allowedRadiusMeters)
        put("delegatedUsherUserIds", JSONArray(b.delegatedUsherUserIds))
        put("delegatedUsherNames", JSONArray(b.delegatedUsherNames))
        put("secretHmacKey", b.secretHmacKey)
        put("isOfflineCheckInEnabled", b.isOfflineCheckInEnabled)
        put("updatedAt", b.updatedAt)
    }
}
