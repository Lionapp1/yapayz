package com.redex.pro.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ApkInfo
import com.redex.pro.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
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
            Surface(
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Android,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "ReDex Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.openUpdateScreen() }) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = "Güncelleme", tint = Color.White)
                        }
                        IconButton(onClick = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "APK Aç", tint = Color.White)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                is MainViewModel.UiState.Home -> {
                    ModernHomeContent(
                        recentFiles = recentFiles,
                        onSelectApk = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) },
                        onRecentClick = { viewModel.openApkFromPath(it.path) },
                        onUpdateClick = { viewModel.openUpdateScreen() }
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
                            onChangePackage = { viewModel.changePackageName(it) }
                        )
                    }
                }
                is MainViewModel.UiState.DexViewer -> {
                    currentApk?.let { apk ->
                        DexViewerScreen(
                            dexFiles = apk.dexFiles,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
                is MainViewModel.UiState.ArscViewer -> {
                    currentApk?.let { apk ->
                        ArscViewerScreen(
                            resources = apk.resources,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
                is MainViewModel.UiState.ManifestViewer -> {
                    currentApk?.let { apk ->
                        ManifestViewerScreen(
                            manifest = apk.manifest,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
                is MainViewModel.UiState.Converter -> {
                    currentApk?.let { apk ->
                        ConverterScreen(
                            apk = apk,
                            onBack = { viewModel.navigateBack() }
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
                    ModernHomeContent(
                        recentFiles = recentFiles,
                        onSelectApk = { filePicker.launch(arrayOf("application/vnd.android.package-archive")) },
                        onRecentClick = { viewModel.openApkFromPath(it.path) },
                        onUpdateClick = { viewModel.openUpdateScreen() }
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Yükleniyor...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            error?.let { errorMsg ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Hata") },
                    text = { Text(errorMsg) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Tamam")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernHomeContent(
    recentFiles: List<ApkInfo>,
    onSelectApk: () -> Unit,
    onRecentClick: (ApkInfo) -> Unit,
    onUpdateClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroSection(onSelectApk = onSelectApk)
        }
        
        item {
            Text(
                "Özellikler",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }
        
        item {
            ModernFeatureGrid()
        }
        
        item {
            ModernUpdateCard(onUpdateClick = onUpdateClick)
        }
        
        if (recentFiles.isNotEmpty()) {
            item {
                Text(
                    "Son Dosyalar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
            
            items(recentFiles.take(10), key = { it.path }) { apk ->
                ModernRecentFileItem(
                    apk = apk,
                    onClick = { onRecentClick(apk) }
                )
            }
        }
        
        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeroSection(onSelectApk: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "ReDex Pro",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Profesyonel APK & DEX Editör",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = onSelectApk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "APK Dosyası Seç",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernFeatureGrid() {
    val features = listOf(
        FeatureData(Icons.Default.Code, "DEX", "Smali/Java", MaterialTheme.colorScheme.primary),
        FeatureData(Icons.Default.Storage, "ARSC", "Kaynaklar", MaterialTheme.colorScheme.secondary),
        FeatureData(Icons.Default.Transform, "Dönüştür", "APK/AAB", MaterialTheme.colorScheme.tertiary),
        FeatureData(Icons.Default.Edit, "Düzenle", "XML/Manifest", MaterialTheme.colorScheme.primary),
        FeatureData(Icons.Default.Image, "İkon", "Resimler", MaterialTheme.colorScheme.secondary),
        FeatureData(Icons.Default.Inventory, "Paket", "Ad Değiştir", MaterialTheme.colorScheme.tertiary)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.chunked(3).forEach { rowFeatures ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowFeatures.forEach { feature ->
                    ModernFeatureCard(
                        feature = feature,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowFeatures.size < 3) {
                    repeat(3 - rowFeatures.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernFeatureCard(feature: FeatureData, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .height(110.dp)
            .scale(scale)
            .clickable(
                onClick = { /* Feature click */ },
                interactionSource = interactionSource,
                indication = null
            ),
        colors = CardDefaults.cardColors(
            containerColor = feature.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                tint = feature.color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                feature.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = feature.color
            )
            Text(
                feature.desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModernUpdateCard(onUpdateClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onUpdateClick,
                interactionSource = interactionSource,
                indication = null
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Güncelleme Kontrolü",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Yeni sürüm varsa hemen indir",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ModernRecentFileItem(apk: ApkInfo, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        apk.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        apk.packageName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                    Text(
                        "${formatFileSize(apk.size)} • v${apk.versionName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class FeatureData(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val color: Color
)

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
