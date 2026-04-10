package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ClassInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmaliViewerScreen(
    className: String,
    smaliCode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var currentSearchIndex by remember { mutableIntStateOf(-1) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Smali kodunu satırlara böl
    val lines = remember(smaliCode) {
        smaliCode.split("\n").mapIndexed { index, line ->
            LineInfo(index + 1, line, getLineType(line))
        }
    }
    
    // Arama sonuçları
    LaunchedEffect(searchQuery, lines) {
        if (searchQuery.isNotEmpty()) {
            delay(100)
            val results = mutableListOf<Pair<Int, Int>>()
            lines.forEachIndexed { index, lineInfo ->
                var startIndex = lineInfo.content.indexOf(searchQuery, ignoreCase = true)
                while (startIndex >= 0) {
                    results.add(index to startIndex)
                    startIndex = lineInfo.content.indexOf(searchQuery, startIndex + 1, ignoreCase = true)
                }
            }
            searchResults = results
            currentSearchIndex = if (results.isNotEmpty()) 0 else -1
            
            // İlk sonuca scroll
            if (results.isNotEmpty()) {
                scope.launch {
                    listState.scrollToItem(results[0].first)
                }
            }
        } else {
            searchResults = emptyList()
            currentSearchIndex = -1
        }
    }
    
    // Filtreleme
    val filteredLines = remember(lines, searchQuery) {
        if (searchQuery.isEmpty()) lines
        else lines.filter { it.content.contains(searchQuery, ignoreCase = true) }
    }
    
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
                            className.substringAfterLast("/").removeSuffix(";"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${lines.size} satır Smali kod",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    // Arama butonu
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(
                            if (isSearchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            "Ara"
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
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Arama çubuğu
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SearchBarWithNavigation(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = searchResults,
                    currentIndex = currentSearchIndex,
                    onNext = {
                        if (searchResults.isNotEmpty()) {
                            val newIndex = (currentSearchIndex + 1) % searchResults.size
                            currentSearchIndex = newIndex
                            scope.launch {
                                listState.scrollToItem(searchResults[newIndex].first)
                            }
                        }
                    },
                    onPrevious = {
                        if (searchResults.isNotEmpty()) {
                            val newIndex = if (currentSearchIndex > 0) 
                                currentSearchIndex - 1 
                            else 
                                searchResults.size - 1
                            currentSearchIndex = newIndex
                            scope.launch {
                                listState.scrollToItem(searchResults[newIndex].first)
                            }
                        }
                    },
                    onClose = {
                        isSearchVisible = false
                        searchQuery = ""
                    }
                )
            }
            
            // Smali kod görüntüleyici
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = filteredLines,
                        key = { _, line -> line.number }
                    ) { _, lineInfo ->
                        SmaliLineItem(
                            lineInfo = lineInfo,
                            searchQuery = searchQuery,
                            isHighlighted = searchResults.getOrNull(currentSearchIndex)?.first == lineInfo.number - 1
                        )
                    }
                }
                
                // Hızlı scroll butonları
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.scrollToItem(0)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(20.dp))
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.scrollToItem(filteredLines.size - 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarWithNavigation(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Pair<Int, Int>>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Smali kodda ara...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            if (results.isNotEmpty()) {
                Text(
                    text = "${currentIndex + 1}/${results.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = onPrevious, enabled = results.isNotEmpty()) {
                Icon(Icons.Filled.KeyboardArrowUp, "Önceki")
            }
            
            IconButton(onClick = onNext, enabled = results.isNotEmpty()) {
                Icon(Icons.Filled.KeyboardArrowDown, "Sonraki")
            }
            
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Kapat")
            }
        }
    }
}

data class LineInfo(
    val number: Int,
    val content: String,
    val type: LineType
)

enum class LineType {
    CLASS_DEF,      // .class
    SUPER_DEF,      // .super
    SOURCE_DEF,     // .source
    METHOD_DEF,     // .method
    FIELD_DEF,      // .field
    DIRECTIVE,      // .locals, .prologue, .line, etc
    INSTRUCTION,    // invoke-*, const-*, etc
    LABEL,          // :cond_, :goto_, etc
    COMMENT,        // #
    EMPTY           // boş satır
}

private fun getLineType(line: String): LineType {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return LineType.EMPTY
    if (trimmed.startsWith("#")) return LineType.COMMENT
    if (trimmed.startsWith(".class")) return LineType.CLASS_DEF
    if (trimmed.startsWith(".super")) return LineType.SUPER_DEF
    if (trimmed.startsWith(".source")) return LineType.SOURCE_DEF
    if (trimmed.startsWith(".method")) return LineType.METHOD_DEF
    if (trimmed.startsWith(".end method")) return LineType.METHOD_DEF
    if (trimmed.startsWith(".field")) return LineType.FIELD_DEF
    if (trimmed.startsWith(".")) return LineType.DIRECTIVE
    if (trimmed.startsWith(":")) return LineType.LABEL
    if (trimmed.contains("-")) return LineType.INSTRUCTION
    return LineType.EMPTY
}

@Composable
private fun SmaliLineItem(
    lineInfo: LineInfo,
    searchQuery: String,
    isHighlighted: Boolean
) {
    val backgroundColor = when {
        isHighlighted -> Color(0xFFFFEB3B).copy(alpha = 0.3f)
        lineInfo.number % 2 == 0 -> Color(0xFF1E1E1E)
        else -> Color(0xFF252525)
    }
    
    val lineNumberColor = Color(0xFF6E7681)
    val textColor = when (lineInfo.type) {
        LineType.CLASS_DEF -> Color(0xFFFFC107)      // Sarı
        LineType.METHOD_DEF -> Color(0xFF4CAF50)    // Yeşil
        LineType.FIELD_DEF -> Color(0xFF2196F3)     // Mavi
        LineType.DIRECTIVE -> Color(0xFF9C27B0)     // Mor
        LineType.INSTRUCTION -> Color(0xFFA9B7C6)   // Açık gri
        LineType.LABEL -> Color(0xFFFF5722)         // Turuncu
        LineType.COMMENT -> Color(0xFF808080)       // Gri
        else -> Color(0xFFA9B7C6)
    }
    
    val annotatedString = buildAnnotatedString {
        if (searchQuery.isNotEmpty()) {
            val content = lineInfo.content
            var currentIndex = 0
            while (currentIndex < content.length) {
                val foundIndex = content.indexOf(searchQuery, currentIndex, ignoreCase = true)
                if (foundIndex >= 0) {
                    // Eşleşmeden önceki kısım
                    append(content.substring(currentIndex, foundIndex))
                    // Eşleşen kısım (vurgulu)
                    withStyle(SpanStyle(background = Color(0xFFFFEB3B), color = Color.Black)) {
                        append(content.substring(foundIndex, foundIndex + searchQuery.length))
                    }
                    currentIndex = foundIndex + searchQuery.length
                } else {
                    append(content.substring(currentIndex))
                    break
                }
            }
        } else {
            append(lineInfo.content)
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Satır numarası
        Text(
            text = lineInfo.number.toString().padStart(4, ' '),
            color = lineNumberColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(40.dp)
        )
        
        // Smali kodu
        Text(
            text = annotatedString,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
