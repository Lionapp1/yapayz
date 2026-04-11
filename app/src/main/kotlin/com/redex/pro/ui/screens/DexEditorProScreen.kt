package com.redex.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.redex.pro.data.model.ApkFileEntry
import com.redex.pro.data.model.ClassInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DexEditorProScreen(
    dexFiles: List<Pair<ApkFileEntry, List<ClassInfo>>>,
    onBack: () -> Unit,
    onClassClick: (ApkFileEntry, ClassInfo) -> Unit,
    onEditSmali: (ApkFileEntry, ClassInfo, String) -> Unit = { _, _, _ -> },
    onViewSmali: ((ApkFileEntry, ClassInfo) -> Unit)? = null
) {
    var selectedDexIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showOnlyUserClasses by rememberSaveable { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<ClassInfo?>(null) }
    var smaliCode by remember { mutableStateOf("") }
    
    // Filtrelenmiş sınıflar - background thread'de hesaplanacak
    var filteredClasses by remember { mutableStateOf<List<ClassInfo>>(emptyList()) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    val currentDex = dexFiles.getOrNull(selectedDexIndex)
    
    // Performans için debounce arama + background thread
    var debouncedQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(searchQuery) {
        delay(150) // 150ms debounce
        debouncedQuery = searchQuery
    }
    
    // Filtreleme - background thread'de yapılıyor (ANA PERFORMANS İYİLEŞTİRMESİ)
    LaunchedEffect(debouncedQuery, showOnlyUserClasses, currentDex, selectedDexIndex) {
        isLoading = true
        withContext(Dispatchers.Default) {
            val result = currentDex?.second?.let { classes ->
                classes.filter { classInfo ->
                    val matchesSearch = debouncedQuery.isEmpty() || 
                        classInfo.name.contains(debouncedQuery, ignoreCase = true)
                    
                    val isUserClass = if (showOnlyUserClasses) {
                        !classInfo.name.startsWith("Landroid/") &&
                        !classInfo.name.startsWith("Ljava/") &&
                        !classInfo.name.startsWith("Lkotlin/") &&
                        !classInfo.name.startsWith("Landroidx/") &&
                        !classInfo.name.startsWith("Lkotlinx/") &&
                        !classInfo.name.startsWith("Lcom/google/") &&
                        !classInfo.name.startsWith("Lcom/android/")
                    } else true
                    
                    matchesSearch && isUserClass
                }.sortedBy { it.name }
            } ?: emptyList()
            
            withContext(Dispatchers.Main) {
                filteredClasses = result
                isLoading = false
            }
        }
    }
    
    // İstatistikler
    val stats = remember(currentDex) {
        val totalClasses = currentDex?.second?.size ?: 0
        val totalMethods = currentDex?.second?.sumOf { it.methods.size } ?: 0
        val totalFields = currentDex?.second?.sumOf { it.fields.size } ?: 0
        val userClasses = currentDex?.second?.count { 
            !it.name.startsWith("Landroid/") && !it.name.startsWith("Ljava/") 
        } ?: 0
        Triple(totalClasses, totalMethods, totalFields) to userClasses
    }
    
    val (totalStats, userClassCount) = stats
    val (totalClasses, totalMethods, totalFields) = totalStats
    
    // Android fiziksel geri tuşu desteği
    BackHandler(enabled = true) {
        onBack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DEX Editor Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        currentDex?.let { (dex, _) ->
                            Text(
                                "${dex.name} • ${filteredClasses.size} sınıf",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                    IconButton(
                        onClick = { showOnlyUserClasses = !showOnlyUserClasses },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (showOnlyUserClasses) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            if (showOnlyUserClasses) Icons.Filled.FilterAlt 
                            else Icons.Filled.FilterAltOff,
                            "Filtre"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // DEX Sekmeleri
            if (dexFiles.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedDexIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    dexFiles.forEachIndexed { index, (dex, _) ->
                        Tab(
                            selected = selectedDexIndex == index,
                            onClick = { 
                                selectedDexIndex = index
                                scope.launch {
                                    listState.scrollToItem(0)
                                }
                            },
                            text = { Text(dex.name) }
                        )
                    }
                }
            }
            
            // İstatistik Kartları
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = if (showOnlyUserClasses) "$userClassCount" else "$totalClasses",
                        label = "Sınıf",
                        icon = Icons.Filled.Class
                    )
                    StatItem(
                        value = "$totalMethods",
                        label = "Metod",
                        icon = Icons.Filled.Functions
                    )
                    StatItem(
                        value = "$totalFields",
                        label = "Alan",
                        icon = Icons.Filled.DataObject
                    )
                }
            }
            
            // Arama çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                placeholder = { Text("Sınıf ara...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            // Sınıf listesi - performans için LazyColumn
            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredClasses.isEmpty() && !isLoading) {
                    EmptyState(
                        icon = Icons.Filled.SearchOff,
                        message = if (searchQuery.isEmpty()) 
                            "Sınıf bulunamadı" 
                        else 
                            "Arama sonucu bulunamadı"
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = filteredClasses,
                            key = { it.name }
                        ) { classInfo ->
                            currentDex?.let { (dex, _) ->
                                ClassListItem(
                                    classInfo = classInfo,
                                    onClick = { 
                                        selectedClass = classInfo
                                        smaliCode = "// Smali code placeholder for ${classInfo.name}"
                                        showEditDialog = true
                                    },
                                    onViewSmali = onViewSmali?.let { { it(dex, classInfo) } },
                                    onEditSmali = { 
                                        selectedClass = classInfo
                                        smaliCode = "// Smali code placeholder for ${classInfo.name}"
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                }
                
                // Yükleme göstergesi
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
    
    // Smali düzenleme dialogu
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Smali Düzenle") },
            text = {
                Column {
                    Text(
                        "Sınıf: ${selectedClass?.name?.removePrefix("L")?.removeSuffix(";")?.replace("/", ".")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smaliCode,
                        onValueChange = { smaliCode = it },
                        label = { Text("Smali Kodu") },
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedClass?.let { classInfo ->
                        currentDex?.let { (dex, _) ->
                            onEditSmali(dex, classInfo, smaliCode)
                        }
                    }
                    showEditDialog = false
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ClassListItem(
    classInfo: ClassInfo,
    onClick: () -> Unit,
    onViewSmali: (() -> Unit)? = null,
    onEditSmali: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayName = classInfo.name
        .removePrefix("L")
        .removeSuffix(";")
        .replace("/", ".")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sınıf ikonu
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Sınıf bilgileri
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName.substringAfterLast("."),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displayName.substringBeforeLast(".", ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Metod ve alan sayıları
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountBadge(
                    count = classInfo.methods.size,
                    icon = Icons.Filled.Functions
                )
                CountBadge(
                    count = classInfo.fields.size,
                    icon = Icons.Filled.DataObject
                )
                
                // Düzenle butonu
                if (onEditSmali != null) {
                    IconButton(
                        onClick = onEditSmali,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Düzenle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Kodları Gör butonu
                if (onViewSmali != null) {
                    IconButton(
                        onClick = onViewSmali,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = "Kodları Gör",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun CountBadge(
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
