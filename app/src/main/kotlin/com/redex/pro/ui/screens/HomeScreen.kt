package com.redex.pro.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ApkInfo
import com.redex.pro.ui.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentApk by viewModel.currentApk.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val recentFiles by viewModel.recentFiles.collectAsState()
    
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.openApkFromUri(it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ReDex Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.openUpdateScreen() }) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Güncelleme", tint = Color.White)
                    }
                    IconButton(onClick = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "APK Aç", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.goHome() }) {
                        Icon(Icons.Default.Home, contentDescription = "Ana Sayfa", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is MainViewModel.UiState.Home -> {
                HomeContent(
                    recentFiles = recentFiles,
                    onSelectApk = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) },
                    onRecentClick = { viewModel.openApkFromPath(it.path) },
                    onUpdateClick = { viewModel.openUpdateScreen() },
                    modifier = Modifier.padding(padding)
                )
            }
            is MainViewModel.UiState.ApkDetail -> {
                currentApk?.let { apk ->
                    ApkDetailScreen(
                        apk = apk,
                        onBack = { viewModel.navigateBack() },
                        onViewDex = { viewModel.navigateTo(MainViewModel.UiState.DexViewer) },
                        onViewArsc = { viewModel.navigateTo(MainViewModel.UiState.ArscViewer) },
                        onViewManifest = { viewModel.navigateTo(MainViewModel.UiState.ManifestViewer) },
                        onViewConverter = { viewModel.navigateTo(MainViewModel.UiState.Converter) },
                        onOpenFileBrowser = { viewModel.openFileBrowser() },
                        onOpenDexEditor = { viewModel.openDexEditor() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MainViewModel.UiState.DexViewer -> {
                currentApk?.let { apk ->
                    DexViewerScreen(
                        dexFiles = apk.dexFiles,
                        onBack = { viewModel.navigateBack() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MainViewModel.UiState.ArscViewer -> {
                currentApk?.let { apk ->
                    ArscViewerScreen(
                        resources = apk.resources,
                        onBack = { viewModel.navigateBack() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MainViewModel.UiState.ManifestViewer -> {
                currentApk?.let { apk ->
                    ManifestViewerScreen(
                        manifest = apk.manifest,
                        onBack = { viewModel.navigateBack() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MainViewModel.UiState.Converter -> {
                currentApk?.let { apk ->
                    ConverterScreen(
                        apk = apk,
                        onBack = { viewModel.navigateBack() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MainViewModel.UiState.FileBrowser -> {
                // Handled in MainActivity
            }
            is MainViewModel.UiState.Update -> {
                UpdateScreen(
                    onNavigateBack = { viewModel.navigateBack() }
                )
            }
            is MainViewModel.UiState.DexEditor -> {
                // Handled in MainActivity
            }
            is MainViewModel.UiState.TextEditor -> {
                // Handled in MainActivity
            }
            else -> {
                // Unknown state - go home
                HomeContent(
                    recentFiles = recentFiles,
                    onSelectApk = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) },
                    onRecentClick = { viewModel.openApkFromPath(it.path) },
                    onUpdateClick = { viewModel.openUpdateScreen() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
        
        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        
        // Error
        error?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Hata") },
                text = { Text(errorMsg) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Tamam")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeContent(
    recentFiles: List<ApkInfo>,
    onSelectApk: () -> Unit,
    onRecentClick: (ApkInfo) -> Unit,
    onUpdateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Hoşgeldiniz kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ReDex Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Profesyonel APK & DEX Editör",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Button(
                    onClick = onSelectApk,
                    modifier = Modifier.padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("APK Dosyası Seç")
                }
            }
        }
        
        // Özellikler
        Text(
            "Özellikler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureItem(Icons.Default.Code, "DEX Görüntüle", "Smali/Java")
            FeatureItem(Icons.Default.Settings, "ARSC Parser", "Kaynaklar")
            FeatureItem(Icons.Default.Transform, "Dönüştürücü", "APK/AAB")
        }
        
        // Güncelleme kartı
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onUpdateClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Güncelleme Kontrolü",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Yeni sürüm varsa hemen indir",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
        
        // Son dosyalar
        if (recentFiles.isNotEmpty()) {
            Text(
                "Son Dosyalar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn {
                items(recentFiles) { apk ->
                    RecentFileItem(
                        apk = apk,
                        onClick = { onRecentClick(apk) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun RecentFileItem(apk: ApkInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    apk.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "${apk.packageName} • ${formatFileSize(apk.size)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
