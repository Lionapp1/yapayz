package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    var editValue by remember { mutableStateOf(TextFieldValue("")) }
    
    BackHandler(enabled = true) { onBack() }
    
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                apk.name,
                                maxLines = 1,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                apk.packageName,
                                maxLines = 1,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
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
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ModernEditCard(
                    apk = apk,
                    onEditIcon = onEditIcon,
                    onEditName = {
                        editType = "name"
                        editValue = TextFieldValue(apk.name)
                        showEditDialog = true
                    },
                    onEditPackage = {
                        editType = "package"
                        editValue = TextFieldValue(apk.packageName)
                        showEditDialog = true
                    }
                )
            }
            
            item {
                ModernInfoCard(apk = apk)
            }
            
            item {
                ModernSecurityCard(isSigned = apk.isSigned)
            }
            
            if (apk.dexFiles.isNotEmpty()) {
                item {
                    Text(
                        "DEX Dosyaları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(apk.dexFiles, key = { it.name }) { dex ->
                    ModernDexCard(dex = dex, onClick = onViewDex)
                }
            }
            
            if (apk.permissions.isNotEmpty()) {
                item {
                    ModernPermissionsCard(permissions = apk.permissions)
                }
            }
            
            item {
                ModernResourcesCard(
                    onViewArsc = onViewArsc,
                    onViewManifest = onViewManifest
                )
            }
            
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    if (showEditDialog) {
        ModernEditDialog(
            editType = editType,
            editValue = editValue,
            onValueChange = { editValue = it },
            onDismiss = { showEditDialog = false },
            onConfirm = {
                when (editType) {
                    "name" -> onChangeName(editValue.text)
                    "package" -> onChangePackage(editValue.text)
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ModernEditCard(
    apk: ApkInfo,
    onEditIcon: () -> Unit,
    onEditName: () -> Unit,
    onEditPackage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "APK Düzenleme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernActionButton(
                    icon = Icons.Default.Image,
                    label = "İkon",
                    onClick = onEditIcon,
                    modifier = Modifier.weight(1f)
                )
                ModernActionButton(
                    icon = Icons.Default.Edit,
                    label = "İsim",
                    onClick = onEditName,
                    modifier = Modifier.weight(1f)
                )
                ModernActionButton(
                    icon = Icons.Default.Inventory,
                    label = "Paket",
                    onClick = onEditPackage,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .height(70.dp)
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ModernInfoCard(apk: ApkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Uygulama Bilgileri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRowModern(label = "Paket", value = apk.packageName)
                InfoRowModern(label = "Versiyon", value = "${apk.versionName} (${apk.versionCode})")
                InfoRowModern(label = "Boyut", value = formatFileSize(apk.size))
                InfoRowModern(label = "Min SDK", value = "API ${apk.minSdk}")
                InfoRowModern(label = "Target SDK", value = "API ${apk.targetSdk}")
            }
        }
    }
}

@Composable
private fun InfoRowModern(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModernSecurityCard(isSigned: Boolean) {
    val color = if (isSigned) Color(0xFF4CAF50) else Color(0xFFF44336)
    val bgColor = color.copy(alpha = 0.1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isSigned) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    if (isSigned) "İmzalı APK" else "İmzasız APK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    if (isSigned) "Bu APK güvenli bir şekilde imzalanmış" 
                    else "Bu APK imzalanmamış, dikkatli olun",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ModernDexCard(dex: com.redex.pro.data.model.DexFile, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dex.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    "${dex.classCount} sınıf • ${dex.methodCount} metod • ${dex.fieldCount} alan",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ModernPermissionsCard(permissions: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "İzinler (${permissions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                permissions.take(5).forEach { permission ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            permission.substringAfterLast("."),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 13.sp
                        )
                    }
                }
                if (permissions.size > 5) {
                    Text(
                        "+${permissions.size - 5} daha fazla...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernResourcesCard(
    onViewArsc: () -> Unit,
    onViewManifest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Kaynaklar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernResourceButton(
                    icon = Icons.Default.Storage,
                    label = "resources.arsc",
                    onClick = onViewArsc,
                    modifier = Modifier.weight(1f)
                )
                ModernResourceButton(
                    icon = Icons.Default.Description,
                    label = "Manifest",
                    onClick = onViewManifest,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ModernResourceButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .height(80.dp)
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ModernEditDialog(
    editType: String,
    editValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
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
                onValueChange = onValueChange,
                label = { Text("Yeni değer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
