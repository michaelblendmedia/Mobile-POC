package com.example.sfmcregister.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sfmcregister.ui.theme.OcbcRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(onBackClick: () -> Unit) {
    var isSpinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("Tunggu putaran...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamification - Spin & Win", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OcbcRed)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Coba Keberuntungan Anda!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Mock Wheel (Just a circle)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(if (isSpinning) Color.LightGray else OcbcRed, shape = RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSpinning) "Memutar..." else "SPIN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isSpinning = true
                    resultText = "Memutar..."
                },
                enabled = !isSpinning,
                colors = ButtonDefaults.buttonColors(containerColor = OcbcRed)
            ) {
                Text("Putar Roda")
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!isSpinning && resultText != "Tunggu putaran...") {
                Text(
                    text = resultText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            delay(2000)
            resultText = "Selamat! Anda mendapat 1.000 Poin!"
            isSpinning = false
        }
    }
}
