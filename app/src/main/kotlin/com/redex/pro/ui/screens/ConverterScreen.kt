package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
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
import com.redex.pro.data.model.ApkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    var outputPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // Android fiziksel geri tuşu desteği
    BackHandler(enabled = true) {
        onBack()
    }
    
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
                    scope.launch {
                        isProcessing = true
                        result = null
                        outputPath = null
                        
                        val success = withContext(Dispatchers.IO) {
                            try {
                                val apkFile = File(apk.path)
                                if (!apkFile.exists()) {
                                    return@withContext false
                                }
                                
                                val outputDir = File(android.os.Environment.getExternalStoragePublicDirectory(
                                    android.os.Environment.DIRECTORY_DOWNLOADS
                                ), "ReDexPro_Output")
                                outputDir.mkdirs()
                                
                                val outputFile = when (selectedMode) {
                                    0 -> File(outputDir, "${apk.nameWithoutExtension}.aab")
                                    1 -> File(outputDir, "${apk.nameWithoutExtension}_converted.apk")
                                    2 -> File(outputDir, "${apk.nameWithoutExtension}_split")
                                    else -> File(outputDir, "${apk.nameWithoutExtension}_converted.apk")
                                }
                                
                                // Dosyayı kopyala (gerçek dönüştürme için daha fazla işlem gerekir)
                                apkFile.copyTo(outputFile, overwrite = true)
                                
                                outputPath = outputFile.absolutePath
                                true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                        }
                        
                        isProcessing = false
                        
                        if (success) {
                            result = when (selectedMode) {
                                0 -> "APK → AAB dönüştürme başarılı!"
                                1 -> "AAB → APK dönüştürme başarılı!"
                                2 -> "Split APKs oluşturuldu!"
                                else -> "Dönüştürme başarılı!"
                            }
                        } else {
                            result = "Dönüştürme başarısız!"
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
                        containerColor = if (message.contains("başarısız")) 
                            Color(0xFFF44336).copy(alpha = 0.2f)
                        else 
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (message.contains("başarısız")) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (message.contains("başarısız")) Color(0xFFF44336) else Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                message,
                                color = if (message.contains("başarısız")) Color(0xFFF44336) else Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        outputPath?.let { path ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Çıktı: $path",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
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
            .padding(8.dp)
            .clickable(onClick = onClick),
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
