package com.apkpro.editor.ui.screens

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
import com.apkpro.editor.data.model.ResourceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArscViewerScreen(
    resources: ResourceInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    val tabs = listOf("Stringler", "Drawable", "Layout", "Diğer")
    
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
                )
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                0 -> StringList(resources.strings.filter { it.key.contains(searchQuery, ignoreCase = true) })
                1 -> DrawableList(resources.drawables.filter { it.contains(searchQuery, ignoreCase = true) })
                2 -> LayoutList(resources.layouts.filter { it.contains(searchQuery, ignoreCase = true) })
                3 -> OtherResourcesList(resources)
            }
        }
    }
}

@Composable
private fun ResourceStat(label: String, count: Int) {
    Column {
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun StringList(strings: Map<String, String>) {
    LazyColumn {
        items(strings.toList()) { (key, value) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        key,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        value,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawableList(drawables: List<String>) {
    LazyColumn {
        items(drawables) { drawable ->
            ListItem(
                headlineContent = { Text(drawable.substringAfterLast("/")) },
                supportingContent = { Text(drawable, fontSize = 11.sp) },
                leadingContent = {
                    Icon(Icons.Default.Image, contentDescription = null)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LayoutList(layouts: List<String>) {
    LazyColumn {
        items(layouts) { layout ->
            ListItem(
                headlineContent = { Text(layout.substringAfterLast("/")) },
                supportingContent = { Text(layout, fontSize = 11.sp) },
                leadingContent = {
                    Icon(Icons.Default.ViewCompact, contentDescription = null)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OtherResourcesList(resources: ResourceInfo) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Diğer Kaynaklar", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        Text("Raw: ${resources.rawCount} dosya")
        Text("Assets: ${resources.assetCount} dosya")
        Text("Toplam string: ${resources.strings.size}")
    }
}
