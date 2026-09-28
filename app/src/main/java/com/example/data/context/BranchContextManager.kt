package com.example.data.context

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.example.data.model.AdminUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Keep
data class ChurchBranchInfo(
    val branchId: String,
    val branchName: String,
    val branchShortCode: String = "NCC",
    val location: String = "",
    val isMainBranch: Boolean = false,
    val pastorInCharge: String = ""
)

@Keep
data class ActiveBranchContextState(
    val activeScope: String = "branch_ncc_01", // "GLOBAL" or specific branchId
    val activeBranchInfo: ChurchBranchInfo = DefaultChurches.MAIN_BRANCH,
    val isGlobalScope: Boolean = false,
    val accessibleBranches: List<ChurchBranchInfo> = listOf(DefaultChurches.MAIN_BRANCH),
    val isMultiBranchUser: Boolean = false,
    val userRoleTier: String = "master_admin"
)

object DefaultChurches {
    val MAIN_BRANCH = ChurchBranchInfo(
        branchId = "branch_ncc_01",
        branchName = "न्यू क्रिएशन चर्च (मुख्य कलीसिया • भिलाई)",
        branchShortCode = "NCC-01",
        location = "भिलाई, छत्तीसगढ़",
        isMainBranch = true,
        pastorInCharge = "रेव. विनय कुमार"
    )

    val KUMHARI_BRANCH = ChurchBranchInfo(
        branchId = "branch_ncc_khwr",
        branchName = "न्यू क्रिएशन कलीसिया (कुम्हारी शाखा)",
        branchShortCode = "NCC-KMH",
        location = "कुम्हारी, दुर्ग",
        isMainBranch = false,
        pastorInCharge = "पास्टर थॉमस"
    )

    val RAIPUR_BRANCH = ChurchBranchInfo(
        branchId = "branch_ncc_rpr",
        branchName = "न्यू क्रिएशन कलीसिया (रायपुर शाखा)",
        branchShortCode = "NCC-RPR",
        location = "रायपुर, छत्तीसगढ़",
        isMainBranch = false,
        pastorInCharge = "पास्टर जॉन"
    )

    val DURG_BRANCH = ChurchBranchInfo(
        branchId = "branch_ncc_drg",
        branchName = "न्यू क्रिएशन कलीसिया (दुर्ग प्रार्थना भवन)",
        branchShortCode = "NCC-DRG",
        location = "दुर्ग, छत्तीसगढ़",
        isMainBranch = false,
        pastorInCharge = "पास्टर डेविड"
    )

    val ALL_BRANCHES = listOf(MAIN_BRANCH, KUMHARI_BRANCH, RAIPUR_BRANCH, DURG_BRANCH)
    const val GLOBAL_SCOPE_ID = "GLOBAL"
}

/**
 * Universal Role-Adaptive Branch Context Switcher Engine.
 * Supports Master Admin, Bishop, Multi-Branch Pastor, and Single-Branch Pastor.
 */
class BranchContextManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("branch_context_prefs", Context.MODE_PRIVATE)

    private val _contextState = MutableStateFlow(ActiveBranchContextState())
    val contextState: StateFlow<ActiveBranchContextState> = _contextState.asStateFlow()

    private var currentAdminUser: AdminUser? = null

    companion object {
        @Volatile
        private var instance: BranchContextManager? = null

        fun getInstance(context: Context): BranchContextManager {
            return instance ?: synchronized(this) {
                instance ?: BranchContextManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initializes or updates context based on user profile and role tier permissions.
     */
    fun initializeUserBranchContext(user: AdminUser?) {
        currentAdminUser = user
        if (user == null) {
            _contextState.value = ActiveBranchContextState()
            return
        }

        val isMaster = user.isMasterAdmin() || user.rank >= 5
        val isBishop = user.rank in 3..4
        val isGlobalAccess = isMaster || isBishop || user.accessibleBranches.contains("*")

        val accessible = if (isGlobalAccess) {
            DefaultChurches.ALL_BRANCHES
        } else {
            val userBranchIds = if (user.accessibleBranches.isNotEmpty()) {
                user.accessibleBranches
            } else {
                listOf(user.homeBranchId.ifBlank { "branch_ncc_01" })
            }
            val filtered = DefaultChurches.ALL_BRANCHES.filter { it.branchId in userBranchIds }
            if (filtered.isNotEmpty()) filtered else listOf(DefaultChurches.MAIN_BRANCH)
        }

        val isMultiBranch = isGlobalAccess || accessible.size > 1

        // Default branch rule: Always prioritize user.homeBranchId on initial boot
        val savedScope = prefs.getString("persisted_active_branch_id", null)
        val targetScope = when {
            savedScope != null && (savedScope == DefaultChurches.GLOBAL_SCOPE_ID && isGlobalAccess) -> DefaultChurches.GLOBAL_SCOPE_ID
            savedScope != null && accessible.any { it.branchId == savedScope } -> savedScope
            user.homeBranchId.isNotBlank() && accessible.any { it.branchId == user.homeBranchId } -> user.homeBranchId
            isGlobalAccess -> DefaultChurches.GLOBAL_SCOPE_ID
            else -> accessible.firstOrNull()?.branchId ?: "branch_ncc_01"
        }

        applyScope(
            targetScope = targetScope,
            accessibleList = accessible,
            isMultiBranch = isMultiBranch,
            isGlobalAccess = isGlobalAccess,
            user = user
        )
    }

    /**
     * Switches active branch context with strict validation and sub-100ms state emission.
     */
    fun switchBranchContext(targetBranchIdOrGlobal: String, onComplete: ((Boolean) -> Unit)? = null) {
        val currentState = _contextState.value
        val user = currentAdminUser
        val isMaster = user?.isMasterAdmin() == true || (user?.rank ?: 0) >= 5
        val isBishop = (user?.rank ?: 0) in 3..4
        val isGlobalEligible = isMaster || isBishop || user?.accessibleBranches?.contains("*") == true

        if (targetBranchIdOrGlobal == DefaultChurches.GLOBAL_SCOPE_ID && !isGlobalEligible) {
            onComplete?.invoke(false)
            return
        }

        val branchMatch = DefaultChurches.ALL_BRANCHES.find { it.branchId == targetBranchIdOrGlobal }
        if (targetBranchIdOrGlobal != DefaultChurches.GLOBAL_SCOPE_ID && branchMatch == null) {
            onComplete?.invoke(false)
            return
        }

        // Persist
        prefs.edit().putString("persisted_active_branch_id", targetBranchIdOrGlobal).apply()

        applyScope(
            targetScope = targetBranchIdOrGlobal,
            accessibleList = currentState.accessibleBranches,
            isMultiBranch = currentState.isMultiBranchUser,
            isGlobalAccess = isGlobalEligible,
            user = user
        )

        // Propagate to AttendanceGovernanceRepository if specific branch
        if (targetBranchIdOrGlobal != DefaultChurches.GLOBAL_SCOPE_ID) {
            try {
                com.example.data.repository.AttendanceGovernanceRepository.getInstance(context)
                    .switchActiveBranch(targetBranchIdOrGlobal, branchMatch?.branchName ?: "")
            } catch (_: Exception) {}
        }

        onComplete?.invoke(true)
    }

    private fun applyScope(
        targetScope: String,
        accessibleList: List<ChurchBranchInfo>,
        isMultiBranch: Boolean,
        isGlobalAccess: Boolean,
        user: AdminUser?
    ) {
        val isGlobal = targetScope == DefaultChurches.GLOBAL_SCOPE_ID
        val branchInfo = DefaultChurches.ALL_BRANCHES.find { it.branchId == targetScope } ?: DefaultChurches.MAIN_BRANCH

        _contextState.value = ActiveBranchContextState(
            activeScope = targetScope,
            activeBranchInfo = branchInfo,
            isGlobalScope = isGlobal,
            accessibleBranches = accessibleList,
            isMultiBranchUser = isMultiBranch,
            userRoleTier = user?.roleTier ?: (if (user?.isMasterAdmin() == true) "master_admin" else "pastor")
        )
    }
}
