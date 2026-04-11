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
                        onEditIcon = { },
                        onChangeName = { viewModel.changeApkName(it) },
                        onChangePackage = { viewModel.changePackageName(it) },
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
            }
            is MainViewModel.UiState.Update -> {
                UpdateScreen(
                    onNavigateBack = { viewModel.navigateBack() }
                )
            }
            is MainViewModel.UiState.DexEditor -> {
            }
            is MainViewModel.UiState.TextEditor -> {
            }
            else -> {
                HomeContent(
                    recentFiles = recentFiles,
                    onSelectApk = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) },
                    onRecentClick = { viewModel.openApkFromPath(it.path) },
                    onUpdateClick = { viewModel.openUpdateScreen() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
        
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Button(
                    onClick = onSelectApk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("APK Dosyası Seç")
                }
            }
        }
        
        Text(
            "Özellikler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureCard(Icons.Default.Code, "DEX", "Smali/Java", Modifier.weight(1f))
            FeatureCard(Icons.Default.Settings, "ARSC", "Kaynaklar", Modifier.weight(1f))
            FeatureCard(Icons.Default.Transform, "Dönüş", "APK/AAB", Modifier.weight(1f))
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureCard(Icons.Default.Edit, "Düzenle", "XML/Manifest", Modifier.weight(1f))
            FeatureCard(Icons.Default.Image, "İkon", "Resimler", Modifier.weight(1f))
            FeatureCard(Icons.Default.Package, "Paket", "Ad/Değiş", Modifier.weight(1f))
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onUpdateClick() },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
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
        
        if (recentFiles.isNotEmpty()) {
            Text(
                "Son Dosyalar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
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
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
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
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    apk.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 14.sp
                )
                Text(
                    "${apk.packageName} • ${formatFileSize(apk.size)}",
                    fontSize = 11.sp,
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
