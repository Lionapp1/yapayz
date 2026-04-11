package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ApkFileEntry
import kotlinx.coroutines.launch

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
    var searchQuery by remember { mutableStateOf("") }
    var showLineNumbers by remember { mutableStateOf(true) }
    var fontSize by remember { mutableIntStateOf(12) }
    var showSearch by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    
    // Değişiklik kontrolü
    LaunchedEffect(textContent) {
        hasChanges = textContent != content
    }
    
    // Dosya tipine göre syntax highlighting
    val isXml = fileEntry.name.endsWith(".xml", ignoreCase = true)
    val isSmali = fileEntry.name.endsWith(".smali", ignoreCase = true)
    
    // Kod satırları ve arama filtrelemesi
    val codeLines = remember(textContent, searchQuery) {
        textContent.lines().mapIndexed { index, line ->
            index to line
        }.filter { (_, line) ->
            if (searchQuery.isEmpty()) true
            else line.contains(searchQuery, ignoreCase = true)
        }
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
                            fileEntry.name,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            "${codeLines.size} satır ${if (hasChanges) "• Değişiklikler var" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasChanges) MaterialTheme.colorScheme.error 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
                    // Ara
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            "Ara"
                        )
                    }
                    
                    // Satır numaraları
                    IconButton(onClick = { showLineNumbers = !showLineNumbers }) {
                        Icon(
                            if (showLineNumbers) Icons.Default.FormatListNumbered else Icons.AutoMirrored.Filled.FormatListBulleted,
                            "Satır Numaraları"
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
                    
                    // Smali görüntüle (sadece DEX için)
                    if (fileEntry.type == com.redex.pro.data.model.ApkFileType.DEX && onViewSmali != null) {
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
            // Arama çubuğu
            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Ara...",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                "${codeLines.size} eşleşme",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            
            // Dosya bilgisi
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val typeLabel = when {
                            isXml -> "XML"
                            isSmali -> "Smali"
                            fileEntry.name.endsWith(".json") -> "JSON"
                            fileEntry.name.endsWith(".properties") -> "Properties"
                            else -> "Text"
                        }
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(typeLabel, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Boyut: ${formatSize(fileEntry.size)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "CRC: ${fileEntry.crc}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Kod editörü
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(codeLines, key = { index, item -> item.first }) { index, item ->
                            val (lineNum, line) = item
                            EditorLine(
                                lineNumber = lineNum + 1,
                                code = line,
                                showLineNumbers = showLineNumbers,
                                searchQuery = searchQuery,
                                fontSize = fontSize,
                                isXml = isXml,
                                isSmali = isSmali
                            )
                        }
                    }
                    
                    // Hızlı scroll FAB'ları
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
                                        scrollState.animateScrollToItem(0)
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
                                        scrollState.animateScrollToItem(codeLines.size - 1)
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

@Composable
private fun EditorLine(
    lineNumber: Int,
    code: String,
    showLineNumbers: Boolean,
    searchQuery: String,
    fontSize: Int,
    isXml: Boolean,
    isSmali: Boolean
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
        
        // Kod satırı
        Text(
            text = buildAnnotatedString {
                val highlightedCode = if (searchQuery.isNotEmpty() && trimmedCode.contains(searchQuery, ignoreCase = true)) {
                    highlightSearchInText(trimmedCode, searchQuery, isXml, isSmali)
                } else {
                    when {
                        isXml -> highlightXmlSyntax(trimmedCode)
                        isSmali -> highlightSmaliSyntaxForEditor(trimmedCode)
                        else -> buildAnnotatedString { append(trimmedCode) }
                    }
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

private fun highlightSearchInText(code: String, query: String, isXml: Boolean, isSmali: Boolean): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val startIndex = code.indexOf(query, ignoreCase = true)
    if (startIndex == -1) {
        return when {
            isXml -> highlightXmlSyntax(code)
            isSmali -> highlightSmaliSyntaxForEditor(code)
            else -> buildAnnotatedString { append(code) }
        }
    }
    
    val endIndex = startIndex + query.length
    
    // Arama öncesi
    val before = when {
        isXml -> highlightXmlSyntax(code.substring(0, startIndex))
        isSmali -> highlightSmaliSyntaxForEditor(code.substring(0, startIndex))
        else -> buildAnnotatedString { append(code.substring(0, startIndex)) }
    }
    builder.append(before)
    
    // Arama metni
    builder.withStyle(SpanStyle(
        background = Color.Yellow.copy(alpha = 0.4f),
        color = Color.Black,
        fontWeight = FontWeight.Bold
    )) {
        append(code.substring(startIndex, endIndex))
    }
    
    // Arama sonrası
    val after = when {
        isXml -> highlightXmlSyntax(code.substring(endIndex))
        isSmali -> highlightSmaliSyntaxForEditor(code.substring(endIndex))
        else -> buildAnnotatedString { append(code.substring(endIndex)) }
    }
    builder.append(after)
    
    return builder.toAnnotatedString()
}

private fun highlightXmlSyntax(code: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    
    // XML syntax highlighting
    val tagRegex = "<\\/?[\\w-]+".toRegex()
    val attrRegex = "\\s+[\\w-]+=".toRegex()
    val valueRegex = """"[^"]*""".toRegex()
    val commentRegex = "<!--.*?-->".toRegex()
    
    var currentIndex = 0
    
    while (currentIndex < code.length) {
        val remaining = code.substring(currentIndex)
        
        when {
            // Yorum
            remaining.startsWith("<!--") -> {
                val endIndex = remaining.indexOf("-->")
                if (endIndex != -1) {
                    val comment = remaining.substring(0, endIndex + 3)
                    builder.withStyle(SpanStyle(color = Color(0xFF757575), fontStyle = FontStyle.Italic)) {
                        builder.append(comment)
                    }
                    currentIndex += comment.length
                } else {
                    builder.append(remaining)
                    break
                }
            }
            // Etiket başlangıcı
            remaining.startsWith("<") -> {
                val match = tagRegex.find(remaining)
                if (match != null) {
                    builder.withStyle(SpanStyle(color = Color(0xFF0066CC), fontWeight = FontWeight.Medium)) {
                        builder.append(match.value)
                    }
                    currentIndex += match.value.length
                } else {
                    builder.append("<")
                    currentIndex++
                }
            }
            // Nitelik
            remaining.contains("=") && remaining.matches(Regex(".*\\s+[\\w-]+=.*")) -> {
                val attrMatch = attrRegex.find(remaining)
                if (attrMatch != null && attrMatch.range.first == 0) {
                    builder.withStyle(SpanStyle(color = Color(0xFFE65100))) {
                        builder.append(attrMatch.value.trimEnd('='))
                    }
                    builder.append("=")
                    currentIndex += attrMatch.value.length
                } else {
                    builder.append(remaining[0])
                    currentIndex++
                }
            }
            // String değer
            remaining.startsWith("\"") -> {
                val valueMatch = valueRegex.find(remaining)
                if (valueMatch != null && valueMatch.range.first == 0) {
                    builder.withStyle(SpanStyle(color = Color(0xFF2E7D32))) {
                        builder.append(valueMatch.value)
                    }
                    currentIndex += valueMatch.value.length
                } else {
                    builder.append("\"")
                    currentIndex++
                }
            }
            else -> {
                builder.append(remaining[0])
                currentIndex++
            }
        }
    }
    
    return builder.toAnnotatedString()
}

private fun highlightSmaliSyntaxForEditor(code: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    
    val smaliKeywords = listOf(
        ".class", ".super", ".source", ".method", ".end method", ".field",
        ".prologue", ".line", ".locals", ".param",
        "public", "private", "protected", "static", "final",
        "return", "const", "invoke-virtual", "invoke-direct", "invoke-static",
        "new-instance", "check-cast", "if-eq", "if-ne", "goto"
    )
    
    val parts = code.split(" ")
    
    parts.forEachIndexed { index, part ->
        if (index > 0) builder.append(" ")
        
        when {
            smaliKeywords.any { part.startsWith(it) || part == it } -> {
                builder.withStyle(SpanStyle(color = Color(0xFF0066CC), fontWeight = FontWeight.Medium)) {
                    builder.append(part)
                }
            }
            part.startsWith("L") && (part.contains("/") || part.contains(";")) -> {
                builder.withStyle(SpanStyle(color = Color(0xFFE65100))) {
                    builder.append(part)
                }
            }
            part.startsWith("\"") && part.endsWith("\"") -> {
                builder.withStyle(SpanStyle(color = Color(0xFF2E7D32))) {
                    builder.append(part)
                }
            }
            part.startsWith("#") -> {
                builder.withStyle(SpanStyle(color = Color(0xFF757575), fontStyle = FontStyle.Italic)) {
                    builder.append(part)
                }
            }
            part.matches(Regex("^-?\\d+$")) || part.matches(Regex("^0x[0-9a-fA-F]+$")) -> {
                builder.withStyle(SpanStyle(color = Color(0xFF7B1FA2))) {
                    builder.append(part)
                }
            }
            else -> builder.append(part)
        }
    }
    
    return builder.toAnnotatedString()
}

private fun formatSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}
