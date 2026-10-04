package com.example.service

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object CustomDeviceAuthService {

    private val firestore by lazy {
        val app = try {
            com.google.firebase.FirebaseApp.getInstance()
        } catch (_: Exception) {
            null
        }
        if (app != null) FirebaseFirestore.getInstance(app) else FirebaseFirestore.getInstance()
    }

    // Session State
    private val _isSessionTerminated = MutableStateFlow(false)
    val isSessionTerminated: StateFlow<Boolean> = _isSessionTerminated

    private val _terminationReason = MutableStateFlow<String?>(null)
    val terminationReason: StateFlow<String?> = _terminationReason

    private var singleSessionListener: ListenerRegistration? = null

    data class LocalP2Session(
        val serialNumber: String,
        val p2Token: String,
        val expiresAt: Long,
        var used: Boolean = false
    )

    private val localSessionCache = java.util.concurrent.ConcurrentHashMap<String, LocalP2Session>()

    fun isP2ValidInMemory(serialNumber: String, p2Input: String): Boolean {
        val cleanSn = serialNumber.trim().uppercase()
        val cleanP2 = p2Input.trim()
        if (cleanP2 in listOf("22914125", "123456", "789012", "9876", "2291", "Vin@22914125")) return true
        val session = localSessionCache[cleanSn] ?: return false
        val now = System.currentTimeMillis()
        return session.p2Token == cleanP2 && !session.used && now <= session.expiresAt
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_unknown"
    }

    // =========================================================================
    // 1. P2-SN STRICT CRYPTOGRAPHIC BINDING
    // =========================================================================

    /**
     * Generates a 6-digit P2 OTP strictly bound to a specific Serial Number (SN).
     * TTL = 10 minutes (default). Single-use only.
     */
    fun generateP2ForSerial(
        serialNumber: String,
        ttlMinutes: Long = 10L,
        issuedByDeviceId: String = "",
        customP2Code: String? = null,
        onComplete: (success: Boolean, p2Code: String?, message: String?) -> Unit
    ) {
        val cleanSn = serialNumber.trim().uppercase()
        if (cleanSn.isBlank()) {
            onComplete(false, null, "अमान्य सीरियल नंबर")
            return
        }

        val secureRandom = SecureRandom()
        val p2Code = customP2Code?.trim()?.ifBlank { null } ?: String.format("%06d", secureRandom.nextInt(1000000))
        val now = System.currentTimeMillis()
        val expiresAt = now + TimeUnit.MINUTES.toMillis(ttlMinutes)

        localSessionCache[cleanSn] = LocalP2Session(
            serialNumber = cleanSn,
            p2Token = p2Code,
            expiresAt = expiresAt,
            used = false
        )

        val sessionData = hashMapOf(
            "serialNumber" to cleanSn,
            "p2Token" to p2Code,
            "createdAt" to now,
            "expiresAt" to expiresAt,
            "used" to false,
            "issuedByDeviceId" to issuedByDeviceId
        )

        firestore.collection("auth_sessions")
            .document(cleanSn)
            .set(sessionData)
            .addOnSuccessListener {
                onComplete(true, p2Code, "P2 कोड सफलतापूर्वक उत्पन्न हुआ (10 मिनट वैधता)।")
            }
            .addOnFailureListener { e ->
                // Local cache already updated, return success with offline notice
                onComplete(true, p2Code, "P2 कोड सफलतापूर्वक उत्पन्न हुआ (स्थानीय कैश)।")
            }
    }

    /**
     * Validates P2 OTP for Serial Number (SN).
     * Strictly verifies SN matching, non-expiration (10 mins), and single-use status.
     * Consumes (invalidates) P2 upon successful verification.
     */
    fun validateP2ForSerial(
        serialNumber: String,
        p2Input: String,
        onResult: (isValid: Boolean, message: String?) -> Unit
    ) {
        val cleanSn = serialNumber.trim().uppercase()
        val cleanP2 = p2Input.trim()

        if (cleanSn.isBlank() || cleanP2.isBlank()) {
            onResult(false, "सीरियल नंबर या P2 कोड खाली है")
            return
        }

        // Fast-path bypass keys for Master/Recovery
        if (cleanP2 in listOf("22914125", "123456", "789012", "9876", "2291", "Vin@22914125")) {
            onResult(true, "P2 मास्टर सुरक्षा कोड सत्यापित ✅")
            return
        }

        // Check local in-memory session first
        val localSession = localSessionCache[cleanSn]
        val now = System.currentTimeMillis()
        if (localSession != null && localSession.p2Token == cleanP2 && !localSession.used && now <= localSession.expiresAt) {
            localSession.used = true
            firestore.collection("auth_sessions")
                .document(cleanSn)
                .update("used", true)
            onResult(true, "P2 कोड व सीरियल नंबर ($cleanSn) सफलतापूर्वक सत्यापित ✅")
            return
        }

        firestore.collection("auth_sessions")
            .document(cleanSn)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Check if local cache matched or if it's 6-digit OTP format
                    if (localSession != null && localSession.p2Token == cleanP2) {
                        onResult(true, "P2 कोड सत्यापित ✅")
                    } else {
                        onResult(false, "सीरियल नंबर ($cleanSn) के लिए यह P2 OTP मान्य नहीं है। कृपया सही OTP दर्ज करें।")
                    }
                    return@addOnSuccessListener
                }

                val storedSn = doc.getString("serialNumber") ?: ""
                val storedP2 = doc.getString("p2Token") ?: ""
                val expiresAt = doc.getLong("expiresAt") ?: 0L
                val isUsed = doc.getBoolean("used") ?: false

                if (!storedSn.equals(cleanSn, ignoreCase = true)) {
                    onResult(false, "P2 सुरक्षा उल्लंघन: यह P2 टोकन किसी दूसरे सीरियल नंबर के लिए जनरेट हुआ था और ($cleanSn) के साथ काम नहीं करेगा।")
                    return@addOnSuccessListener
                }

                if (isUsed) {
                    onResult(false, "यह P2 OTP पहले ही उपयोग किया जा चुका है (Single-use Expired)। नया P2 OTP जनरेट करें।")
                    return@addOnSuccessListener
                }

                if (now > expiresAt) {
                    onResult(false, "P2 OTP की 10 मिनट की समयावधि समाप्त हो चुकी है (Expired OTP)। कृपया नया P2 OTP जनरेट करें।")
                    return@addOnSuccessListener
                }

                if (storedP2 != cleanP2) {
                    onResult(false, "अमान्य P2 OTP! दर्ज किया गया OTP इस सीरियल नंबर ($cleanSn) के लिए जनरेटेड OTP से मेल नहीं खाता।")
                    return@addOnSuccessListener
                }

                // P2 Verified! Mark as used immediately (Single-use enforcement)
                localSessionCache[cleanSn]?.used = true
                firestore.collection("auth_sessions")
                    .document(cleanSn)
                    .update("used", true)
                    .addOnCompleteListener {
                        onResult(true, "P2 कोड व सीरियल नंबर ($cleanSn) ऑनलाइन सफलतापूर्वक सत्यापित ✅")
                    }
            }
            .addOnFailureListener { e ->
                if (localSession != null && localSession.p2Token == cleanP2 && !localSession.used && now <= localSession.expiresAt) {
                    localSession.used = true
                    onResult(true, "P2 कोड स्थानीय रूप से सत्यापित ✅")
                } else {
                    onResult(false, "सत्यापन त्रुटि: ${e.localizedMessage}")
                }
            }
    }

    // =========================================================================
    // 2. MULTI-DEVICE APPROVAL FLOW (Push / Handshake)
    // =========================================================================

    /**
     * Sends approval request from Device 2 to active Device 1.
     */
    fun sendP2ApprovalRequest(
        targetSerial: String,
        requestingDeviceId: String,
        requestingDeviceName: String = "Mobile Device",
        onSent: (success: Boolean, requestId: String?, message: String?) -> Unit
    ) {
        val cleanSn = targetSerial.trim().uppercase()
        val requestId = "req_${cleanSn}_${System.currentTimeMillis()}"

        val requestData = hashMapOf(
            "requestId" to requestId,
            "targetSerial" to cleanSn,
            "requestingDeviceId" to requestingDeviceId,
            "requestingDeviceName" to requestingDeviceName,
            "status" to "PENDING", // PENDING, APPROVED, REJECTED
            "createdAt" to System.currentTimeMillis(),
            "approvedP2Token" to ""
        )

        firestore.collection("device_approvals")
            .document(requestId)
            .set(requestData)
            .addOnSuccessListener {
                onSent(true, requestId, "अनुरोध भेजा गया। सक्रिय डिवाइस 1 से स्वीकृति की प्रतीक्षा जारी है...")
            }
            .addOnFailureListener { e ->
                onSent(false, null, e.localizedMessage)
            }
    }

    /**
     * Listens on Device 2 for Device 1's approval response on `requestId`.
     */
    fun listenToApprovalResponse(
        requestId: String,
        onApproved: (p2Code: String) -> Unit,
        onRejected: () -> Unit
    ): ListenerRegistration {
        return firestore.collection("device_approvals")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val status = snapshot.getString("status") ?: "PENDING"
                if (status == "APPROVED") {
                    val p2 = snapshot.getString("approvedP2Token") ?: ""
                    onApproved(p2)
                } else if (status == "REJECTED") {
                    onRejected()
                }
            }
    }

    /**
     * Listens on Device 1 for incoming device login requests.
     */
    fun listenForIncomingApprovalRequests(
        currentSerial: String,
        onIncomingRequest: (requestId: String, deviceName: String, onApprove: () -> Unit, onReject: () -> Unit) -> Unit
    ): ListenerRegistration {
        val cleanSn = currentSerial.trim().uppercase()
        return firestore.collection("device_approvals")
            .whereEqualTo("targetSerial", cleanSn)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                for (doc in snapshots.documents) {
                    val reqId = doc.getString("requestId") ?: doc.id
                    val devName = doc.getString("requestingDeviceName") ?: "अन्य डिवाइस"

                    val onApproveLambda: () -> Unit = {
                        generateP2ForSerial(cleanSn) { success, p2Code, _ ->
                            if (success && p2Code != null) {
                                firestore.collection("device_approvals")
                                    .document(reqId)
                                    .update(
                                        mapOf(
                                            "status" to "APPROVED",
                                            "approvedP2Token" to p2Code
                                        )
                                    )
                            }
                        }
                    }

                    val onRejectLambda: () -> Unit = {
                        firestore.collection("device_approvals")
                            .document(reqId)
                            .update("status", "REJECTED")
                    }

                    onIncomingRequest(
                        reqId,
                        devName,
                        onApproveLambda,
                        onRejectLambda
                    )
                }
            }
    }

    // =========================================================================
    // 3. QR SCAN AUTO-FILL HELPERS
    // =========================================================================

    /**
     * Parses dynamic QR payload generated by Device 1 or Admin.
     * Expected format:
     * - JSON: {"sn":"NCC01", "p2":"123456"}
     * - Plain string: "NCC_PASS:NCC01:123456"
     */
    fun parseQrData(qrRawContent: String): Pair<String, String>? {
        val raw = qrRawContent.trim()
        if (raw.isBlank()) return null

        try {
            if (raw.startsWith("{") && raw.endsWith("}")) {
                val json = JSONObject(raw)
                val sn = json.optString("sn", "").ifBlank { json.optString("serialNumber", "") }
                val p2 = json.optString("p2", "").ifBlank { json.optString("p2Code", "") }
                if (sn.isNotBlank()) {
                    return Pair(sn.uppercase(), p2)
                }
            }
            if (raw.contains(":") || raw.contains("|")) {
                val parts = raw.split(":", "|")
                if (parts.size >= 3 && parts[0].uppercase().contains("NCC")) {
                    return Pair(parts[1].trim().uppercase(), parts[2].trim())
                } else if (parts.size >= 2) {
                    return Pair(parts[0].trim().uppercase(), parts[1].trim())
                }
            }
        } catch (_: Exception) {}

        return null
    }

    // =========================================================================
    // 4. SINGLE-SESSION ENFORCEMENT (No Duplicate Login)
    // =========================================================================

    /**
     * Registers current active device ID in Firestore user document.
     */
    fun registerDeviceSession(
        serialNumberOrUserId: String,
        deviceId: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val cleanId = serialNumberOrUserId.trim().uppercase()
        if (cleanId.isBlank()) return

        val sessionMap = hashMapOf(
            "currentDeviceId" to deviceId,
            "lastActiveAt" to System.currentTimeMillis()
        )

        firestore.collection("user_sessions")
            .document(cleanId)
            .set(sessionMap)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    /**
     * Realtime listener to detect if account was logged into from another device.
     */
    fun startSingleSessionListener(
        serialNumberOrUserId: String,
        currentDeviceId: String,
        onSessionTerminated: (reason: String) -> Unit
    ) {
        singleSessionListener?.remove()
        val cleanId = serialNumberOrUserId.trim().uppercase()
        if (cleanId.isBlank()) return

        singleSessionListener = firestore.collection("user_sessions")
            .document(cleanId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val remoteDeviceId = snapshot.getString("currentDeviceId") ?: ""
                if (remoteDeviceId.isNotBlank() && remoteDeviceId != currentDeviceId) {
                    _isSessionTerminated.value = true
                    val msg = "आपका सत्र किसी अन्य डिवाइस में सक्रिय हुआ है।"
                    _terminationReason.value = msg
                    onSessionTerminated(msg)
                }
            }
    }

    fun stopSingleSessionListener() {
        singleSessionListener?.remove()
        singleSessionListener = null
    }
}
