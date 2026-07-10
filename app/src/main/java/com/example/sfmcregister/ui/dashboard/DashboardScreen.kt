package com.example.sfmcregister.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sfmcregister.ui.theme.OcbcRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onTransferClick: () -> Unit = {},
    onQrisClick: () -> Unit = {},
    onKartuClick: () -> Unit = {},
    onGamificationClick: () -> Unit = {},
    onPuzzleClick: () -> Unit = {},
    onGrappleClick: () -> Unit = {},
    onScratchClick: () -> Unit = {}
) {
    Scaffold(
        bottomBar = { 
            DashboardBottomNav(
                onTransferClick = onTransferClick,
                onQrisClick = onQrisClick,
                onKartuClick = onKartuClick
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section (Red)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = OcbcRed,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selamat pagi, ${viewModel.firstName}",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Balance Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tabungan - 123-456-7890", fontSize = 12.sp, color = Color.Gray)
                            Text("Rp 42.580.000", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = onTransferClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OcbcRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OcbcRed)
                                ) {
                                    Text("Transfer")
                                }
                                OutlinedButton(
                                    onClick = onKartuClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OcbcRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OcbcRed)
                                ) {
                                    Text("Kartu")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Menu Section
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Menu cepat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                
                val menus = listOf(
                    "Transfer" to Icons.Default.Send,
                    "QRIS" to Icons.Default.QrCode,
                    "Location" to Icons.Default.LocationOn,
                    "Inbox" to Icons.Default.Mail,
                    "Deposito" to Icons.Default.AccountBalanceWallet,
                    "Events" to Icons.Default.Star,
                    "Kartu" to Icons.Default.CreditCard,
                    "Lainnya" to Icons.Default.MoreHoriz
                )
                
                // Grid representation
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        menus.take(4).forEach { (label, icon) ->
                            QuickMenuItem(label, icon, onClick = {
                                viewModel.trackEvent("Menu_Clicked_$label")
                                when(label) {
                                    "Transfer" -> onTransferClick()
                                    "QRIS" -> onQrisClick()
                                    "Kartu" -> onKartuClick()
                                    "Events" -> onGamificationClick()
                                    "Inbox" -> onPuzzleClick()
                                    "Location" -> onGrappleClick()
                                    "Deposito" -> onScratchClick()
                                }
                            })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        menus.drop(4).forEach { (label, icon) ->
                            QuickMenuItem(label, icon, onClick = {
                                viewModel.trackEvent("Menu_Clicked_$label")
                                when(label) {
                                    "Transfer" -> onTransferClick()
                                    "QRIS" -> onQrisClick()
                                    "Kartu" -> onKartuClick()
                                    "Events" -> onGamificationClick()
                                    "Inbox" -> onPuzzleClick()
                                    "Location" -> onGrappleClick()
                                    "Deposito" -> onScratchClick()
                                }
                            })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text("Promo untuk kamu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Carousel
                val pagerState = rememberPagerState(pageCount = { 3 })
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                tint = OcbcRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Promo Makan di Solaria ${page + 1}", fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("Cashback 50% setiap hari Jumat", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { iteration ->
                        val color = if (pagerState.currentPage == iteration) OcbcRed else Color.LightGray
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickMenuItem(label: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = OcbcRed)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun DashboardBottomNav(
    onTransferClick: () -> Unit = {},
    onQrisClick: () -> Unit = {},
    onKartuClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = OcbcRed
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Beranda") },
            label = { Text("Beranda") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OcbcRed,
                selectedTextColor = OcbcRed,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color(0xFFFFF0F0)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Send, contentDescription = "Transfer") },
            label = { Text("Transfer") },
            selected = false,
            onClick = onTransferClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.QrCode, contentDescription = "QRIS") },
            label = { Text("QRIS") },
            selected = false,
            onClick = onQrisClick
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.CreditCard, contentDescription = "Kartu") },
            label = { Text("Kartu") },
            selected = false,
            onClick = onKartuClick
        )
    }
}
