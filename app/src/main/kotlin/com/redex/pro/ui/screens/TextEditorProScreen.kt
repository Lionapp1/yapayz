package com.redex.pro.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ApkFileEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorProScreen(
    fileEntry: ApkFileEntry,
    content: String,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(content)) }
    var hasChanges by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentSearchIndex by remember { mutableStateOf(-1) }
    
    val isSmali = fileEntry.name.endsWith(".smali", ignoreCase = true)
    val isXml = fileEntry.name.endsWith(".xml", ignoreCase = true)
    val isJson = fileEntry.name.endsWith(".json", ignoreCase = true)
    val fileType = when {
        isSmali -> "Smali"
        isXml -> "XML"
        isJson -> "JSON"
        else -> "Text"
    }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Değişiklik takibi
    LaunchedEffect(textFieldValue.text) {
        hasChanges = textFieldValue.text != content
    }
    
    // Arama sonuçları
    LaunchedEffect(searchQuery, textFieldValue.text) {
        if (searchQuery.isNotEmpty()) {
            delay(100) // Debounce
            val results = mutableListOf<Int>()
            var index = textFieldValue.text.indexOf(searchQuery, ignoreCase = true)
            while (index >= 0) {
                results.add(index)
                index = textFieldValue.text.indexOf(searchQuery, index + 1, ignoreCase = true)
            }
            searchResults = results
            currentSearchIndex = if (results.isNotEmpty()) 0 else -1
        } else {
            searchResults = emptyList()
            currentSearchIndex = -1
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            fileEntry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            "$fileType • ${formatFileSize(fileEntry.size)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges) {
                                showSaveDialog = true
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    // Arama
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(
                            if (isSearchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            "Ara"
                        )
                    }
                    
                    // Kaydet
                    IconButton(
                        onClick = {
                            onSave(textFieldValue.text)
                            hasChanges = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Kaydedildi")
                            }
                        },
                        enabled = hasChanges,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (hasChanges) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    ) {
                        Icon(Icons.Filled.Save, "Kaydet")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Arama çubuğu
            AnimatedVisibility(visible = isSearchVisible) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = searchResults,
                    currentIndex = currentSearchIndex,
                    onNext = {
                        if (searchResults.isNotEmpty()) {
                            currentSearchIndex = (currentSearchIndex + 1) % searchResults.size
                        }
                    },
                    onPrevious = {
                        if (searchResults.isNotEmpty()) {
                            currentSearchIndex = if (currentSearchIndex > 0) 
                                currentSearchIndex - 1 
                            else 
                                searchResults.size - 1
                        }
                    },
                    onClose = { 
                        isSearchVisible = false 
                        searchQuery = ""
                    }
                )
            }
            
            // Editör
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
            ) {
                CodeEditor(
                    textFieldValue = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    isSmali = isSmali,
                    isXml = isXml,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Satır ve sütun bilgisi
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val lines = textFieldValue.text.count { it == '\n' } + 1
                    val chars = textFieldValue.text.length
                    Text(
                        text = "$lines satır • $chars karakter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    
    // Kaydetme dialogu
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Değişiklikleri Kaydet?") },
            text = { Text("Dosyada yapılan değişiklikleri kaydetmek istiyor musunuz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(textFieldValue.text)
                        showSaveDialog = false
                        onBack()
                    }
                ) {
                    Text("Kaydet ve Çık")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        onBack()
                    }
                ) {
                    Text("Çık (Kaydetme)")
                }
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Int>,
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
                placeholder = { Text("Ara...") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Sonuç sayısı
            if (results.isNotEmpty()) {
                Text(
                    text = "${currentIndex + 1}/${results.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            // Önceki
            IconButton(onClick = onPrevious, enabled = results.isNotEmpty()) {
                Icon(Icons.Filled.KeyboardArrowUp, "Önceki")
            }
            
            // Sonraki
            IconButton(onClick = onNext, enabled = results.isNotEmpty()) {
                Icon(Icons.Filled.KeyboardArrowDown, "Sonraki")
            }
            
            // Kapat
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Kapat")
            }
        }
    }
}

@Composable
private fun CodeEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isSmali: Boolean,
    isXml: Boolean,
    modifier: Modifier = Modifier
) {
    val codeBackground = Color(0xFF1E1E1E)
    val lineNumberColor = Color(0xFF6E7681)
    val textColor = Color(0xFFA9B7C6)
    
    // Satır numaraları
    val lines = textFieldValue.text.split("\n")
    val lineCount = lines.size
    val lineNumberWidth = (lineCount.toString().length + 1) * 12
    
    Row(modifier = modifier.background(codeBackground)) {
        // Satır numaraları
        Column(
            modifier = Modifier
                .width(lineNumberWidth.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            lines.indices.forEach { index ->
                Text(
                    text = "${index + 1}",
                    color = lineNumberColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        
        // Dikey ayırıcı
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF444444))
        )
        
        // Editör
        BasicTextField(
            value = textFieldValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            textStyle = TextStyle(
                color = textColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                innerTextField()
            }
        )
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}
