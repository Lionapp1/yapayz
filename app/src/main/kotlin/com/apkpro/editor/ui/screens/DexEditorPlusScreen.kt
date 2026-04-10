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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apkpro.editor.data.model.ApkFileEntry
import com.apkpro.editor.data.model.ClassInfo
import com.apkpro.editor.data.model.MethodInfo
import com.apkpro.editor.data.model.SmaliClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexEditorPlusScreen(
    dexFiles: List<Pair<ApkFileEntry, List<ClassInfo>>>,
    onBack: () -> Unit,
    onClassClick: (ApkFileEntry, ClassInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDexIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyUserClasses by remember { mutableStateOf(true) }
    
    val currentDex = dexFiles.getOrNull(selectedDexIndex)
    
    val filteredClasses = remember(searchQuery, showOnlyUserClasses, currentDex) {
        currentDex?.second?.filter { classInfo ->
            val matchesSearch = if (searchQuery.isEmpty()) true
            else classInfo.name.contains(searchQuery, ignoreCase = true)
            
            val isUserClass = if (showOnlyUserClasses) {
                !classInfo.name.startsWith("Landroid/") &&
                !classInfo.name.startsWith("Ljava/") &&
                !classInfo.name.startsWith("Lkotlin/") &&
                !classInfo.name.startsWith("Landroidx/")
            } else true
            
            matchesSearch && isUserClass
        } ?: emptyList()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DEX Editör Plus")
                        currentDex?.first?.let { dex ->
                            Text(
                                "${dex.name} - ${dex.second.size} sınıf",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showOnlyUserClasses = !showOnlyUserClasses }) {
                        Icon(
                            if (showOnlyUserClasses) Icons.Default.Person else Icons.Default.Public,
                            contentDescription = "Filtre"
                        )
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
            // DEX Dosya Seçici
            if (dexFiles.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedDexIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dexFiles.forEachIndexed { index, (dexEntry, _) ->
                        Tab(
                            selected = selectedDexIndex == index,
                            onClick = { selectedDexIndex = index },
                            text = { Text(dexEntry.name) }
                        )
                    }
                }
            }
            
            // Arama
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Sınıf ara... (örn: MainActivity)") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            
            // İstatistikler
            currentDex?.let { (dex, classes) ->
                DexStatisticsRow(
                    totalClasses = classes.size,
                    filteredClasses = filteredClasses.size,
                    methodCount = classes.sumOf { it.methods.size },
                    fieldCount = classes.sumOf { it.fields.size }
                )
            }
            
            // Sınıf listesi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(filteredClasses) { classInfo ->
                    ClassEntryItem(
                        classInfo = classInfo,
                        onClick = { 
                            currentDex?.first?.let { dex ->
                                onClassClick(dex, classInfo)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DexStatisticsRow(
    totalClasses: Int,
    filteredClasses: Int,
    methodCount: Int,
    fieldCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Sınıf", filteredClasses.toString())
        StatItem("Metod", methodCount.toString())
        StatItem("Field", fieldCount.toString())
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassEntryItem(
    classInfo: ClassInfo,
    onClick: () -> Unit
) {
    val simpleName = classInfo.name.substringAfterLast('/').trimEnd(';')
    val packageName = classInfo.name.substringBeforeLast('/').trimStart('L').replace('/', '.')
    
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
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = simpleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row {
                    Text(
                        text = "${classInfo.methods.size} metod",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${classInfo.fields.size} field",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Görüntüle"
            )
        }
    }
}

// ========== SMALI KOD GÖRÜNTÜLEYİCİ ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmaliViewerScreen(
    smaliClass: SmaliClass,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMethodIndex by remember { mutableIntStateOf(-1) }
    var viewMode by remember { mutableStateOf(ViewMode.SMALI) }
    
    enum class ViewMode { SMALI, JAVA, BYTECODE }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(smaliClass.className.substringAfterLast('/').trimEnd(';'))
                        Text(
                            "${smaliClass.methods.size} metod",
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
                    // Görünüm modu seçici
                    DropdownMenuContent(
                        currentMode = viewMode,
                        onModeChange = { viewMode = it }
                    )
                }
            )
        }
    ) { padding ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sol panel - Metod listesi
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    "Metodlar",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                LazyColumn {
                    items(smaliClass.methods) { method ->
                        val index = smaliClass.methods.indexOf(method)
                        MethodListItem(
                            method = method,
                            isSelected = selectedMethodIndex == index,
                            onClick = { selectedMethodIndex = index }
                        )
                    }
                }
            }
            
            // Sağ panel - Kod görüntüleyici
            VerticalDivider()
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Kod başlığı
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (viewMode) {
                                ViewMode.SMALI -> "Smali Kod"
                                ViewMode.JAVA -> "Java (Tahmini)"
                                ViewMode.BYTECODE -> "Bytecode"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                
                // Kod içeriği
                val codeToShow = if (selectedMethodIndex >= 0) {
                    smaliClass.methods.getOrNull(selectedMethodIndex)?.code ?: smaliClass.smaliCode
                } else {
                    smaliClass.smaliCode
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Text(
                            text = codeToShow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodListItem(
    method: com.apkpro.editor.data.model.SmaliMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = method.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${method.lineCount} satır",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun DropdownMenuContent(
    currentMode: DexEditorPlusScreen.ViewMode,
    onModeChange: (DexEditorPlusScreen.ViewMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, "Menü")
    }
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Smali") },
            onClick = { 
                onModeChange(DexEditorPlusScreen.ViewMode.SMALI)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Java") },
            onClick = { 
                onModeChange(DexEditorPlusScreen.ViewMode.JAVA)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Bytecode") },
            onClick = { 
                onModeChange(DexEditorPlusScreen.ViewMode.BYTECODE)
                expanded = false
            }
        )
    }
}
