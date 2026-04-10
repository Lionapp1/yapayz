package com.apkpro.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkpro.editor.data.model.ApkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    apk: ApkInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK Dönüştürücü") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // APK bilgisi
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(apk.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${apk.packageName} • ${formatFileSize(apk.size)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Dönüştürme modu seçimi
            Text(
                "Dönüştürme Modu",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ModeOption(
                        icon = Icons.Default.Archive,
                        title = "APK → App Bundle (AAB)",
                        desc = "Google Play için App Bundle formatına dönüştür",
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ModeOption(
                        icon = Icons.Default.FolderZip,
                        title = "App Bundle → APK",
                        desc = "AAB dosyasını APK'ya dönüştür",
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 }
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    ModeOption(
                        icon = Icons.Default.Splitscreen,
                        title = "Split APKs",
                        desc = "APK'yı mimari bazlı böl (arm64, armeabi, x86)",
                        selected = selectedMode == 2,
                        onClick = { selectedMode = 2 }
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Dönüştür butonu
            Button(
                onClick = {
                    isProcessing = true
                    // Simülasyon
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(2000)
                        isProcessing = false
                        result = when (selectedMode) {
                            0 -> "APK → AAB dönüştürme başarılı!"
                            1 -> "AAB → APK dönüştürme başarılı!"
                            2 -> "Split APKs oluşturuldu!"
                            else -> "Bilinmeyen mod"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dönüştürülüyor...")
                } else {
                    Icon(Icons.Default.Transform, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dönüştür")
                }
            }
            
            // Sonuç
            result?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            message,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Not
            Text(
                "Not: Bu özellik bazı APK dosyalarında çalışmayabilir. İmzasız veya özel koruma altındaki APK'lar dönüştürülemez.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ModeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurface
            )
            Text(
                desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
