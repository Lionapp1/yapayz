package com.redex.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Android,
            title = "ReDex Pro'ya Hoş Geldiniz!",
            description = "APK ve DEX dosyalarını profesyonel şekilde analiz edin, düzenleyin ve derleyin.",
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            icon = Icons.Default.Code,
            title = "DEX Analizi",
            description = "Smali ve Java kodunu görüntüleyin. Sınıf, metod ve alanları inceleyin.",
            color = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            icon = Icons.Default.Edit,
            title = "Gelişmiş Düzenleme",
            description = "Metin dosyalarını düzenleyin, APK içeriğini modifiye edin ve yeniden paketleyin.",
            color = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            icon = Icons.Default.Update,
            title = "Otomatik Güncellemeler",
            description = "Yeni sürümler otomatik olarak bildirilir. Tek dokunuşla güncelleyin.",
            color = MaterialTheme.colorScheme.primary
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Üst boşluk
            Spacer(modifier = Modifier.height(40.dp))
            
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }
            
            // Sayfa göstergeleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            .padding(horizontal = 4.dp)
                    )
                    if (index < pages.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            
            // Butonlar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                if (pagerState.currentPage == pages.size - 1) {
                    // Son sayfa - Başla butonu
                    Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Başlayalım",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Diğer sayfalar - Devam butonu
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Devam",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Atlama butonu
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Atla")
                }
            } else {
                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İkon
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(page.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(70.dp),
                tint = page.color
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Başlık
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Açıklama
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
