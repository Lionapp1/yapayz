package com.apkpro.editor.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apkpro.editor.data.model.ApkInfo
import com.apkpro.editor.data.parser.ApkParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Home)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _currentApk = MutableStateFlow<ApkInfo?>(null)
    val currentApk: StateFlow<ApkInfo?> = _currentApk.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _recentFiles = MutableStateFlow<List<ApkInfo>>(emptyList())
    val recentFiles: StateFlow<List<ApkInfo>> = _recentFiles.asStateFlow()
    
    private val apkParser = ApkParser(application)
    
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
    
    fun navigateToDexViewer() {
        _uiState.value = UiState.DexViewer
    }
    
    fun navigateToArscViewer() {
        _uiState.value = UiState.ArscViewer
    }
    
    fun navigateToManifestViewer() {
        _uiState.value = UiState.ManifestViewer
    }
    
    fun navigateToConverter() {
        _uiState.value = UiState.Converter
    }
    
    fun navigateBack() {
        _uiState.value = when (_uiState.value) {
            is UiState.ApkDetail -> UiState.Home
            is UiState.DexViewer -> UiState.ApkDetail
            is UiState.ArscViewer -> UiState.ApkDetail
            is UiState.ManifestViewer -> UiState.ApkDetail
            is UiState.Converter -> UiState.ApkDetail
            else -> UiState.Home
        }
    }
    
    fun goHome() {
        _uiState.value = UiState.Home
        _currentApk.value = null
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
    
    sealed class UiState {
        object Home : UiState()
        object ApkDetail : UiState()
        object DexViewer : UiState()
        object ArscViewer : UiState()
        object ManifestViewer : UiState()
        object Converter : UiState()
    }
}
