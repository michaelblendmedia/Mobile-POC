package com.example.sfmcregister.ui.dashboard

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ---------- Brand tokens ----------
private val OcbcRed = Color(0xFFC8102E)
private val OcbcRedDark = Color(0xFF7A0A1E)
private val OcbcRedDeep = Color(0xFF4E0713)
private val Gold = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFF3D98B)
private val Cream = Color(0xFFFBF7F1)

// ---------- Prize model ----------
data class Prize(val name: String, val emoji: String, val tag: String, val weight: Int)

private val prizePool = listOf(
    Prize("$8 Cash Reward", "\uD83D\uDCB5", "Instant credit", 1),
    Prize("500 OCBC$ Points", "\uD83E\uDE99", "Points reward", 1),
    Prize("$5 Grab Voucher", "\uD83D\uDE97", "Ride voucher", 1),
    Prize("Free Kopi Voucher", "\u2615", "Treat reward", 1),
    Prize("Movie Ticket \u00D71", "\uD83C\uDFAC", "Entertainment", 1),
    Prize("300 Miles (90\u00B0N)", "\u2708\uFE0F", "Travel reward", 1),
    Prize("Try Again", "\uD83C\uDF40", "No prize this time", 2),
)

private fun pickPrize(): Prize {
    val total = prizePool.sumOf { it.weight }
    var r = Random.nextInt(total)
    for (p in prizePool) {
        if (r < p.weight) return p
        r -= p.weight
    }
    return prizePool.first()
}

// ---------- Foil bitmap ----------
private fun createFoilBitmap(width: Int, height: Int): Bitmap {
    val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val w = width.toFloat()
    val h = height.toFloat()

    // base gradient
    val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(0xFFE23A52.toInt(), 0xFFC8102E.toInt(), 0xFF7A0A1E.toInt()),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w, h, gradPaint)

    // diagonal shimmer stripes
    canvas.save()
    canvas.translate(w / 2f, h / 2f)
    canvas.rotate(-30f)
    val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.White.copy(alpha = 0.12f).toArgb()
    }
    var x = -w
    while (x < w * 1.5f) {
        canvas.drawRect(x, -h * 1.5f, x + 8f, h * 1.5f, stripePaint)
        x += 26f
    }
    canvas.restore()

    // repeating brand wordmark
    canvas.save()
    canvas.translate(w / 2f, h / 2f)
    canvas.rotate(-18f)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GoldLight.copy(alpha = 0.18f).toArgb()
        textSize = 13.spToPx()
        isFakeBoldText = true
    }
    var ty = -h
    while (ty < h) {
        var tx = -w
        while (tx < w) {
            canvas.drawText("OCBC", tx, ty, textPaint)
            tx += 90f
        }
        ty += 34f
    }
    canvas.restore()

    // center coin + label
    val coinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.White.copy(alpha = 0.14f).toArgb() }
    canvas.drawCircle(w / 2f, h / 2f - 14f, 30f, coinPaint)
    val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f; textAlign = Paint.Align.CENTER }
    canvas.drawText("\uD83E\uDE99", w / 2f, h / 2f - 2f, emojiPaint)
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GoldLight.toArgb()
        textSize = 12.spToPx()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        letterSpacing = 0.12f
    }
    canvas.drawText("SCRATCH HERE", w / 2f, h / 2f + 40f, labelPaint)

    return bmp
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)
private fun Int.spToPx(): Float = this * 2.75f // rough density-independent fallback for bitmap-space text

// ---------- Scratch erase ----------
private val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = 70f
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
}
private val eraseDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
}

private fun scratchLine(bitmap: Bitmap, from: Offset, to: Offset) {
    val canvas = AndroidCanvas(bitmap)
    canvas.drawCircle(from.x, from.y, 35f, eraseDotPaint)
    canvas.drawLine(from.x, from.y, to.x, to.y, erasePaint)
    canvas.drawCircle(to.x, to.y, 35f, eraseDotPaint)
}

// downscaled alpha sampling -> percent scratched (cheap, throttled by caller)
private fun scratchedPercent(bitmap: Bitmap): Float {
    val sw = 32
    val sh = 32
    val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
    val pixels = IntArray(sw * sh)
    scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)
    var clear = 0
    for (p in pixels) {
        val alpha = (p ushr 24) and 0xFF
        if (alpha < 40) clear++
    }
    scaled.recycle()
    return clear.toFloat() / (sw * sh)
}

// ---------- Confetti ----------
private data class ConfettiPiece(val startX: Float, val color: Color, val delayMs: Int, val durationMs: Int, val size: Float)

@Composable
private fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val pieces = remember(trigger) {
        val colors = listOf(Gold, OcbcRed, GoldLight, Color.White)
        List(24) {
            ConfettiPiece(
                startX = Random.nextFloat(),
                color = colors[Random.nextInt(colors.size)],
                delayMs = Random.nextInt(300),
                durationMs = 2200 + Random.nextInt(1400),
                size = 6f + Random.nextFloat() * 6f
            )
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxW = constraints.maxWidth
        val boxH = constraints.maxHeight
        pieces.forEach { piece ->
            val progress = remember(piece) { Animatable(0f) }
            LaunchedEffect(piece) {
                delay(piece.delayMs.toLong())
                progress.animateTo(1f, animationSpec = tween(piece.durationMs, easing = LinearEasing))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                x = (piece.startX * boxW).toInt(),
                                y = (progress.value * boxH * 1.1f).toInt() - boxH / 10
                            )
                        }
                        .size(width = piece.size.dp, height = (piece.size * 0.4f).dp)
                        .background(piece.color, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

// ---------- Main screen ----------
@Composable
fun ScratchGameScreen(
    onBackClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var currentPrize by remember { mutableStateOf(pickPrize()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var foilBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var revealed by remember { mutableStateOf(false) }
    var progressPct by remember { mutableStateOf(0) }
    var redrawTick by remember { mutableStateOf(0) }
    var confettiTrigger by remember { mutableStateOf(0) }
    var newCardEnabled by remember { mutableStateOf(true) }

    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    var lastCheckMs by remember { mutableStateOf(0L) }

    fun resetCard() {
        currentPrize = pickPrize()
        revealed = false
        progressPct = 0
        confettiTrigger = 0
        newCardEnabled = false
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            foilBitmap = createFoilBitmap(canvasSize.width, canvasSize.height)
        }
        redrawTick++
        scope.launch { delay(400); newCardEnabled = true }
    }

    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && foilBitmap == null) {
            foilBitmap = createFoilBitmap(canvasSize.width, canvasSize.height)
            redrawTick++
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(OcbcRed, OcbcRedDark, OcbcRedDeep),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // eyebrow with back button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp).padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Cream
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Gold, RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "OCBC REWARDS",
                            color = GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Scratch & Win",
                    color = Cream,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Swipe the foil panel to reveal your surprise reward. New card, new odds.",
                    color = Cream.copy(alpha = 0.72f),
                    fontSize = 13.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(22.dp))

                // ticket
                Box(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.9f)
                        .background(Cream, RoundedCornerShape(22.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, OcbcRedDark.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                                .onSizeChanged { canvasSize = it }
                        ) {
                            // prize layer (always underneath)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFFDFA), Color(0xFFF6EDE0))))
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(currentPrize.emoji, fontSize = 48.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    currentPrize.name,
                                    color = OcbcRedDark,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    currentPrize.tag.uppercase(),
                                    color = Color(0xFF9A6B1F),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }

                            // foil layer on top, removed once revealed
                            if (!revealed) {
                                foilBitmap?.let { bmp ->
                                    Canvas(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(bmp) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        lastPoint = offset
                                                        scratchLine(bmp, offset, offset)
                                                        redrawTick++
                                                    },
                                                    onDragEnd = { lastPoint = null },
                                                    onDragCancel = { lastPoint = null },
                                                    onDrag = { change, _ ->
                                                        val current = change.position
                                                        val last = lastPoint ?: current
                                                        scratchLine(bmp, last, current)
                                                        lastPoint = current
                                                        redrawTick++

                                                        val now = System.currentTimeMillis()
                                                        if (now - lastCheckMs > 110) {
                                                            lastCheckMs = now
                                                            val pct = (scratchedPercent(bmp) * 100).toInt()
                                                            progressPct = pct.coerceAtMost(100)
                                                            if (progressPct >= 55 && !revealed) {
                                                                revealed = true
                                                                progressPct = 100
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                if (currentPrize.name != "Try Again") {
                                                                    confettiTrigger++
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        // reading redrawTick here ties this draw phase to bitmap mutations
                                        @Suppress("UNUSED_EXPRESSION") redrawTick
                                        drawImage(bmp.asImageBitmap())
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${if (revealed) 100 else progressPct}% revealed",
                                color = OcbcRedDark,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(OcbcRedDark.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (revealed) 1f else progressPct / 100f)
                                        .background(Brush.horizontalGradient(listOf(Gold, OcbcRed)), RoundedCornerShape(999.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { resetCard() },
                        enabled = newCardEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Cream,
                            contentColor = OcbcRedDark
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("\uD83C\uDF9F\uFE0F  New Card", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Demo game for illustration only. Not a real OCBC promotion — no purchase necessary, no actual rewards issued.",
                    color = Cream.copy(alpha = 0.45f),
                    fontSize = 10.5.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
            }

            ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
        }
    }
}
