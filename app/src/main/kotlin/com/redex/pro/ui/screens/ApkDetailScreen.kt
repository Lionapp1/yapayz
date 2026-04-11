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
import androidx.compose.runtime.*
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
    onEditIcon: () -> Unit = {},
    onChangeName: (String) -> Unit = {},
    onChangePackage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editType by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    
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
            // APK Düzenleme Butonları
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "APK Düzenleme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EditButton(
                                icon = Icons.Default.Image,
                                text = "İkon",
                                onClick = onEditIcon
                            )
                            EditButton(
                                icon = Icons.Default.Edit,
                                text = "İsim",
                                onClick = {
                                    editType = "name"
                                    editValue = apk.name
                                    showEditDialog = true
                                }
                            )
                            EditButton(
                                icon = Icons.Default.Package,
                                text = "Paket",
                                onClick = {
                                    editType = "package"
                                    editValue = apk.packageName
                                    showEditDialog = true
                                }
                            )
                        }
                    }
                }
            }
            
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
                        "İzinler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            apk.permissions.take(10).forEach { permission ->
                                Text(
                                    permission,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            if (apk.permissions.size > 10) {
                                Text(
                                    "+${apk.permissions.size - 10} daha fazla",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Kaynaklar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Kaynaklar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ResourceButton("resources.arsc", Icons.Default.Storage, onViewArsc)
                            ResourceButton("AndroidManifest.xml", Icons.Default.Description, onViewManifest)
                        }
                    }
                }
            }
        }
    }
    
    // Düzenleme Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { 
                Text(
                    when (editType) {
                        "name" -> "İsim Değiştir"
                        "package" -> "Paket Değiştir"
                        else -> "Düzenle"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Yeni değer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (editType) {
                            "name" -> onChangeName(editValue)
                            "package" -> onChangePackage(editValue)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun EditButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = Color.White)
    }
}

@Composable
private fun ResourceButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, fontSize = 10.sp, color = Color.White)
        }
    }
}

@Composable
private fun ModernInfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ModernInfoGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            InfoRow(label, value)
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = if (monospace) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SecurityStatusCard(isSigned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSigned) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else 
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSigned) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isSigned) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    if (isSigned) "İmzalı APK" else "İmzasız APK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSigned) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    if (isSigned) "Bu APK güvenli bir şekilde imzalanmış" 
                    else "Bu APK imzalanmamış, dikkatli olun",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
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
