package com.apkpro.editor.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
import com.apkpro.editor.data.model.ApkFileEntry
import com.apkpro.editor.data.model.ClassInfo
import com.apkpro.editor.data.model.FieldInfo
import com.apkpro.editor.data.model.MethodInfo
import com.apkpro.editor.data.model.SmaliClass
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun DexEditorPlusScreen(
    dexFiles: List<Pair<ApkFileEntry, List<ClassInfo>>>,
    onBack: () -> Unit,
    onClassClick: (ApkFileEntry, ClassInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDexIndex by rememberSaveable { mutableIntStateOf(0) }
    val searchQueryFlow = remember { MutableStateFlow("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var showOnlyUserClasses by rememberSaveable { mutableStateOf(true) }
    var expandedClassIndex by rememberSaveable { mutableIntStateOf(-1) }
    var sortByMethodCount by rememberSaveable { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Debounce arama için
    LaunchedEffect(Unit) {
        searchQueryFlow
            .debounce(150) // 150ms debounce
            .distinctUntilChanged()
            .collect { query ->
                debouncedQuery = query
            }
    }
    
    val currentDex = dexFiles.getOrNull(selectedDexIndex)
    
    // Hızlı filtreleme
    val filteredClasses = remember(debouncedQuery, showOnlyUserClasses, currentDex, sortByMethodCount, expandedClassIndex) {
        val classes = currentDex?.second
            ?.filter { classInfo ->
                val matchesSearch = if (debouncedQuery.isEmpty()) true
                else classInfo.name.contains(debouncedQuery, ignoreCase = true)
                
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
            }
            ?.let { list ->
                if (sortByMethodCount) {
                    list.sortedByDescending { it.methods.size }
                } else {
                    list.sortedBy { it.name }
                }
            }
            ?: emptyList()
        classes
    }
    
    // İstatistikler
    val totalMethods = remember(currentDex) { currentDex?.second?.sumOf { it.methods.size } ?: 0 }
    val totalFields = remember(currentDex) { currentDex?.second?.sumOf { it.fields.size } ?: 0 }
    val userClassCount = remember(currentDex) { 
        currentDex?.second?.count { 
            !it.name.startsWith("Landroid/") && !it.name.startsWith("Ljava/") 
        } ?: 0 
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DEX Editör Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        currentDex?.let { (dex, _) ->
                            Text(
                                "${dex.name} • ${filteredClasses.size}/${currentDex.second.size} sınıf",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri Dön")
                    }
                },
                actions = {
                    // Sıralama
                    IconButton(onClick = { sortByMethodCount = !sortByMethodCount }) {
                        Icon(
                            if (sortByMethodCount) Icons.Default.Sort else Icons.Default.SortByAlpha,
                            contentDescription = "Sırala"
                        )
                    }
                    // Filtre
                    IconButton(onClick = { showOnlyUserClasses = !showOnlyUserClasses }) {
                        Icon(
                            if (showOnlyUserClasses) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = "Filtre",
                            tint = if (showOnlyUserClasses) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // DEX Sekmeleri
            if (dexFiles.size > 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedDexIndex,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty()) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDexIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        dexFiles.forEachIndexed { index, (dexEntry, classes) ->
                            Tab(
                                selected = selectedDexIndex == index,
                                onClick = { 
                                    selectedDexIndex = index
                                    expandedClassIndex = -1
                                },
                                text = { 
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(dexEntry.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${classes.size} sınıf",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Arama Barı
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQueryFlow.value,
                    onValueChange = { 
                        searchQueryFlow.value = it
                        coroutineScope.launch { searchQueryFlow.emit(it) }
                        expandedClassIndex = -1
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    placeholder = { 
                        Text("Sınıf ara... (örn: MainActivity, onCreate)") 
                    },
                    leadingIcon = { 
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) 
                    },
                    trailingIcon = {
                        if (searchQueryFlow.value.isNotEmpty()) {
                            IconButton(
                                onClick = { 
                                    searchQueryFlow.value = ""
                                    coroutineScope.launch { searchQueryFlow.emit("") }
                                }
                            ) {
                                Icon(Icons.Default.Clear, "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
            
            // İstatistik Kartları
            AnimatedVisibility(visible = currentDex != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Sınıf",
                        value = filteredClasses.size.toString(),
                        total = currentDex?.second?.size?.toString() ?: "0",
                        icon = Icons.Default.Code,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Metod",
                        value = filteredClasses.sumOf { it.methods.size }.toString(),
                        total = totalMethods.toString(),
                        icon = Icons.Default.Functions,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Field",
                        value = filteredClasses.sumOf { it.fields.size }.toString(),
                        total = totalFields.toString(),
                        icon = Icons.Default.DataObject,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Sınıf Listesi - HIZLI ve PAGINATED
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = filteredClasses,
                        key = { _, classInfo -> classInfo.name }
                    ) { index, classInfo ->
                        ExpandableClassItem(
                            classInfo = classInfo,
                            searchQuery = debouncedQuery,
                            isExpanded = expandedClassIndex == index,
                            onExpandToggle = { 
                                expandedClassIndex = if (expandedClassIndex == index) -1 else index 
                            },
                            onOpenClick = { 
                                currentDex?.first?.let { dex ->
                                    onClassClick(dex, classInfo)
                                }
                            }
                        )
                    }
                    
                    // Sonuç yoksa
                    if (filteredClasses.isEmpty() && debouncedQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Sonuç bulunamadı",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Yükleme göstergesi
                if (currentDex == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    total: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "/$total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun ExpandableClassItem(
    classInfo: ClassInfo,
    searchQuery: String,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onOpenClick: () -> Unit
) {
    val simpleName = classInfo.name.substringAfterLast('/').trimEnd(';')
    val packageName = classInfo.name.substringBeforeLast('/').trimStart('L').replace('/', '.')
    val isFrameworkClass = classInfo.name.startsWith("Landroid/") || 
                          classInfo.name.startsWith("Ljava/") ||
                          classInfo.name.startsWith("Lkotlin/")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 4.dp else 1.dp
        )
    ) {
        Column {
            // Ana satır
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aç/kapa ikonu
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Sınıf ikonu
                Icon(
                    imageVector = if (isFrameworkClass) Icons.Default.Code else Icons.Default.ClassIcon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isFrameworkClass) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else 
                        MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Sınıf adı ve paket
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            if (searchQuery.isNotEmpty() && simpleName.contains(searchQuery, ignoreCase = true)) {
                                val startIndex = simpleName.indexOf(searchQuery, ignoreCase = true)
                                val endIndex = startIndex + searchQuery.length
                                append(simpleName.substring(0, startIndex))
                                withStyle(SpanStyle(
                                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append(simpleName.substring(startIndex, endIndex))
                                }
                                append(simpleName.substring(endIndex))
                            } else {
                                append(simpleName)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Metod/Field sayısı
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (classInfo.methods.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("${classInfo.methods.size} M", fontSize = 10.sp)
                        }
                    }
                    if (classInfo.fields.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Text("${classInfo.fields.size} F", fontSize = 10.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Aç butonu
                IconButton(
                    onClick = { 
                        onExpandToggle()
                        onOpenClick() 
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Kod Görüntüle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Genişletilmiş içerik
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(thickness = 0.5.dp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Metodlar
                    if (classInfo.methods.isNotEmpty()) {
                        Text(
                            "Metodlar (${classInfo.methods.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        classInfo.methods.take(5).forEach { method ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Functions,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = method.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (classInfo.methods.size > 5) {
                            Text(
                                "+${classInfo.methods.size - 5} daha...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                            )
                        }
                    }
                    
                    // Fields
                    if (classInfo.fields.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Fields (${classInfo.fields.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        classInfo.fields.take(3).forEach { field ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DataObject,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = field.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (classInfo.fields.size > 3) {
                            Text(
                                "+${classInfo.fields.size - 3} daha...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Detaylı Görüntüle butonu
                    Button(
                        onClick = onOpenClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Code, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kod Görüntüle")
                    }
                }
            }
        }
    }
}

// Sınıf ikonu için extension
private val Icons.Default.ClassIcon: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Default.Code

// ========== SMALI KOD GÖRÜNTÜLEYİCİ ==========

enum class SmaliViewMode { SMALI, JAVA, BYTECODE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmaliViewerScreen(
    smaliClass: SmaliClass,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMethodIndex by rememberSaveable { mutableIntStateOf(-1) }
    var viewMode by remember { mutableStateOf(SmaliViewMode.SMALI) }
    var showLineNumbers by rememberSaveable { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSize by rememberSaveable { mutableIntStateOf(12) }
    
    val coroutineScope = rememberCoroutineScope()
    val codeScrollState = rememberLazyListState()
    val methodScrollState = rememberLazyListState()
    
    // Kodu satırlara ayır
    val codeToShow = remember(selectedMethodIndex, smaliClass) {
        if (selectedMethodIndex >= 0) {
            smaliClass.methods.getOrNull(selectedMethodIndex)?.code ?: smaliClass.smaliCode
        } else {
            smaliClass.smaliCode
        }
    }
    
    val codeLines = remember(codeToShow, searchQuery) {
        codeToShow.lines().mapIndexed { index, line ->
            index to line
        }.filter { (_, line) ->
            if (searchQuery.isEmpty()) true
            else line.contains(searchQuery, ignoreCase = true)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            smaliClass.className.substringAfterLast('/').trimEnd(';'),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${smaliClass.methods.size} metod • ${codeLines.size} satır",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri Dön")
                    }
                },
                actions = {
                    // Satır numaraları
                    IconButton(onClick = { showLineNumbers = !showLineNumbers }) {
                        Icon(
                            if (showLineNumbers) Icons.Default.FormatListNumbered else Icons.Default.FormatListBulleted,
                            contentDescription = "Satır Numaraları"
                        )
                    }
                    // Font boyutu
                    IconButton(
                        onClick = { if (fontSize > 8) fontSize-- },
                        enabled = fontSize > 8
                    ) {
                        Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { if (fontSize < 20) fontSize++ },
                        enabled = fontSize < 20
                    ) {
                        Text("A+", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    // Mod menüsü
                    DropdownMenuContent(
                        currentMode = viewMode,
                        onModeChange = { viewMode = it }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sol panel - Metod listesi
            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column {
                    // Metod ara
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("Metod ara...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    
                    // Tüm sınıf butonu
                    val isAllSelected = selectedMethodIndex == -1
                    Surface(
                        onClick = { selectedMethodIndex = -1 },
                        color = if (isAllSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Tüm Sınıf",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Metod listesi
                    LazyColumn(
                        state = methodScrollState,
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(smaliClass.methods) { index, method ->
                            val isSelected = selectedMethodIndex == index
                            val matchesSearch = searchQuery.isEmpty() || 
                                              method.name.contains(searchQuery, ignoreCase = true)
                            
                            if (matchesSearch) {
                                MethodListItem(
                                    method = method,
                                    isSelected = isSelected,
                                    searchQuery = searchQuery,
                                    onClick = { 
                                        selectedMethodIndex = index
                                        coroutineScope.launch {
                                            methodScrollState.animateScrollToItem(index)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Sağ panel - Kod görüntüleyici
            VerticalDivider()
            
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    // Kod başlığı
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (viewMode) {
                                        SmaliViewMode.SMALI -> Icons.Default.Code
                                        SmaliViewMode.JAVA -> Icons.Default.Coffee
                                        SmaliViewMode.BYTECODE -> Icons.Default.Memory
                                    },
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when (viewMode) {
                                        SmaliViewMode.SMALI -> "Smali Kod"
                                        SmaliViewMode.JAVA -> "Java (Tahmini)"
                                        SmaliViewMode.BYTECODE -> "Bytecode"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Text(
                                "${codeLines.size} satır",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    // Kod içeriği - Syntax Highlighting ile
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = codeScrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            items(codeLines, key = { it.first }) { (lineNum, line) ->
                                CodeLine(
                                    lineNumber = lineNum + 1,
                                    code = line,
                                    showLineNumbers = showLineNumbers,
                                    searchQuery = searchQuery,
                                    fontSize = fontSize
                                )
                            }
                        }
                        
                        // Hızlı scroll FAB
                        if (codeLines.size > 100) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            codeScrollState.animateScrollToItem(0)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(Icons.Default.VerticalAlignTop, "Başa Git", modifier = Modifier.size(20.dp))
                                }
                                FloatingActionButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            codeScrollState.animateScrollToItem(codeLines.size - 1)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Icon(Icons.Default.VerticalAlignBottom, "Sona Git", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
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
    searchQuery: String,
    onClick: () -> Unit
) {
    val isConstructor = method.name == "<init>" || method.name == "<clinit>"
    val isPrivate = method.access.contains("private")
    
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metod ikonu
            Icon(
                imageVector = if (isConstructor) Icons.Default.Home else Icons.Default.Functions,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isConstructor -> MaterialTheme.colorScheme.tertiary
                    isPrivate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
            
            Spacer(Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        val name = if (method.name == "<init>") "Constructor"
                                  else if (method.name == "<clinit>") "Static Block"
                                  else method.name
                        
                        if (searchQuery.isNotEmpty() && name.contains(searchQuery, ignoreCase = true)) {
                            val start = name.indexOf(searchQuery, ignoreCase = true)
                            val end = start + searchQuery.length
                            append(name.substring(0, start))
                            withStyle(SpanStyle(
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold
                            )) {
                                append(name.substring(start, end))
                            }
                            append(name.substring(end))
                        } else {
                            append(name)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${method.lineCount} satır • ${method.access}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CodeLine(
    lineNumber: Int,
    code: String,
    showLineNumbers: Boolean,
    searchQuery: String,
    fontSize: Int
) {
    val trimmedCode = code.trimEnd()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Satır numarası
        if (showLineNumbers) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    text = lineNumber.toString(),
                    modifier = Modifier
                        .width(48.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = (fontSize - 2).sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Kod satırı - Syntax Highlighting
        Text(
            text = buildAnnotatedString {
                val highlightedCode = if (searchQuery.isNotEmpty() && trimmedCode.contains(searchQuery, ignoreCase = true)) {
                    highlightSearch(trimmedCode, searchQuery)
                } else {
                    highlightSmaliSyntax(trimmedCode)
                }
                append(highlightedCode)
            },
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 6).sp,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun AnnotatedString.Builder.highlightSearch(code: String, query: String): AnnotatedString {
    val startIndex = code.indexOf(query, ignoreCase = true)
    if (startIndex == -1) {
        return highlightSmaliSyntax(code)
    }
    
    val endIndex = startIndex + query.length
    
    // Arama öncesi kısmı normal renklendir
    append(highlightSmaliSyntax(code.substring(0, startIndex)))
    
    // Arama metnini vurgula
    withStyle(SpanStyle(
        background = Color.Yellow.copy(alpha = 0.4f),
        color = Color.Black,
        fontWeight = FontWeight.Bold
    )) {
        append(code.substring(startIndex, endIndex))
    }
    
    // Arama sonrası kısmı normal renklendir
    append(highlightSmaliSyntax(code.substring(endIndex)))
    
    return toAnnotatedString()
}

private fun AnnotatedString.Builder.highlightSmaliSyntax(code: String): AnnotatedString {
    val smaliKeywords = listOf(
        ".class", ".super", ".source", ".method", ".end method", ".field", ".end field",
        ".prologue", ".line", ".locals", ".param", ".annotation", ".end annotation",
        "public", "private", "protected", "static", "final", "abstract", "synthetic",
        "return", "return-void", "const", "const-string", "const-class", "const/high16",
        "invoke-virtual", "invoke-direct", "invoke-static", "invoke-interface",
        "new-instance", "check-cast", "instance-of", "if-eq", "if-ne", "if-lt", "if-ge",
        "if-gt", "if-le", "if-eqz", "if-nez", "if-ltz", "if-gez", "if-gtz", "if-lez",
        "goto", "goto/16", "packed-switch", "sparse-switch"
    )
    
    val parts = code.split(" ")
    
    parts.forEachIndexed { index, part ->
        if (index > 0) append(" ")
        
        when {
            smaliKeywords.any { part.startsWith(it) || part == it } -> {
                // Anahtar kelime - mavi
                withStyle(SpanStyle(color = Color(0xFF0066CC), fontWeight = FontWeight.Medium)) {
                    append(part)
                }
            }
            part.startsWith("L") && (part.contains("/") || part.contains(";")) -> {
                // Sınıf referansı - turuncu
                withStyle(SpanStyle(color = Color(0xFFE65100))) {
                    append(part)
                }
            }
            part.startsWith("\"") && part.endsWith("\"") -> {
                // String - yeşil
                withStyle(SpanStyle(color = Color(0xFF2E7D32))) {
                    append(part)
                }
            }
            part.startsWith("#") -> {
                // Yorum - gri
                withStyle(SpanStyle(color = Color(0xFF757575), fontStyle = FontStyle.Italic)) {
                    append(part)
                }
            }
            part.matches(Regex("^-?\\d+$")) || part.matches(Regex("^0x[0-9a-fA-F]+$")) -> {
                // Sayı - mor
                withStyle(SpanStyle(color = Color(0xFF7B1FA2))) {
                    append(part)
                }
            }
            else -> {
                append(part)
            }
        }
    }
    
    return toAnnotatedString()
}

@Composable
private fun DropdownMenuContent(
    currentMode: SmaliViewMode,
    onModeChange: (SmaliViewMode) -> Unit
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
                onModeChange(SmaliViewMode.SMALI)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Java") },
            onClick = { 
                onModeChange(SmaliViewMode.JAVA)
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Bytecode") },
            onClick = { 
                onModeChange(SmaliViewMode.BYTECODE)
                expanded = false
            }
        )
    }
}
