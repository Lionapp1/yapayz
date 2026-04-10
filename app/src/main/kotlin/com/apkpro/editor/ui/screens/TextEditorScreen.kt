package com.apkpro.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkpro.editor.data.model.ApkFileEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    fileEntry: ApkFileEntry,
    content: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onViewSmali: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var textContent by remember { mutableStateOf(content) }
    var hasChanges by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Değişiklik kontrolü
    LaunchedEffect(textContent) {
        hasChanges = textContent != content
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(fileEntry.name)
                        if (hasChanges) {
                            Text(
                                "Değişiklikler kaydedilmedi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (hasChanges) showSaveDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    // Smali görüntüle (sadece DEX için)
                    if (fileEntry.type == com.apkpro.editor.data.model.ApkFileType.DEX && onViewSmali != null) {
                        IconButton(onClick = onViewSmali) {
                            Icon(Icons.Default.Code, "Smali Görüntüle")
                        }
                    }
                    
                    // Kaydet
                    IconButton(
                        onClick = { onSave(textContent) },
                        enabled = hasChanges
                    ) {
                        Icon(Icons.Default.Save, "Kaydet")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Dosya bilgisi
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Boyut: ${formatSize(fileEntry.size)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "CRC: ${fileEntry.crc}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Metin editörü
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                BasicTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        if (textContent.isEmpty()) {
                            Text(
                                "Dosya içeriği...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
    
    // Kaydetme dialogu
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Değişiklikleri Kaydet?") },
            text = { Text("Kaydedilmemiş değişiklikler var. Kaydetmeden çıkmak istiyor musunuz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(textContent)
                        showSaveDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        onBack()
                    }
                ) {
                    Text("Çık")
                }
            }
        )
    }
}

private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}
