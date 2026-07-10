package com.example.sfmcregister.ui.dashboard

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sfmcregister.ui.theme.OcbcRed
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(onBackClick: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("Putar roda untuk mendapat poin!") }

    val rotation = remember { Animatable(0f) }

    val items = listOf("Zonk", "10 Poin", "50 Poin", "100 Poin", "500 Poin", "1.000 Poin")
    val sliceColors = listOf(OcbcRed, Color(0xFFFFC107), Color.White, OcbcRed, Color(0xFFFFC107), Color.White)
    val textColors = listOf(Color.White, Color.Black, Color.Black, Color.White, Color.Black, Color.Black)
    val sweepAngle = 360f / items.size

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Coba Keberuntungan Anda!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier.size(300.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // The Wheel
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                ) {
                    val radius = size.width / 2
                    val center = Offset(x = size.width / 2, y = size.height / 2)

                    rotate(rotation.value, center) {
                        for (i in items.indices) {
                            val startAngle = i * sweepAngle
                            drawArc(
                                color = sliceColors[i % sliceColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )

                            // Draw text
                            val paint = Paint().apply {
                                color = textColors[i % textColors.size].toArgb()
                                textSize = 40f
                                textAlign = Paint.Align.CENTER
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            }
                            
                            val textAngle = startAngle + (sweepAngle / 2)
                            rotate(textAngle, center) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    items[i],
                                    center.x + radius * 0.65f, // Geser teks agak ke pinggir
                                    center.y + 15f,
                                    paint
                                )
                            }
                        }
                    }
                }

                // The Pointer (Jarum)
                Canvas(modifier = Modifier.size(40.dp)) {
                    val pointerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width / 2, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(
                        path = pointerPath,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (!isSpinning) {
                        isSpinning = true
                        resultText = "Memutar..."
                        coroutineScope.launch {
                            val randomSpins = Random.nextInt(5, 10)
                            val randomAngle = Random.nextFloat() * 360f
                            val targetRotation = rotation.value + (randomSpins * 360f) + randomAngle

                            rotation.animateTo(
                                targetValue = targetRotation,
                                animationSpec = tween(
                                    durationMillis = 4000,
                                    easing = FastOutSlowInEasing
                                )
                            )

                            // Kalkulasi pemenang
                            val normalizedRotation = targetRotation % 360f
                            var pointerAngle = (270f - normalizedRotation) % 360f
                            if (pointerAngle < 0) pointerAngle += 360f

                            val winningIndex = (pointerAngle / sweepAngle).toInt() % items.size
                            val prize = items[winningIndex]

                            resultText = if (prize == "Zonk") "Yah, coba lagi besok!" else "Selamat! Anda mendapat $prize!"
                            isSpinning = false
                        }
                    }
                },
                enabled = !isSpinning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OcbcRed)
            ) {
                Text("Putar Roda", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = resultText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (resultText.contains("Selamat")) Color(0xFF4CAF50) else Color.Black
            )
        }
    }
}
