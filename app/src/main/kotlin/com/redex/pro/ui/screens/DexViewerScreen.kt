package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ClassInfo
import com.redex.pro.data.model.DexFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexViewerScreen(
    dexFiles: List<DexFile>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedClass by remember { mutableStateOf<ClassInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val allClasses = dexFiles.flatMap { it.classes }
    val filteredClasses = if (searchQuery.isEmpty()) {
        allClasses
    } else {
        allClasses.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    // Android fiziksel geri tuşu desteği
    BackHandler(enabled = true) {
        onBack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DEX Görüntüleyici") },
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
        Column(modifier = modifier.padding(padding)) {
            // Arama çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Sınıf ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine = true
            )
            
            // İstatistikler
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Sınıf", dexFiles.sumOf { it.classCount }.toString())
                    StatItem("Metod", dexFiles.sumOf { it.methodCount }.toString())
                    StatItem("Alan", dexFiles.sumOf { it.fieldCount }.toString())
                    StatItem("String", dexFiles.sumOf { it.stringCount }.toString())
                }
            }
            
            // DEX dosyası seçimi
            if (dexFiles.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    dexFiles.forEachIndexed { index, dex ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(dex.name) }
                        )
                    }
                }
            }
            
            // Sınıf listesi
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredClasses) { classInfo ->
                    ClassItem(
                        classInfo = classInfo,
                        onClick = { selectedClass = classInfo }
                    )
                }
            }
        }
    }
    
    // Sınıf detay dialogu
    selectedClass?.let { classInfo ->
        ClassDetailDialog(
            classInfo = classInfo,
            onDismiss = { selectedClass = null }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ClassItem(classInfo: ClassInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                classInfo.name.substringAfterLast("/", classInfo.name),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                classInfo.name,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Badge(count = classInfo.methods.size, label = "metod")
                Badge(count = classInfo.fields.size, label = "alan")
            }
        }
    }
}

@Composable
private fun Badge(count: Int, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            "$count $label",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ClassDetailDialog(classInfo: ClassInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                classInfo.name.substringAfterLast("/"),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                Text(
                    "Tam Ad: ${classInfo.name}",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                classInfo.superClass?.let {
                    Text(
                        "Extends: $it",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Text(
                    "Metodlar (${classInfo.methods.size}):",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                classInfo.methods.take(10).forEach { method ->
                    Text(
                        "  • ${method.name}${method.descriptor}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (classInfo.methods.size > 10) {
                    Text(
                        "  +${classInfo.methods.size - 10} metod daha...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
