package com.redex.pro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redex.pro.data.model.ManifestInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManifestViewerScreen(
    manifest: ManifestInfo?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AndroidManifest.xml") },
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
        if (manifest != null) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    // Manifest kodu
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Manifest Kodu",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                generateManifestCode(manifest),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                
                // Aktiviteler
                if (manifest.activities.isNotEmpty()) {
                    item {
                        Text(
                            "Aktiviteler (${manifest.activities.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(manifest.activities) { activity ->
                        ActivityItem(activity)
                    }
                }
                
                // İzinler
                if (manifest.usesPermissions.isNotEmpty()) {
                    item {
                        Text(
                            "Kullanılan İzinler (${manifest.usesPermissions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    items(manifest.usesPermissions.take(20)) { permission ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                permission,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Manifest bilgisi bulunamadı")
            }
        }
    }
}

@Composable
private fun ActivityItem(activity: com.redex.pro.data.model.ActivityInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.name.substringAfterLast("."),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    activity.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (activity.isMain) {
                Chip("MAIN")
            }
            if (activity.isLauncher) {
                Chip("LAUNCHER")
            }
            if (activity.exported) {
                Chip("EXPORTED", Color(0xFFFFA726))
            }
        }
    }
}

@Composable
private fun Chip(label: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = color
        )
    }
}

private fun generateManifestCode(manifest: ManifestInfo): String {
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"")
        appendLine("    package=\"${manifest.packageName}\"")
        appendLine("    android:versionName=\"${manifest.versionName}\"")
        appendLine("    android:versionCode=\"${manifest.versionCode}\">")
        appendLine()
        appendLine("    <uses-sdk")
        appendLine("        android:minSdkVersion=\"${manifest.minSdk}\"")
        appendLine("        android:targetSdkVersion=\"${manifest.targetSdk}\" />")
        appendLine()
        
        manifest.usesPermissions.take(5).forEach { perm ->
            appendLine("    <uses-permission android:name=\"$perm\" />")
        }
        if (manifest.usesPermissions.size > 5) {
        appendLine("    <!-- +${manifest.usesPermissions.size - 5} izin daha... -->")
        }
        appendLine()
        
        appendLine("    <application>")
        manifest.activities.take(3).forEach { activity ->
            appendLine("        <activity android:name=\"${activity.name}\" />")
        }
        if (manifest.activities.size > 3) {
            appendLine("        <!-- +${manifest.activities.size - 3} aktivite daha... -->")
        }
        appendLine("    </application>")
        appendLine()
        appendLine("</manifest>")
    }
}
