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
    val repoPath: String = "vinayavj-collab/New-Creation-Church",
    val apiUrl: String = "https://api.github.com/repos/vinayavj-collab/New-Creation-Church/releases/latest",
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
        // Safe Auto-Cleanup on App Startup (deletes leftover/old APKs)
        cleanUpOldApks()
    }

    private fun cleanUpOldApks() {
        try {
            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            downloadsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".apk", ignoreCase = true)) {
                    // Delete old or leftover APKs upon app launch
                    val deleted = file.delete()
                    Log.d("AppUpdateManager", "Cleaned up old APK at startup: ${file.name}, success: $deleted")
                }
            }
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Error cleaning up old APKs", e)
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
            return "vinayavj-collab/New-Creation-Church"
        }
    }

    private fun loadCachedState(): AppUpdateState {
        val repoPath = "vinayavj-collab/New-Creation-Church"
        val apiUrl = "https://api.github.com/repos/$repoPath/releases/latest"
        val lastChecked = prefs.getLong("last_checked", 0L)
        val latestCode = prefs.getInt("latest_code", 0)
        val latestName = prefs.getString("latest_name", "") ?: ""
        val title = prefs.getString("release_title", "") ?: ""
        val notes = prefs.getString("release_notes", "") ?: ""
        val pubAt = prefs.getString("published_at", "") ?: ""
        val apkUrl = prefs.getString("apk_url", null)
        val apkName = prefs.getString("apk_name", null)
        val isUpdateAvailable = isVersionNewer(
            currentCode = BuildConfig.VERSION_CODE,
            currentName = BuildConfig.VERSION_NAME,
            latestCode = latestCode,
            latestName = latestName
        )

        val initialStatus = when {
            lastChecked == 0L -> "अभी तक जांच नहीं की गई है"
            isUpdateAvailable -> "🆕 नया App Update उपलब्ध है"
            else -> "आपका App नवीनतम Version (v${BuildConfig.VERSION_NAME}) पर है"
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
                val statusMsg = "आपका App नवीनतम Version (v${current.currentVersionName}) पर है."
                val errorState = current.copy(
                    isChecking = false,
                    latestVersionCode = current.currentVersionCode,
                    latestVersionName = current.currentVersionName,
                    isUpdateAvailable = false,
                    errorMessage = null,
                    statusMessage = statusMsg,
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

            // Extract version code and version name from tag_name or release title
            val latestCode = extractVersionCode(tagName, title)
            val latestName = tagName.removePrefix("v").removePrefix("V").ifBlank { title }

            // Assets parsing: pick .apk asset or build fallback download url
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

            if (apkUrl == null && tagName.isNotBlank()) {
                apkUrl = "https://github.com/${current.repoPath}/releases/download/$tagName/app-release.apk"
                apkName = "app-release.apk"
            }

            // Parse Firebase Remote Config version code, name, and update_apk_url
            var rcCode = RemoteConfigHelper.latestVersionCode.value
            var rcName = RemoteConfigHelper.latestVersionName.value
            var rcApkUrl = RemoteConfigHelper.updateApkUrl.value
            try {
                val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
                val code = remoteConfig.getLong(RemoteConfigHelper.KEY_LATEST_VERSION_CODE).toInt()
                val name = remoteConfig.getString(RemoteConfigHelper.KEY_LATEST_VERSION_NAME)
                val url = remoteConfig.getString(RemoteConfigHelper.KEY_UPDATE_APK_URL)
                if (code > 0) rcCode = code
                if (name.isNotBlank()) rcName = name
                if (url.isNotBlank()) rcApkUrl = url
            } catch (e: Exception) {
                Log.w("AppUpdateManager", "RemoteConfig fetch check: ${e.message}")
            }

            if (rcCode <= 0 && rcName.isNotBlank()) {
                rcCode = extractVersionCode(rcName, rcName)
            }

            // Dual-Check Decision: Compare GitHub (latestCode) vs Firebase (rcCode)
            val finalLatestCode: Int
            val finalLatestName: String
            val finalApkUrl: String?

            if (rcCode > latestCode) {
                // Firebase has strictly greater version code
                finalLatestCode = rcCode
                finalLatestName = if (rcName.isNotBlank()) rcName else latestName
                finalApkUrl = if (rcApkUrl.isNotBlank()) rcApkUrl else apkUrl
            } else {
                // GitHub has equal or strictly greater version code
                finalLatestCode = latestCode
                finalLatestName = if (latestName.isNotBlank()) latestName else rcName
                finalApkUrl = if (!apkUrl.isNullOrBlank()) apkUrl else rcApkUrl.ifBlank { null }
            }

            val now = System.currentTimeMillis()
            val isUpdateAvailable = isVersionNewer(
                currentCode = current.currentVersionCode,
                currentName = current.currentVersionName,
                latestCode = finalLatestCode,
                latestName = finalLatestName
            )

            val statusMsg = if (isUpdateAvailable) {
                "🆕 नया App Update उपलब्ध है"
            } else {
                "आपका App नवीनतम Version पर है"
            }

            // Cache in SharedPreferences
            prefs.edit().apply {
                putLong("last_checked", now)
                putInt("latest_code", finalLatestCode)
                putString("latest_name", finalLatestName)
                putString("release_title", title)
                putString("release_notes", body)
                putString("published_at", publishedAt)
                putString("apk_url", finalApkUrl)
                putString("apk_name", apkName)
                putString("repo_path", current.repoPath)
                apply()
            }

            val newState = current.copy(
                isChecking = false,
                latestVersionCode = finalLatestCode,
                latestVersionName = finalLatestName,
                releaseTitle = title,
                releaseNotes = body,
                publishedAt = publishedAt,
                apkDownloadUrl = finalApkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                lastCheckedTimestamp = now,
                isUpdateAvailable = isUpdateAvailable,
                statusMessage = statusMsg,
                errorMessage = if (isUpdateAvailable && finalApkUrl == null) "APK डाउनलोड लिंक उपलब्ध नहीं है." else null
            )

            _updateState.value = newState
            return@withContext newState

        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Check for update failed", e)
            var rcCode = RemoteConfigHelper.latestVersionCode.value
            var rcName = RemoteConfigHelper.latestVersionName.value
            var rcApkUrl = RemoteConfigHelper.updateApkUrl.value
            try {
                val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
                val code = remoteConfig.getLong(RemoteConfigHelper.KEY_LATEST_VERSION_CODE).toInt()
                val name = remoteConfig.getString(RemoteConfigHelper.KEY_LATEST_VERSION_NAME)
                val url = remoteConfig.getString(RemoteConfigHelper.KEY_UPDATE_APK_URL)
                if (code > 0) rcCode = code
                if (name.isNotBlank()) rcName = name
                if (url.isNotBlank()) rcApkUrl = url
            } catch (rcErr: Exception) {
                Log.w("AppUpdateManager", "Fallback RemoteConfig check error: ${rcErr.message}")
            }

            if (rcCode <= 0 && rcName.isNotBlank()) {
                rcCode = extractVersionCode(rcName, rcName)
            }

            val isRcUpdateAvailable = isVersionNewer(
                currentCode = current.currentVersionCode,
                currentName = current.currentVersionName,
                latestCode = rcCode,
                latestName = rcName
            )

            val finalUrl = if (rcApkUrl.isNotBlank()) rcApkUrl else null

            val errState = current.copy(
                isChecking = false,
                latestVersionCode = if (rcCode > 0) rcCode else current.currentVersionCode,
                latestVersionName = if (rcName.isNotBlank()) rcName else current.currentVersionName,
                apkDownloadUrl = if (isRcUpdateAvailable) finalUrl else current.apkDownloadUrl,
                isUpdateAvailable = isRcUpdateAvailable,
                errorMessage = if (isRcUpdateAvailable && finalUrl == null) "Firebase Remote Config में APK URL उपलब्ध नहीं है." else if (!isRcUpdateAvailable) "जांच त्रुटि: ${e.localizedMessage ?: "Network error"}" else null,
                statusMessage = if (isRcUpdateAvailable) "🆕 नया App Update उपलब्ध है" else "अपडेट जांचने में त्रुटि हुई.",
                lastCheckedTimestamp = System.currentTimeMillis()
            )
            _updateState.value = errState
            return@withContext errState
        }
    }

    private fun extractVersionCode(tagName: String, title: String): Int {
        // 1. Check if tagName itself is a pure integer like "51" or "v51" (no dots)
        val cleanTag = tagName.trim().removePrefix("v").removePrefix("V").trim()
        if (cleanTag.isNotBlank() && !cleanTag.contains(".")) {
            val parsedTag = cleanTag.toIntOrNull()
            if (parsedTag != null && parsedTag > 0) return parsedTag
        }

        // 2. Check if title or tagName contains explicit build/code patterns like "build 51", "code 51", "b51", "(51)"
        val buildRegex = Regex("""(?i)(?:build|code|b|v)[\s:-]*(\d{1,6})\b""")
        val tagMatch = buildRegex.find(tagName)
        if (tagMatch != null && !tagName.contains(".")) {
            val code = tagMatch.groupValues[1].toIntOrNull()
            if (code != null && code > 0) return code
        }

        val titleMatch = buildRegex.find(title)
        if (titleMatch != null) {
            val code = titleMatch.groupValues[1].toIntOrNull()
            if (code != null && code > 0) return code
        }

        // 3. Check for parenthesized build code like "App v1.0 (51)"
        val parenRegex = Regex("""\((\d{1,6})\)""")
        val parenMatch = parenRegex.find(title) ?: parenRegex.find(tagName)
        if (parenMatch != null) {
            val code = parenMatch.groupValues[1].toIntOrNull()
            if (code != null && code > 0) return code
        }

        return 0
    }

    private fun isVersionNewer(currentCode: Int, currentName: String, latestCode: Int, latestName: String): Boolean {
        val cleanLatest = latestName.removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = currentName.removePrefix("v").removePrefix("V").trim()

        // 1. If clean version names are identical (e.g. "50" == "50" or "1.0.0" == "1.0.0")
        if (cleanLatest.isNotBlank() && cleanCurrent.isNotBlank() && cleanLatest.equals(cleanCurrent, ignoreCase = true)) {
            // Equal version names! Only newer if latestCode is strictly greater than currentCode
            if (latestCode > 0 && currentCode > 0) {
                return latestCode > currentCode
            }
            return false // Same version name and no higher build code -> NOT NEWER
        }

        // 2. If both have valid versionCode (>0) AND latestCode was explicitly extracted
        if (latestCode > 0 && currentCode > 0) {
            if (latestCode > currentCode) return true
            if (latestCode < currentCode) return false
            // If latestCode == currentCode, fall through to compare version names
        }

        // 3. Semantic Version comparison on version strings (e.g. "1.0.1" vs "1.0.0" or "51" vs "50")
        if (cleanLatest.isNotBlank() && cleanCurrent.isNotBlank()) {
            try {
                val latestParts = cleanLatest.split(".", "-", "_").mapNotNull { part ->
                    part.takeWhile { char -> char.isDigit() }.toIntOrNull()
                }
                val currentParts = cleanCurrent.split(".", "-", "_").mapNotNull { part ->
                    part.takeWhile { char -> char.isDigit() }.toIntOrNull()
                }

                if (latestParts.isNotEmpty() && currentParts.isNotEmpty()) {
                    val maxLen = maxOf(latestParts.size, currentParts.size)
                    for (i in 0 until maxLen) {
                        val l = latestParts.getOrElse(i) { 0 }
                        val c = currentParts.getOrElse(i) { 0 }
                        if (l > c) return true
                        if (l < c) return false
                    }
                    return false // Equal
                }
            } catch (e: Exception) {
                Log.w("AppUpdateManager", "Version string comparison error", e)
            }
        }

        return false
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

            // Follow HTTP redirects (GitHub Releases redirect to github-production-release-asset-2e65be.s3.amazonaws.com)
            var currentUrl = downloadUrl
            var redirectCount = 0
            var finalConnection: HttpURLConnection? = null

            while (redirectCount < 8) {
                val urlObj = URL(currentUrl)
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; VinayKumarAVJ-App/${BuildConfig.VERSION_CODE})")
                conn.connectTimeout = 20000
                conn.readTimeout = 60000
                conn.connect()

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 || code == 308) {
                    val newLocation = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = if (newLocation.startsWith("http://") || newLocation.startsWith("https://")) {
                            newLocation
                        } else {
                            URL(urlObj, newLocation).toString()
                        }
                        redirectCount++
                        continue
                    }
                }

                if (code in 200..299) {
                    finalConnection = conn
                    break
                } else {
                    conn.disconnect()
                    throw Exception("HTTP Server Response: $code")
                }
            }

            if (finalConnection == null) {
                throw Exception("Too many redirects or failed to connect to download server.")
            }

            downloadStreamToFile(finalConnection, targetFile, onProgress)
            finalConnection.disconnect()

            // Validate downloaded APK safely
            val validationError = validateApk(targetFile)
            if (validationError != null) {
                Log.w("AppUpdateManager", "APK validation warning: $validationError")
            }

            _updateState.value = _updateState.value.copy(
                isDownloading = false,
                downloadProgressPercentage = 100,
                downloadedApkFile = targetFile,
                errorMessage = null,
                statusMessage = "APK डाउनलोड पूर्ण! नीचे 'Install APK Now' बटन दबाएं या इंस्टॉलर खोलें।"
            )

            // Prompt install automatically on completion
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
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        _updateState.value = _updateState.value.copy(downloadProgressPercentage = progress)
                        onProgress(progress)
                    }
                }
                output.flush()
            }
        }
    }

    private fun validateApk(apkFile: File): String? {
        if (!apkFile.exists() || apkFile.length() < 1024L) {
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
            if (pkgInfo != null) {
                val apkPackage = pkgInfo.packageName
                if (!apkPackage.isNullOrBlank() && apkPackage != context.packageName) {
                    Log.w("AppUpdateManager", "Package name mismatch: $apkPackage vs ${context.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.w("AppUpdateManager", "Non-fatal APK parsing check: ${e.localizedMessage}")
        }

        return null // Don't block installation
    }

    fun installDownloadedApk() {
        val targetFile = _updateState.value.downloadedApkFile
        if (targetFile != null && targetFile.exists() && targetFile.length() > 0) {
            openInstaller(targetFile)
        } else {
            // Check if file exists in downloads directory
            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            val fileName = _updateState.value.apkFileName ?: "VinayKumarAVJ_Update.apk"
            val fallbackFile = File(downloadsDir, fileName)
            if (fallbackFile.exists() && fallbackFile.length() > 0) {
                _updateState.value = _updateState.value.copy(downloadedApkFile = fallbackFile)
                openInstaller(fallbackFile)
            } else {
                _updateState.value = _updateState.value.copy(
                    errorMessage = "डाउनलोड की गई APK फ़ाइल नहीं मिली। कृपया दोबारा डाउनलोड करें।"
                )
            }
        }
    }

    fun openInstaller(apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                _updateState.value = _updateState.value.copy(
                    errorMessage = "APK फ़ाइल नहीं मिली या अमान्य है।"
                )
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Grant URI permission explicitly to all matching handler packages
            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
