package com.apkpro.editor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apkpro.editor.updater.AppUpdater
import com.apkpro.editor.updater.DownloadState
import com.apkpro.editor.updater.UpdateInfo
import com.apkpro.editor.updater.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val appUpdater = remember { AppUpdater(context) }
    val updateManager = remember { UpdateManager(context) }
    
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val downloadState by updateManager.downloadState.collectAsState()
    
    // İlk açılışta güncelleme kontrolü
    LaunchedEffect(Unit) {
        appUpdater.checkForUpdate().fold(
            onSuccess = { info ->
                updateInfo = info
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = error.message ?: "Güncelleme kontrolü başarısız"
                isLoading = false
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Güncelleme") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Güncelleme kontrol ediliyor...")
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                appUpdater.checkForUpdate().fold(
                                    onSuccess = { info ->
                                        updateInfo = info
                                        isLoading = false
                                    },
                                    onFailure = { error ->
                                        errorMessage = error.message
                                        isLoading = false
                                    }
                                )
                            }
                        }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tekrar Dene")
                        }
                    }
                }
                updateInfo != null -> {
                    UpdateContent(
                        updateInfo = updateInfo!!,
                        currentVersion = appUpdater.getCurrentVersionName(),
                        downloadState = downloadState,
                        onDownload = {
                            scope.launch {
                                updateManager.downloadAndInstallDirect(updateInfo!!.downloadUrl)
                            }
                        },
                        onCancel = {
                            updateManager.cancelDownload()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateContent(
    updateInfo: UpdateInfo,
    currentVersion: String,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // İkon
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Başlık
        Text(
            text = if (updateInfo.isUpdateAvailable) 
                "Yeni Sürüm Mevcut!" 
            else 
                "En Güncel Sürüm",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sürüm bilgileri
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (updateInfo.isUpdateAvailable)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                VersionInfoRow(
                    label = "Mevcut Sürüm",
                    value = currentVersion,
                    icon = Icons.Default.PhoneAndroid
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                VersionInfoRow(
                    label = "Yeni Sürüm",
                    value = updateInfo.versionName,
                    icon = Icons.Default.NewReleases,
                    isHighlighted = updateInfo.isUpdateAvailable
                )
                if (updateInfo.publishedAt.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    VersionInfoRow(
                        label = "Yayınlanma",
                        value = updateInfo.publishedAt.substringBefore("T"),
                        icon = Icons.Default.CalendarToday
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sürüm notları
        if (updateInfo.releaseNotes.isNotBlank()) {
            Text(
                text = "Sürüm Notları",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = updateInfo.releaseNotes,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // İndirme durumu
        AnimatedVisibility(visible = downloadState is DownloadState.Progress) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val progress = (downloadState as? DownloadState.Progress)?.percent ?: 0
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("İndiriliyor... %$progress")
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        AnimatedVisibility(visible = downloadState is DownloadState.Error) {
            val error = (downloadState as? DownloadState.Error)?.message ?: "Bilinmeyen hata"
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Butonlar
        when (downloadState) {
            is DownloadState.Progress -> {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cancel, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("İptal")
                }
            }
            is DownloadState.Completed -> {
                Button(
                    onClick = { /* Kurulum başlatıldı */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kurulum Başlatıldı")
                }
            }
            else -> {
                if (updateInfo.isUpdateAvailable) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Güncelle")
                    }
                } else {
                    OutlinedButton(
                        onClick = { /* Zaten güncel */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Zaten En Güncel Sürüm")
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlighted: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlighted) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isHighlighted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Basit AlertDialog versiyonu (daha küçük kullanım için)
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, null) },
        title = { Text("Güncelleme Mevcut") },
        text = {
            Column {
                Text("Yeni sürüm ${updateInfo.versionName} yayınlandı!")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = updateInfo.releaseNotes.take(200) + if (updateInfo.releaseNotes.length > 200) "..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Güncelle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Sonra")
            }
        }
    )
}
