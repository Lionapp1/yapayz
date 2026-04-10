package com.apkpro.editor

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
import com.apkpro.editor.ui.screens.*
import com.apkpro.editor.ui.theme.ReDexProTheme
import android.content.SharedPreferences
import com.apkpro.editor.ui.viewmodel.MainViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.io.File

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
                    
                    // Onboarding ve Permission kontrolü
                    var showWelcome by remember { mutableStateOf(isFirstTime) }
                    var showPermission by remember { mutableStateOf(!hasPermissions && !isFirstTime) }
                    
                    when {
                        showWelcome -> {
                            WelcomeScreen(
                                onFinish = {
                                    prefs.edit().putBoolean("first_time", false).apply()
                                    showWelcome = false
                                    if (!hasPermissions) {
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
                            // Ana navigasyon
                            when (uiState) {
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
                            DexEditorPlusScreen(
                                dexFiles = dexFilesWithClasses,
                                onBack = { viewModel.navigateBack() },
                                onClassClick = { dex, classInfo ->
                                    // TODO: Smali görüntüleyici aç
                                }
                            )
                        }
                        is MainViewModel.UiState.TextEditor -> {
                            selectedFile?.let { file ->
                                val content = selectedFileContent?.let { 
                                    String(it, charset("UTF-8")) 
                                } ?: ""
                                TextEditorScreen(
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
