package com.apkpro.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkpro.editor.data.model.ApkFileEntry
import com.apkpro.editor.data.model.ApkFileType
import com.apkpro.editor.data.model.ApkStructure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    structure: ApkStructure,
    onBack: () -> Unit,
    onFileClick: (ApkFileEntry) -> Unit,
    onDexEditorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    val tabs = listOf("Tümü", "DEX", "Kaynaklar", "Assets", "Lib")
    
    val filteredFiles = remember(searchQuery, selectedTab, structure) {
        val files = when (selectedTab) {
            0 -> structure.allFiles
            1 -> structure.dexFiles
            2 -> structure.resources
            3 -> structure.assets
            4 -> structure.libraries
            else -> structure.allFiles
        }
        
        if (searchQuery.isEmpty()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("APK Dosya Gezgini")
                        Text(
                            "${structure.allFiles.size} dosya",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    // DEX Editör Plus
                    IconButton(onClick = onDexEditorClick) {
                        Icon(Icons.Default.Edit, "DEX Editör")
                    }
                    IconButton(onClick = { /* Arama */ }) {
                        Icon(Icons.Default.Search, "Ara")
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
            // Arama
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Dosya ara...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            
            // Tablar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Dosya listesi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(filteredFiles) { file ->
                    FileEntryItem(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileEntryItem(
    file: ApkFileEntry,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dosya tipi ikonu
            Icon(
                imageVector = getFileIcon(file.type),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = getFileColor(file.type)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row {
                    Text(
                        text = formatSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (file.canEdit) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Düzenlenebilir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Düzenle ikonu
            if (file.canEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun getFileIcon(type: ApkFileType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        ApkFileType.DEX -> Icons.Default.Code
        ApkFileType.MANIFEST -> Icons.Default.Description
        ApkFileType.XML_RESOURCE -> Icons.Default.Code
        ApkFileType.LAYOUT -> Icons.Default.ViewList
        ApkFileType.DRAWABLE -> Icons.Default.Image
        ApkFileType.ASSET -> Icons.Default.Folder
        ApkFileType.LIBRARY -> Icons.Default.Memory
        ApkFileType.CERTIFICATE -> Icons.Default.Security
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getFileColor(type: ApkFileType): androidx.compose.ui.graphics.Color {
    return when (type) {
        ApkFileType.DEX -> MaterialTheme.colorScheme.primary
        ApkFileType.MANIFEST -> MaterialTheme.colorScheme.tertiary
        ApkFileType.XML_RESOURCE -> MaterialTheme.colorScheme.secondary
        ApkFileType.LAYOUT -> MaterialTheme.colorScheme.secondary
        ApkFileType.DRAWABLE -> MaterialTheme.colorScheme.error
        ApkFileType.ASSET -> MaterialTheme.colorScheme.primary
        ApkFileType.LIBRARY -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
}

private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
