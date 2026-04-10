package com.redex.pro.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redex.pro.data.model.*
import com.redex.pro.data.parser.ApkParser
import com.redex.pro.data.editor.ApkEditor
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Home)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Navigasyon geçmişi - geri tuşu için
    private val _navigationHistory = mutableListOf<UiState>()
    private val maxHistorySize = 10
    
    private val _currentApk = MutableStateFlow<ApkInfo?>(null)
    val currentApk: StateFlow<ApkInfo?> = _currentApk.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _recentFiles = MutableStateFlow<List<ApkInfo>>(emptyList())
    val recentFiles: StateFlow<List<ApkInfo>> = _recentFiles.asStateFlow()
    
    private val apkParser = ApkParser(application)
    private val apkEditor = ApkEditor(application)
    
    // Dosya Gezgini verileri
    private val _apkStructure = MutableStateFlow<ApkStructure?>(null)
    val apkStructure: StateFlow<ApkStructure?> = _apkStructure.asStateFlow()
    
    // DEX Editör verileri
    private val _dexFilesWithClasses = MutableStateFlow<List<Pair<ApkFileEntry, List<ClassInfo>>>>(emptyList())
    val dexFilesWithClasses: StateFlow<List<Pair<ApkFileEntry, List<ClassInfo>>>> = _dexFilesWithClasses.asStateFlow()
    
    // Seçili dosya
    private val _selectedFile = MutableStateFlow<ApkFileEntry?>(null)
    val selectedFile: StateFlow<ApkFileEntry?> = _selectedFile.asStateFlow()
    
    // Dosya içeriği
    private val _selectedFileContent = MutableStateFlow<ByteArray?>(null)
    val selectedFileContent: StateFlow<ByteArray?> = _selectedFileContent.asStateFlow()
    
    // Smali görüntüleyici verileri
    private val _selectedClass = MutableStateFlow<ClassInfo?>(null)
    val selectedClass: StateFlow<ClassInfo?> = _selectedClass.asStateFlow()
    
    private val _smaliContent = MutableStateFlow<String>("")
    val smaliContent: StateFlow<String> = _smaliContent.asStateFlow()
    
    fun openApkFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val apkInfo = apkParser.parseApk(uri)
                _currentApk.value = apkInfo
                _uiState.value = UiState.ApkDetail
                addToRecent(apkInfo)
            } catch (e: Exception) {
                _error.value = "APK açılırken hata: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun openApkFromPath(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val file = File(path)
                if (!file.exists()) {
                    throw Exception("Dosya bulunamadı: $path")
                }
                
                val apkInfo = apkParser.parseApkFile(file)
                _currentApk.value = apkInfo
                _uiState.value = UiState.ApkDetail
                addToRecent(apkInfo)
            } catch (e: Exception) {
                _error.value = "APK açılırken hata: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun navigateTo(state: UiState) {
        // Mevcut state'i geçmişe ekle
        val currentState = _uiState.value
        if (currentState != state && currentState !is UiState.Home) {
            _navigationHistory.add(currentState)
            if (_navigationHistory.size > maxHistorySize) {
                _navigationHistory.removeAt(0)
            }
        }
        _uiState.value = state
    }
    
    fun navigateBack() {
        // Geçmişten geri al
        if (_navigationHistory.isNotEmpty()) {
            val previousState = _navigationHistory.removeAt(_navigationHistory.size - 1)
            _uiState.value = previousState
        } else {
            // Geçmiş boşsa default davranış
            _uiState.value = when (_uiState.value) {
                is UiState.ApkDetail -> UiState.Home
                is UiState.DexViewer -> UiState.ApkDetail
                is UiState.ArscViewer -> UiState.ApkDetail
                is UiState.ManifestViewer -> UiState.ApkDetail
                is UiState.Converter -> UiState.ApkDetail
                is UiState.FileBrowser -> UiState.ApkDetail
                is UiState.DexEditor -> UiState.FileBrowser
                is UiState.TextEditor -> UiState.FileBrowser
                is UiState.SmaliViewer -> UiState.DexEditor
                is UiState.Update -> UiState.Home
                else -> UiState.Home
            }
        }
    }
    
    fun openSmaliViewer(dexFile: ApkFileEntry, classInfo: ClassInfo) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Smali kodunu üret
                val smaliCode = apkParser.generateSmaliCode(File(_currentApk.value?.path ?: return@launch), dexFile, classInfo)
                _selectedClass.value = classInfo
                _smaliContent.value = smaliCode
                navigateTo(UiState.SmaliViewer)
            } catch (e: Exception) {
                _error.value = "Smali kodu oluşturulurken hata: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun goHome() {
        _uiState.value = UiState.Home
        _currentApk.value = null
        _navigationHistory.clear() // Geçmişi temizle
    }
    
    fun clearError() {
        _error.value = null
    }
    
    private fun addToRecent(apkInfo: ApkInfo) {
        val current = _recentFiles.value.toMutableList()
        current.removeAll { it.path == apkInfo.path }
        current.add(0, apkInfo)
        if (current.size > 10) current.removeLast()
        _recentFiles.value = current
    }
    
    // ========== NAVİGASYON FONKSİYONLARI ==========
    
    fun openFileBrowser() {
        _currentApk.value?.let { apk ->
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val structure = apkParser.getApkStructure(File(apk.path))
                    _apkStructure.value = structure
                    _dexFilesWithClasses.value = apkParser.getAllDexFilesWithClasses(File(apk.path))
                    _uiState.value = UiState.FileBrowser
                } catch (e: Exception) {
                    _error.value = "Dosya gezgini açılırken hata: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun openDexEditor() {
        _currentApk.value?.let { apk ->
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val dexFiles = apkParser.getAllDexFilesWithClasses(File(apk.path))
                    _dexFilesWithClasses.value = dexFiles
                    _uiState.value = UiState.DexEditor
                } catch (e: Exception) {
                    _error.value = "DEX editör açılırken hata: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun openFile(fileEntry: ApkFileEntry) {
        _currentApk.value?.let { apk ->
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val content = apkParser.extractFileFromApk(File(apk.path), fileEntry.path)
                    _selectedFile.value = fileEntry
                    _selectedFileContent.value = content
                    _uiState.value = UiState.TextEditor
                } catch (e: Exception) {
                    _error.value = "Dosya açılırken hata: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun saveFileChanges(newContent: String) {
        viewModelScope.launch {
            _currentApk.value?.let { apk ->
                _selectedFile.value?.let { file ->
                    _isLoading.value = true
                    try {
                        val outputFile = File(apk.path + "_modified.apk")
                        val success = apkEditor.editXmlFile(
                            File(apk.path),
                            file,
                            newContent,
                            outputFile
                        )
                        if (success) {
                            _error.value = "Değişiklikler kaydedildi: ${outputFile.name}"
                        } else {
                            _error.value = "Kaydetme başarısız"
                        }
                    } catch (e: Exception) {
                        _error.value = "Kaydetme hatası: ${e.message}"
                    } finally {
                        _isLoading.value = false
                    }
                }
            }
        }
    }
    
    sealed class UiState {
        object Home : UiState()
        object ApkDetail : UiState()
        object DexViewer : UiState()
        object ArscViewer : UiState()
        object ManifestViewer : UiState()
        object Converter : UiState()
        object FileBrowser : UiState()
        object DexEditor : UiState()
        object TextEditor : UiState()
        object SmaliViewer : UiState()     // Yeni: Smali kod görüntüleyici
        object Update : UiState()          // Yeni: Güncelleme ekranı
    }
    
    fun openUpdateScreen() {
        navigateTo(UiState.Update)
    }
    
    // Bottom navigation için ana ekranlar
    fun canShowBottomNav(): Boolean {
        return when (_uiState.value) {
            is UiState.Home, 
            is UiState.ApkDetail, 
            is UiState.FileBrowser -> true
            else -> false
        }
    }
}
