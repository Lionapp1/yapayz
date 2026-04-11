package com.redex.pro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ResourceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArscViewerScreen(
    resources: ResourceInfo,
    onBack: () -> Unit,
    onEditResource: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedResource by remember { mutableStateOf<String?>(null) }
    var resourceValue by remember { mutableStateOf("") }
    
    val tabs = listOf("Stringler", "Drawable", "Layout", "Diğer")
    
    // Android fiziksel geri tuşu desteği
    BackHandler(enabled = true) {
        onBack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("resources.arsc") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            // Arama
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Kaynak ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            
            // İstatistik kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResourceStat("String", resources.stringCount)
                        ResourceStat("Drawable", resources.drawableCount)
                        ResourceStat("Layout", resources.layoutCount)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResourceStat("Color", resources.colorCount)
                        ResourceStat("Raw", resources.rawCount)
                        ResourceStat("Asset", resources.assetCount)
                    }
                }
            }
            
            // Tablar
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // İçerik
            when (selectedTab) {
                0 -> ResourceList(resources.strings, searchQuery, onEdit = { key, value ->
                    selectedResource = key
                    resourceValue = value
                    showEditDialog = true
                })
                1 -> ResourceList(resources.drawables.map { it to "" }, searchQuery, isEditable = false)
                2 -> ResourceList(resources.layouts.map { it to "" }, searchQuery, isEditable = false)
                else -> Text("Diğer kaynaklar")
            }
        }
    }
    
    // Düzenleme dialogu
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Kaynağı Düzenle") },
            text = {
                Column {
                    Text("Anahtar: $selectedResource")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resourceValue,
                        onValueChange = { resourceValue = it },
                        label = { Text("Değer") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedResource?.let { onEditResource(it, resourceValue) }
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
private fun ResourceStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ResourceList(
    items: List<Pair<String, String>>,
    searchQuery: String,
    isEditable: Boolean = true,
    onEdit: (String, String) -> Unit = { _, _ -> }
) {
    val filteredItems = if (searchQuery.isEmpty()) {
        items
    } else {
        items.filter { it.first.contains(searchQuery, ignoreCase = true) }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(filteredItems) { (key, value) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            value.take(50) + if (value.length > 50) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (isEditable) {
                        IconButton(onClick = { onEdit(key, value) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                        }
                    }
                }
            }
        }
    }
}
