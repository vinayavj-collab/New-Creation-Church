package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppUpdateState(
    val repoPath: String = "vinayavj000/VinayKumarAVJ",
    val apiUrl: String = "https://api.github.com/repos/vinayavj000/VinayKumarAVJ/releases/latest",
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val publishedAt: String = "",
    val apkDownloadUrl: String? = null,
    val apkFileName: String? = null,
    val apkSizeBytes: Long = 0L,
    val lastCheckedTimestamp: Long = 0L,
    val isUpdateAvailable: Boolean = false,
    val statusMessage: String = "जांच की जा रही है...",
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercentage: Int = 0,
    val downloadedApkFile: File? = null,
    val errorMessage: String? = null
)

class AppUpdateManager private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)

    private val _updateState = MutableStateFlow(loadCachedState())
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    init {
        val detectedRepo = detectGitHubRepo(context)
        val current = _updateState.value
        if (current.repoPath != detectedRepo) {
            val apiUrl = "https://api.github.com/repos/$detectedRepo/releases/latest"
            _updateState.value = current.copy(repoPath = detectedRepo, apiUrl = apiUrl)
        }
    }

    companion object {
        @Volatile
        private var instance: AppUpdateManager? = null

        fun getInstance(context: Context): AppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateManager(context.applicationContext).also { instance = it }
            }
        }

        fun detectGitHubRepo(context: Context): String {
            // 1. SharedPreferences saved override if any
            val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
            val savedRepo = prefs.getString("custom_github_repo", null)
            if (!savedRepo.isNullOrBlank()) {
                return savedRepo.trim()
            }

            // 2. Try parsing .git/config if present in project workspace
            try {
                val gitConfigFile = File(".git/config")
                if (gitConfigFile.exists()) {
                    val content = gitConfigFile.readText()
                    val match = Regex("""url\s*=\s*.*github\.com[/:]([^/\s]+\/[^\s\.]+)(?:\.git)?""").find(content)
                    if (match != null) {
                        val repo = match.groupValues[1].trim()
                        if (repo.isNotBlank()) return repo
                    }
                }
            } catch (e: Exception) {
                Log.d("AppUpdateManager", "No .git/config found: ${e.message}")
            }

            // 3. Environment variable GITHUB_REPOSITORY
            val envRepo = System.getenv("GITHUB_REPOSITORY")
            if (!envRepo.isNullOrBlank()) {
                return envRepo.trim()
            }

            // 4. Default repository for Vinay Kumar AVJ App
            return "vinayavj000/VinayKumarAVJ"
        }
    }

    private fun loadCachedState(): AppUpdateState {
        val repoPath = prefs.getString("repo_path", detectGitHubRepo(context)) ?: "vinayavj000/VinayKumarAVJ"
        val apiUrl = "https://api.github.com/repos/$repoPath/releases/latest"
        val lastChecked = prefs.getLong("last_checked", 0L)
        val latestCode = prefs.getInt("latest_code", 0)
        val latestName = prefs.getString("latest_name", "") ?: ""
        val title = prefs.getString("release_title", "") ?: ""
        val notes = prefs.getString("release_notes", "") ?: ""
        val pubAt = prefs.getString("published_at", "") ?: ""
        val apkUrl = prefs.getString("apk_url", null)
        val apkName = prefs.getString("apk_name", null)
        val isUpdateAvailable = latestCode > BuildConfig.VERSION_CODE

        val initialStatus = when {
            lastChecked == 0L -> "अभी तक जांच नहीं की गई है"
            isUpdateAvailable -> "🆕 नया App Update उपलब्ध है"
            else -> "आपका App नवीनतम Version पर है"
        }

        return AppUpdateState(
            repoPath = repoPath,
            apiUrl = apiUrl,
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionCode = latestCode,
            latestVersionName = latestName,
            releaseTitle = title,
            releaseNotes = notes,
            publishedAt = pubAt,
            apkDownloadUrl = apkUrl,
            apkFileName = apkName,
            lastCheckedTimestamp = lastChecked,
            isUpdateAvailable = isUpdateAvailable,
            statusMessage = initialStatus
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun updateCustomRepoPath(newRepoPath: String) {
        val cleanPath = newRepoPath.trim().removePrefix("https://github.com/").removeSuffix(".git")
        if (cleanPath.isNotBlank()) {
            prefs.edit().putString("custom_github_repo", cleanPath).apply()
            val apiUrl = "https://api.github.com/repos/$cleanPath/releases/latest"
            _updateState.value = _updateState.value.copy(
                repoPath = cleanPath,
                apiUrl = apiUrl,
                lastCheckedTimestamp = 0L,
                statusMessage = "Repository बदला गया। कृपया चेक करें।"
            )
        }
    }

    suspend fun checkForUpdates(force: Boolean = true): AppUpdateState = withContext(Dispatchers.IO) {
        val current = _updateState.value
        val cooldownMillis = 6 * 3600 * 1000L // 6 hours
        val timeSinceLastCheck = System.currentTimeMillis() - current.lastCheckedTimestamp

        if (!force && current.lastCheckedTimestamp > 0 && timeSinceLastCheck < cooldownMillis) {
            return@withContext current
        }

        if (!isNetworkAvailable()) {
            val offlineState = current.copy(
                isChecking = false,
                errorMessage = "Internet connection उपलब्ध नहीं है.",
                statusMessage = "Internet connection उपलब्ध नहीं है."
            )
            _updateState.value = offlineState
            return@withContext offlineState
        }

        _updateState.value = current.copy(isChecking = true, errorMessage = null, statusMessage = "GitHub से जांच हो रही है...")

        try {
            val url = URL(current.apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "VinayKumarAVJ-App/${BuildConfig.VERSION_CODE}")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorMsg = if (responseCode == 404) {
                    "GitHub पर इस repository (${current.repoPath}) की कोई release उपलब्ध नहीं है."
                } else {
                    "GitHub API त्रुटि: Server response HTTP $responseCode"
                }
                val errorState = current.copy(
                    isChecking = false,
                    errorMessage = errorMsg,
                    statusMessage = errorMsg,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
                _updateState.value = errorState
                return@withContext errorState
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseObj = JSONObject(jsonText)

            val tagName = releaseObj.optString("tag_name", "")
            val title = releaseObj.optString("name", tagName)
            val body = releaseObj.optString("body", "कोई Release Notes उपलब्ध नहीं हैं.")
            val publishedAt = releaseObj.optString("published_at", "")
            val isDraft = releaseObj.optBoolean("draft", false)
            val isPrerelease = releaseObj.optBoolean("prerelease", false)

            if (isDraft || isPrerelease) {
                val msg = "नवीनतम रिलीज़ draft/prerelease है।"
                val state = current.copy(
                    isChecking = false,
                    statusMessage = msg,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
                _updateState.value = state
                return@withContext state
            }

            // Extract version code from tag_name or release title
            val latestCode = extractVersionCode(tagName, title)
            val latestName = tagName.removePrefix("v").removePrefix("V")

            // Assets parsing: ONLY pick .apk asset
            val assetsArray: JSONArray = releaseObj.optJSONArray("assets") ?: JSONArray()
            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize = 0L

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val assetName = asset.optString("name", "")
                val contentType = asset.optString("content_type", "")
                val downloadUrl = asset.optString("browser_download_url", "")
                val size = asset.optLong("size", 0L)

                if (assetName.endsWith(".apk", ignoreCase = true) || contentType.contains("android.package-archive")) {
                    apkUrl = downloadUrl
                    apkName = assetName
                    apkSize = size
                    break
                }
            }

            val now = System.currentTimeMillis()
            val isUpdateAvailable = latestCode > BuildConfig.VERSION_CODE

            val statusMsg = if (isUpdateAvailable) {
                "🆕 नया App Update उपलब्ध है"
            } else {
                "आपका App नवीनतम Version पर है"
            }

            // Cache in SharedPreferences
            prefs.edit().apply {
                putLong("last_checked", now)
                putInt("latest_code", latestCode)
                putString("latest_name", latestName)
                putString("release_title", title)
                putString("release_notes", body)
                putString("published_at", publishedAt)
                putString("apk_url", apkUrl)
                putString("apk_name", apkName)
                putString("repo_path", current.repoPath)
                apply()
            }

            val newState = current.copy(
                isChecking = false,
                latestVersionCode = latestCode,
                latestVersionName = latestName,
                releaseTitle = title,
                releaseNotes = body,
                publishedAt = publishedAt,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                lastCheckedTimestamp = now,
                isUpdateAvailable = isUpdateAvailable,
                statusMessage = statusMsg,
                errorMessage = if (isUpdateAvailable && apkUrl == null) "GitHub Release में APK फ़ाइल अटैच नहीं है." else null
            )

            _updateState.value = newState
            return@withContext newState

        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Check for update failed", e)
            val errState = current.copy(
                isChecking = false,
                errorMessage = "जांच त्रुटि: ${e.localizedMessage ?: "Network error"}",
                statusMessage = "अपडेट जांचने में त्रुटि हुई.",
                lastCheckedTimestamp = System.currentTimeMillis()
            )
            _updateState.value = errState
            return@withContext errState
        }
    }

    private fun extractVersionCode(tagName: String, title: String): Int {
        // First try to parse pure numbers in tagName (e.g. "v23" -> 23)
        val tagDigits = tagName.replace(Regex("[^0-9]"), "")
        if (tagDigits.isNotBlank()) {
            val parsed = tagDigits.toIntOrNull()
            if (parsed != null && parsed > 0) return parsed
        }
        val titleDigits = title.replace(Regex("[^0-9]"), "")
        if (titleDigits.isNotBlank()) {
            val parsed = titleDigits.toIntOrNull()
            if (parsed != null && parsed > 0) return parsed
        }
        return 0
    }

    suspend fun downloadAndInstallApk(onProgress: (Int) -> Unit = {}) = withContext(Dispatchers.IO) {
        val current = _updateState.value
        val downloadUrl = current.apkDownloadUrl

        if (downloadUrl.isNullOrBlank()) {
            _updateState.value = current.copy(errorMessage = "APK डाउनलोड लिंक उपलब्ध नहीं है.")
            return@withContext
        }

        if (!isNetworkAvailable()) {
            _updateState.value = current.copy(errorMessage = "Internet connection उपलब्ध नहीं है.")
            return@withContext
        }

        _updateState.value = current.copy(isDownloading = true, downloadProgressPercentage = 0, errorMessage = null)

        try {
            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            val fileName = current.apkFileName ?: "VinayKumarAVJ_Update.apk"
            val targetFile = File(downloadsDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "VinayKumarAVJ-App/${BuildConfig.VERSION_CODE}")
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_MOVED_TEMP && responseCode != HttpURLConnection.HTTP_MOVED_PERM) {
                // Check for redirect header manually if needed
                val redirectUrlStr = connection.getHeaderField("Location")
                if (!redirectUrlStr.isNullOrEmpty()) {
                    val redirectUrl = URL(redirectUrlStr)
                    val conn2 = redirectUrl.openConnection() as HttpURLConnection
                    conn2.requestMethod = "GET"
                    conn2.setRequestProperty("User-Agent", "VinayKumarAVJ-App/${BuildConfig.VERSION_CODE}")
                    conn2.connect()
                    downloadStreamToFile(conn2, targetFile, onProgress)
                } else {
                    throw Exception("Download HTTP status code: $responseCode")
                }
            } else {
                downloadStreamToFile(connection, targetFile, onProgress)
            }

            // Validate the downloaded APK
            val validationError = validateApk(targetFile)
            if (validationError != null) {
                targetFile.delete()
                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    errorMessage = validationError,
                    statusMessage = validationError
                )
                return@withContext
            }

            _updateState.value = _updateState.value.copy(
                isDownloading = false,
                downloadProgressPercentage = 100,
                downloadedApkFile = targetFile,
                statusMessage = "APK डाउनलोड पूर्ण। इंस्टॉलेशन विंडो खोल रहे हैं..."
            )

            // Prompt install
            openInstaller(targetFile)

        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Download failed", e)
            _updateState.value = _updateState.value.copy(
                isDownloading = false,
                errorMessage = "डाउनलोड त्रुटि: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun downloadStreamToFile(connection: HttpURLConnection, targetFile: File, onProgress: (Int) -> Unit) {
        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L

        connection.inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                        _updateState.value = _updateState.value.copy(downloadProgressPercentage = progress)
                        onProgress(progress)
                    }
                }
                output.flush()
            }
        }
    }

    private fun validateApk(apkFile: File): String? {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return "APK फ़ाइल खाली या अमान्य है।"
        }

        try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val pkgInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return "APK पैकेज जानकारी पढ़ने में विफल। फ़ाइल क्षतिग्रस्त हो सकती है।"

            // 1. Validate package ID
            val expectedPackage = context.packageName
            val apkPackage = pkgInfo.packageName
            if (apkPackage != expectedPackage) {
                return "पैकेज ID मेल नहीं खाता (APK: $apkPackage, App: $expectedPackage)"
            }

            // 2. Validate versionCode
            val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode
            }

            if (apkVersionCode <= BuildConfig.VERSION_CODE) {
                return "APK का Version ($apkVersionCode) वर्तमान Version (${BuildConfig.VERSION_CODE}) से पुराना या समान है।"
            }

        } catch (e: Exception) {
            Log.e("AppUpdateManager", "APK validation error", e)
            return "APK सत्यापन में त्रुटि: ${e.localizedMessage}"
        }

        return null // Validation passed
    }

    fun openInstaller(apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to launch installer", e)
            _updateState.value = _updateState.value.copy(
                errorMessage = "पैकेज इंस्टॉलर खोलने में असमर्थ: ${e.localizedMessage}"
            )
        }
    }

    fun formatLastCheckedTime(timestamp: Long): String {
        if (timestamp == 0L) return "कभी नहीं"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
