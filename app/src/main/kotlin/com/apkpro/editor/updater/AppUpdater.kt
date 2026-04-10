package com.apkpro.editor.updater

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String? = null,
    val published_at: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long
)

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String,
    val isUpdateAvailable: Boolean
)

class AppUpdater(private val context: Context) {
    
    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/Lionapp1/yapayz/releases/latest"
        private const val GITHUB_REPO = "Lionapp1/yapayz"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun checkForUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val release = json.decodeFromString<GitHubRelease>(response)
                
                // APK asset'ini bul
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    ?: return@withContext Result.failure(Exception("APK dosyası bulunamadı"))
                
                // Sürüm kodunu tag'den çıkar (v1.0.42 -> 42)
                val versionCode = release.tag_name
                    .removePrefix("v")
                    .split(".")
                    .lastOrNull()
                    ?.toIntOrNull() ?: 0
                
                val currentVersionCode = getCurrentVersionCode()
                
                val updateInfo = UpdateInfo(
                    versionName = release.tag_name.removePrefix("v"),
                    versionCode = versionCode,
                    downloadUrl = apkAsset.browser_download_url,
                    releaseNotes = release.body ?: "Yeni sürüm mevcut!",
                    publishedAt = release.published_at,
                    isUpdateAvailable = versionCode > currentVersionCode
                )
                
                Result.success(updateInfo)
            } else {
                Result.failure(Exception("GitHub API hatası: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.longVersionCode.toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
