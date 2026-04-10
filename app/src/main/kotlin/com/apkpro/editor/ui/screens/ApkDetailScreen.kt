package com.apkpro.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkpro.editor.data.model.ApkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkDetailScreen(
    apk: ApkInfo,
    onBack: () -> Unit,
    onViewDex: () -> Unit,
    onViewArsc: () -> Unit,
    onViewManifest: () -> Unit,
    onViewConverter: () -> Unit,
    onOpenFileBrowser: () -> Unit = {},
    onOpenDexEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        apk.name,
                        maxLines = 1,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onOpenFileBrowser) {
                        Icon(Icons.Default.Folder, contentDescription = "Dosya Gezgini", tint = Color.White)
                    }
                    IconButton(onClick = onOpenDexEditor) {
                        Icon(Icons.Default.Edit, contentDescription = "DEX Editör", tint = Color.White)
                    }
                    IconButton(onClick = onViewConverter) {
                        Icon(Icons.Default.Transform, contentDescription = "Dönüştür", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Temel bilgiler
            item {
                InfoCard(title = "Uygulama Bilgileri") {
                    InfoRow("Paket Adı", apk.packageName)
                    InfoRow("Versiyon", "${apk.versionName} (${apk.versionCode})")
                    InfoRow("Boyut", formatFileSize(apk.size))
                    InfoRow("Min SDK", "Android ${apk.minSdk}")
                    InfoRow("Target SDK", "Android ${apk.targetSdk}")
                }
            }
            
            // İmza bilgisi
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (apk.isSigned) 
                            Color(0xFF4CAF50).copy(alpha = 0.2f) 
                        else 
                            Color(0xFFFFA726).copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (apk.isSigned) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (apk.isSigned) Color(0xFF4CAF50) else Color(0xFFFFA726)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (apk.isSigned) "APK İmzalı" else "İmza Yok!",
                            color = if (apk.isSigned) Color(0xFF4CAF50) else Color(0xFFFFA726),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Hash bilgileri
            item {
                InfoCard(title = "Hash Değerleri") {
                    InfoRow("MD5", apk.md5.take(16) + "...")
                    InfoRow("SHA-1", apk.sha1.take(20) + "...")
                    InfoRow("SHA-256", apk.sha256.take(24) + "...")
                }
            }
            
            // DEX dosyaları
            if (apk.dexFiles.isNotEmpty()) {
                item {
                    Text(
                        "DEX Dosyaları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(apk.dexFiles) { dex ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        onClick = onViewDex
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                dex.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${dex.classCount} sınıf • ${dex.methodCount} metod • ${dex.fieldCount} alan",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            // İzinler
            if (apk.permissions.isNotEmpty()) {
                item {
                    Text(
                        "İzinler (${apk.permissions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(apk.permissions.take(10)) { permission ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            permission.substringAfterLast("."),
                            modifier = Modifier.padding(12.dp),
                            fontSize = 13.sp
                        )
                    }
                }
                
                if (apk.permissions.size > 10) {
                    item {
                        Text(
                            "+${apk.permissions.size - 10} izin daha...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Kaynaklar
            item {
                Button(
                    onClick = onViewArsc,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kaynakları Görüntüle (${apk.resources.stringCount} string)")
                }
            }
            
            // Manifest
            item {
                OutlinedButton(
                    onClick = onViewManifest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manifest Görüntüle")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
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
