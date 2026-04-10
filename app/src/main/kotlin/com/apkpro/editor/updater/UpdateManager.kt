package com.apkpro.editor.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(val percent: Int) : DownloadState()
    object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class UpdateManager(private val context: Context) {
    
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null
    
    companion object {
        private const val APK_FILE_NAME = "update.apk"
    }
    
    fun startDownload(downloadUrl: String) {
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("APKEditor Pro Güncelleme")
            setDescription("Yeni sürüm indiriliyor...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        
        _downloadState.value = DownloadState.Progress(0)
        
        // İndirme tamamlandığında APK kurulumu başlat
        registerDownloadReceiver()
    }
    
    private fun registerDownloadReceiver() {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk()
                    _downloadState.value = DownloadState.Completed
                    unregisterReceiver()
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }
    
    private fun unregisterReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Receiver zaten kayıtlı değilse
            }
            downloadReceiver = null
        }
    }
    
    fun installApk() {
        val apkFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_FILE_NAME
        )
        
        if (!apkFile.exists()) {
            _downloadState.value = DownloadState.Error("APK dosyası bulunamadı")
            return
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }
            
            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        
        context.startActivity(intent)
    }
    
    fun cancelDownload() {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
        }
        unregisterReceiver()
        _downloadState.value = DownloadState.Idle
    }
    
    suspend fun downloadAndInstallDirect(downloadUrl: String) = withContext(Dispatchers.IO) {
        try {
            _downloadState.value = DownloadState.Progress(0)
            
            val apkFile = File(context.getExternalFilesDir(null), APK_FILE_NAME)
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 30000
                readTimeout = 30000
                setRequestProperty("Accept", "*/*")
            }
            
            val totalSize = connection.contentLength
            var downloadedSize = 0
            
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        
                        if (totalSize > 0) {
                            val progress = (downloadedSize * 100 / totalSize)
                            _downloadState.value = DownloadState.Progress(progress)
                        }
                    }
                }
            }
            
            _downloadState.value = DownloadState.Completed
            
            // Kurulumu başlat
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            apkFile
                        )
                    } else {
                        Uri.fromFile(apkFile)
                    }
                    
                    setDataAndType(uri, "application/vnd.android.package-archive")
                }
                context.startActivity(intent)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "İndirme başarısız")
            Result.failure(e)
        }
    }
}
