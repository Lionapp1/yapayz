package com.redex.pro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.redex.pro.ui.screens.*
import com.redex.pro.ui.theme.ReDexProTheme
import android.content.SharedPreferences
import com.redex.pro.ui.viewmodel.MainViewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.io.File

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    
    private lateinit var prefs: SharedPreferences
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences("redexpro_prefs", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("first_time", true)
        val hasPermissions = checkPermissionsGranted()
        
        setContent {
            ReDexProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel = remember { MainViewModel(application) }
                    val uiState by viewModel.uiState.collectAsState()
                    val currentApk by viewModel.currentApk.collectAsState()
                    val apkStructure by viewModel.apkStructure.collectAsState()
                    val dexFilesWithClasses by viewModel.dexFilesWithClasses.collectAsState()
                    val selectedFile by viewModel.selectedFile.collectAsState()
                    val selectedFileContent by viewModel.selectedFileContent.collectAsState()
                    
                    // Gelen intent ile APK açma
                    LaunchedEffect(intent) {
                        if (intent.action == Intent.ACTION_VIEW) {
                            intent.data?.let { uri ->
                                viewModel.openApkFromUri(uri)
                            }
                        }
                    }
                    
                    // Onboarding ve Permission kontrolü - sadece bir kez göster
                    var showWelcome by remember { mutableStateOf(isFirstTime) }
                    var showPermission by remember { mutableStateOf(false) }
                    var permissionChecked by remember { mutableStateOf(false) }
                    
                    // İzin kontrolünü bir kez yap
                    LaunchedEffect(Unit) {
                        if (!permissionChecked && !isFirstTime) {
                            permissionChecked = true
                            val currentHasPermissions = checkPermissionsGranted()
                            if (!currentHasPermissions) {
                                showPermission = true
                            }
                        }
                    }
                    
                    when {
                        showWelcome -> {
                            WelcomeScreen(
                                onFinish = {
                                    prefs.edit().putBoolean("first_time", false).apply()
                                    showWelcome = false
                                    // Welcome bitince izin kontrolü yap
                                    val currentHasPermissions = checkPermissionsGranted()
                                    if (!currentHasPermissions) {
                                        showPermission = true
                                    }
                                }
                            )
                        }
                        showPermission -> {
                            PermissionScreen(
                                onPermissionsGranted = {
                                    checkPermissions()
                                    showPermission = false
                                },
                                onSkip = {
                                    showPermission = false
                                }
                            )
                        }
                        else -> {
                            // Ana navigasyon - animasyonlu geçişler
                            AnimatedContent(
                                targetState = uiState,
                                transitionSpec = {
                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(300)
                                    ) with slideOutHorizontally(
                                        targetOffsetX = { -it },
                                        animationSpec = tween(300)
                                    )
                                },
                                label = "navigation"
                            ) { targetState ->
                                when (targetState) {
                        is MainViewModel.UiState.Home -> {
                            HomeScreen(viewModel = viewModel)
                        }
                        is MainViewModel.UiState.ApkDetail -> {
                            currentApk?.let { apk ->
                                ApkDetailScreen(
                                    apk = apk,
                                    onBack = { viewModel.goHome() },
                                    onViewDex = { viewModel.navigateTo(MainViewModel.UiState.DexViewer) },
                                    onViewArsc = { viewModel.navigateTo(MainViewModel.UiState.ArscViewer) },
                                    onViewManifest = { viewModel.navigateTo(MainViewModel.UiState.ManifestViewer) },
                                    onViewConverter = { viewModel.navigateTo(MainViewModel.UiState.Converter) },
                                    onOpenFileBrowser = { viewModel.openFileBrowser() },
                                    onOpenDexEditor = { viewModel.openDexEditor() }
                                )
                            }
                        }
                        is MainViewModel.UiState.FileBrowser -> {
                            apkStructure?.let { structure ->
                                FileBrowserScreen(
                                    structure = structure,
                                    onBack = { viewModel.navigateBack() },
                                    onFileClick = { file -> viewModel.openFile(file) },
                                    onDexEditorClick = { viewModel.openDexEditor() }
                                )
                            }
                        }
                        is MainViewModel.UiState.DexEditor -> {
                            DexEditorProScreen(
                                dexFiles = dexFilesWithClasses,
                                onBack = { viewModel.navigateBack() },
                                onClassClick = { dex, classInfo ->
                                    // Sınıf detayı için
                                },
                                onViewSmali = { dex, classInfo ->
                                    viewModel.openSmaliViewer(dex, classInfo)
                                }
                            )
                        }
                        is MainViewModel.UiState.SmaliViewer -> {
                            val selectedClass by viewModel.selectedClass.collectAsState()
                            val smaliContent by viewModel.smaliContent.collectAsState()
                            selectedClass?.let { classInfo ->
                                SmaliViewerScreen(
                                    className = classInfo.name,
                                    smaliCode = smaliContent,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                        }
                        is MainViewModel.UiState.TextEditor -> {
                            selectedFile?.let { file ->
                                val content = selectedFileContent?.let { 
                                    String(it, charset("UTF-8")) 
                                } ?: ""
                                TextEditorProScreen(
                                    fileEntry = file,
                                    content = content,
                                    onBack = { viewModel.navigateBack() },
                                    onSave = { newContent -> viewModel.saveFileChanges(newContent) }
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
                        is MainViewModel.UiState.Update -> {
                            UpdateScreen(
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                            }
                        }
                    }
                    
                    // Bottom Navigation - Ana ekranlarda göster
                    val showBottomNav = viewModel.canShowBottomNav()
                    if (showBottomNav) {
                        ReDexProBottomNavigation(
                            currentState = uiState,
                            onNavigate = { state ->
                                when (state) {
                                    is MainViewModel.UiState.Home -> viewModel.goHome()
                                    is MainViewModel.UiState.ApkDetail -> viewModel.navigateTo(MainViewModel.UiState.ApkDetail)
                                    is MainViewModel.UiState.FileBrowser -> viewModel.openFileBrowser()
                                    else -> {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    private fun checkPermissionsGranted(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        val needPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(needPermissions.toTypedArray())
        }
    }
}

@Composable
private fun ReDexProBottomNavigation(
    currentState: MainViewModel.UiState,
    onNavigate: (MainViewModel.UiState) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
            label = { Text("Ana Sayfa") },
            selected = currentState is MainViewModel.UiState.Home,
            onClick = { onNavigate(MainViewModel.UiState.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Info, contentDescription = "APK Detay") },
            label = { Text("Detay") },
            selected = currentState is MainViewModel.UiState.ApkDetail,
            onClick = { onNavigate(MainViewModel.UiState.ApkDetail) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Dosya Gezgini") },
            label = { Text("Dosyalar") },
            selected = currentState is MainViewModel.UiState.FileBrowser,
            onClick = { onNavigate(MainViewModel.UiState.FileBrowser) }
        )
    }
}
