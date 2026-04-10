package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ApkInfo

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
    // Android fiziksel geri tuşu desteği
    BackHandler(enabled = true) {
        onBack()
    }
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
            // Uygulama Bilgileri - Modern kart
            item {
                ModernInfoCard(
                    title = "Uygulama Bilgileri",
                    icon = Icons.Default.Apps
                ) {
                    ModernInfoGrid(items = listOf(
                        "Paket" to apk.packageName,
                        "Versiyon" to "${apk.versionName} (${apk.versionCode})",
                        "Boyut" to formatFileSize(apk.size),
                        "Min SDK" to "API ${apk.minSdk}",
                        "Target" to "API ${apk.targetSdk}"
                    ))
                }
            }
            
            // Güvenlik Durumu - İmza kartı
            item {
                SecurityStatusCard(isSigned = apk.isSigned)
            }
            
            // Hash Değerleri - Collapsable kart
            item {
                ExpandableInfoCard(
                    title = "Hash Değerleri",
                    icon = Icons.Default.Fingerprint
                ) {
                    InfoRow("MD5", apk.md5.take(16) + "...", monospace = true)
                    InfoRow("SHA-1", apk.sha1.take(20) + "...", monospace = true)
                    InfoRow("SHA-256", apk.sha256.take(24) + "...", monospace = true)
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
            
            // Hızlı Erişim Butonları
            item {
                Text(
                    "Hızlı Erişim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            item {
                ActionButtonGrid(
                    buttons = listOf(
                        Triple(Icons.Default.Description, "Manifest", onViewManifest),
                        Triple(Icons.Default.Settings, "Kaynaklar (${apk.resources.stringCount})", onViewArsc),
                        Triple(Icons.Default.Code, "DEX", onViewDex),
                        Triple(Icons.Default.Transform, "Dönüştür", onViewConverter)
                    )
                )
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
private fun InfoRow(label: String, value: String, monospace: Boolean = false) {
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
            fontWeight = FontWeight.Medium,
            fontFamily = if (monospace) androidx.compose.ui.text.font.FontFamily.Monospace else null
        )
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size >= 1024 * 1024 * 1024 -> "%.2f GB".format(size.toDouble() / (1024 * 1024 * 1024))
        size >= 1024 * 1024 -> "%.2f MB".format(size.toDouble() / (1024 * 1024))
        size >= 1024 -> "%.2f KB".format(size.toDouble() / 1024)
        else -> "$size B"
    }
}

// ========== YENİ MODERN KOMPONENTLER ==========

@Composable
private fun ModernInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ModernInfoGrid(items: List<Pair<String, String>>) {
    Column {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rowItems.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SecurityStatusCard(isSigned: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSigned) 
                Color(0xFF4CAF50).copy(alpha = 0.15f) 
            else 
                Color(0xFFFFA726).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSigned) Icons.Default.Security else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSigned) Color(0xFF4CAF50) else Color(0xFFFFA726),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isSigned) "APK İmzalı" else "İmza Yok!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSigned) Color(0xFF4CAF50) else Color(0xFFFFA726)
                )
                Text(
                    text = if (isSigned) "Güvenli - Orjinal imza doğrulandı" 
                           else "Uyarı - APK imzasız veya geçersiz",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSigned) Color(0xFF4CAF50) else Color(0xFFFFA726)
                )
            }
        }
    }
}

@Composable
private fun ExpandableInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ActionButtonGrid(
    buttons: List<Triple<ImageVector, String, () -> Unit>>
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        buttons.chunked(2).forEach { rowButtons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowButtons.forEach { (icon, label, onClick) ->
                    ElevatedButton(
                        onClick = onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (rowButtons.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
