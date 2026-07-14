package com.example.sfmcregister.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sfmcregister.ui.dashboard.DashboardViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sfmcregister.R
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// ============================================================================
// CONSTANTS
// ============================================================================

private const val ROUND_SECONDS = 30
private const val ITEM_COUNT = 5

private const val RETRACT_MS_NORMAL = 1050 
private const val MISS_RETRACT_MS   = 780  
private const val EXTEND_MS         = 400
private const val ROCK_SLOWDOWN     = 2.6f

private const val MAX_ANGLE_DEG   = 55f
private const val SWING_PERIOD_S  = 4.0f
private val ANGULAR_SPEED_RAD_S   = (2f * PI.toFloat()) / SWING_PERIOD_S

private const val MIN_RADIUS_FRAC = 0.28f
private const val ITEM_HALF_WIDTH_DP = 17f
private const val CLAW_HALF_WIDTH_DP = 13f
private const val EXTRA_FORGIVENESS_DEG = 2f

private val SCORE_COIN = 10
private val SCORE_TOKEN = 50
private val SCORE_ROCK = 0

private data class Weight(val type: ItemType, val weight: Float)
private val WEIGHTS = listOf(
    Weight(ItemType.COIN, 0.55f),
    Weight(ItemType.TOKEN, 0.18f),
    Weight(ItemType.ROCK, 0.27f),
)

private data class PrizeTier(val min: Int, val text: String)
private val PRIZE_TIERS = listOf(
    PrizeTier(0,   "Nice try! You've earned 5 Poinseru credit points just for playing."),
    PrizeTier(100, "You've earned 20 Poinseru credit points and a free spin on Spin & Win!"),
    PrizeTier(250, "Great grab! You've earned 50 Poinseru credit points and an e-voucher for Kopi Kenangan."),
    PrizeTier(400, "Amazing round! You've earned 100 Poinseru credit points and an e-voucher for Yoshinoya."),
)

// ============================================================================
// MODEL
// ============================================================================

enum class ItemType { COIN, TOKEN, ROCK }

private data class GrappleItem(
    val id: Int,
    val type: ItemType,
    val angleDeg: Float,
    val radius: Float,
)

private enum class HookState { IDLE, EXTENDING, RETRACTING }

private data class FloatingText(
    val id: Int,
    val text: String,
    val x: Float,
    val y: Float,
    val isRock: Boolean,
    val createdAtMs: Long,
)

private fun pickType(): ItemType {
    val r = Random.nextFloat()
    var acc = 0f
    for (w in WEIGHTS) {
        acc += w.weight
        if (r < acc) return w.type
    }
    return ItemType.ROCK
}

private fun glyph(type: ItemType): String = when (type) {
    ItemType.COIN -> "¢"
    ItemType.TOKEN -> "★"
    ItemType.ROCK -> "▲"
}

private fun itemColor(type: ItemType): Color = when (type) {
    ItemType.COIN -> OcbcGold
    ItemType.TOKEN -> Cream
    ItemType.ROCK -> RockGrey
}

private fun polarToOffset(angleDeg: Float, radius: Float, pivotX: Float, pivotY: Float): Offset {
    val rad = Math.toRadians(angleDeg.toDouble())
    val x = pivotX + radius * sin(rad).toFloat()
    val y = pivotY + radius * cos(rad).toFloat()
    return Offset(x, y)
}

private fun maxRadiusForAngle(angleDeg: Float, pivotX: Float, armReachMax: Float, sideMargin: Float): Float {
    val rad = abs(angleDeg) * PI.toFloat() / 180f
    if (rad < 0.01f) return armReachMax
    val horizontalLimit = (pivotX - sideMargin) / sin(rad)
    return max(40f, min(armReachMax, horizontalLimit))
}

private fun generateItems(
    pivotX: Float,
    armReachMax: Float,
    sideMargin: Float,
    nextId: () -> Int,
): List<GrappleItem> {
    val totalRange = MAX_ANGLE_DEG * 2f
    val laneWidth = totalRange / ITEM_COUNT
    val jitterFrac = 0.15f

    return (0 until ITEM_COUNT).map { i ->
        val laneCenter = -MAX_ANGLE_DEG + laneWidth * (i + 0.5f)
        val angleDeg = laneCenter + (Random.nextFloat() * 2f - 1f) * laneWidth * jitterFrac
        val maxRadius = maxRadiusForAngle(angleDeg, pivotX, armReachMax, sideMargin)
        val minRadius = maxRadius * MIN_RADIUS_FRAC
        val radius = minRadius + Random.nextFloat() * (maxRadius - minRadius)
        GrappleItem(id = nextId(), type = pickType(), angleDeg = angleDeg, radius = radius)
    }
}

// ============================================================================
// COLORS & FONTS
// ============================================================================

private val PressStart2P = FontFamily(Font(R.font.press_start_2p))

private val OcbcRed      = Color(0xFFC8102E)
private val OcbcRedDark  = Color(0xFF7A0E1C) 
private val OcbcRedDeep  = Color(0xFF3C080E)
private val OcbcGold     = Color(0xFFF5B942)
private val OcbcGoldDim  = Color(0xFFB98C0A)
private val Cream        = Color(0xFFFFF6E9)
private val Ink          = Color(0xFF2B1B12)
private val RockGrey     = Color(0xFF6B6459)

// ============================================================================
// SCREEN
// ============================================================================

@Composable
fun GrappleGameScreen(
    onBackClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.sendEventImmediate(
            "page_open",
            mapOf("game_value" to "grapple")
        )
    }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var playW by remember { mutableStateOf(0f) }
    var playH by remember { mutableStateOf(0f) }
    var pivotX by remember { mutableStateOf(0f) }
    var pivotY by remember { mutableStateOf(0f) }
    var armReachMax by remember { mutableStateOf(0f) }
    val sideMarginPx = with(density) { 20.dp.toPx() }
    val itemHalfWidthPx = with(density) { ITEM_HALF_WIDTH_DP.dp.toPx() }
    val clawHalfWidthPx = with(density) { CLAW_HALF_WIDTH_DP.dp.toPx() }

    var score by remember { mutableStateOf(0) }
    var coins by remember { mutableStateOf(0) }
    var tokens by remember { mutableStateOf(0) }
    var rocks by remember { mutableStateOf(0) }
    var timeLeftMs by remember { mutableStateOf(ROUND_SECONDS * 1000L) }
    var running by remember { mutableStateOf(false) }
    var roundOver by remember { mutableStateOf(false) }

    var phase by remember { mutableStateOf(0f) }
    var hookState by remember { mutableStateOf(HookState.IDLE) }
    var clawClosed by remember { mutableStateOf(false) }
    val reach = remember { Animatable(0f) }

    var items by remember { mutableStateOf(listOf<GrappleItem>()) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingText>()) }
    var idCounter by remember { mutableStateOf(0) }
    fun nextId() = idCounter++

    fun startRound() {
        score = 0; coins = 0; tokens = 0; rocks = 0
        timeLeftMs = ROUND_SECONDS * 1000L
        phase = 0f
        hookState = HookState.IDLE
        clawClosed = false
        scope.launch { reach.snapTo(0f) }
        items = generateItems(pivotX, armReachMax, sideMarginPx, ::nextId)
        floatingTexts = emptyList()
        roundOver = false
        running = true
    }

    var started by remember { mutableStateOf(false) }

    fun addFloatingText(text: String, x: Float, y: Float, isRock: Boolean) {
        val t = FloatingText(nextId(), text, x, y, isRock, System.currentTimeMillis())
        floatingTexts = floatingTexts + t
        scope.launch {
            kotlinx.coroutines.delay(750)
            floatingTexts = floatingTexts.filter { it.id != t.id }
        }
    }

    fun resolveGrab(item: GrappleItem) {
        val pos = polarToOffset(item.angleDeg, item.radius, pivotX, pivotY)
        when (item.type) {
            ItemType.COIN -> { coins++; score += SCORE_COIN; addFloatingText("+$SCORE_COIN", pos.x, pos.y, false) }
            ItemType.TOKEN -> { tokens++; score += SCORE_TOKEN; addFloatingText("+$SCORE_TOKEN \u2605", pos.x, pos.y, false) }
            ItemType.ROCK -> { rocks++; addFloatingText("ROCK \u2014 SLOW!", pos.x, pos.y, true) }
        }
        items = generateItems(pivotX, armReachMax, sideMarginPx, ::nextId)
    }

    fun doMiss(angleDeg: Float) {
        hookState = HookState.EXTENDING
        clawClosed = false
        val swoopReach = maxRadiusForAngle(angleDeg, pivotX, armReachMax, sideMarginPx) * 0.72f
        val extendMs = (EXTEND_MS * 0.72f).roundToInt()
        val retractMs = (MISS_RETRACT_MS * 0.72f).roundToInt()

        scope.launch {
            reach.animateTo(swoopReach, tween(extendMs, easing = LinearEasing))
            val pos = polarToOffset(angleDeg, swoopReach, pivotX, pivotY)
            addFloatingText("MISS!", pos.x, pos.y, true)
            hookState = HookState.RETRACTING
            reach.animateTo(0f, tween(retractMs, easing = LinearEasing))
            hookState = HookState.IDLE
        }
    }

    fun doGrab(item: GrappleItem) {
        hookState = HookState.EXTENDING
        clawClosed = false
        val isRock = item.type == ItemType.ROCK
        val grabReach = item.radius

        val extendDepthScale = max(0.4f, grabReach / armReachMax)
        val extendMs = (EXTEND_MS * extendDepthScale).roundToInt()

        val retractDepthScale = max(0.7f, grabReach / armReachMax)
        val retractBase = if (isRock) RETRACT_MS_NORMAL * ROCK_SLOWDOWN else RETRACT_MS_NORMAL.toFloat()
        val retractMs = (retractBase * retractDepthScale).roundToInt()

        scope.launch {
            reach.animateTo(grabReach, tween(extendMs, easing = LinearEasing))
            clawClosed = true
            resolveGrab(item)
            hookState = HookState.RETRACTING
            reach.animateTo(0f, tween(retractMs, easing = LinearEasing))
            clawClosed = false
            hookState = HookState.IDLE
        }
    }

    fun onTap() {
        if (!running || roundOver) return
        if (hookState != HookState.IDLE) return
        if (items.isEmpty()) return

        val currentAngle = MAX_ANGLE_DEG * sin(phase)
        var bestIdx = 0
        var bestDist = Float.MAX_VALUE
        items.forEachIndexed { i, it ->
            val d = abs(it.angleDeg - currentAngle)
            if (d < bestDist) { bestDist = d; bestIdx = i }
        }
        val nearest = items[bestIdx]
        val combinedHalfWidthPx = itemHalfWidthPx + clawHalfWidthPx
        val toleranceDeg = Math.toDegrees(
            atan2(combinedHalfWidthPx.toDouble(), nearest.radius.toDouble())
        ).toFloat() + EXTRA_FORGIVENESS_DEG

        if (bestDist <= toleranceDeg) {
            doGrab(nearest)
        } else {
            doMiss(currentAngle)
        }
    }

    LaunchedEffect(started, armReachMax) {
        if (!started || armReachMax <= 0f) return@LaunchedEffect
        startRound()
        var lastNanos = -1L
        while (true) {
            val frameNanos = withFrameNanos { it }
            if (lastNanos < 0L) lastNanos = frameNanos
            var dtMs = (frameNanos - lastNanos) / 1_000_000f
            lastNanos = frameNanos
            if (dtMs > 40f) dtMs = 40f
            if (!running) continue

            timeLeftMs -= dtMs.toLong()
            if (timeLeftMs <= 0) {
                timeLeftMs = 0
                running = false
                roundOver = true
                continue
            }

            if (hookState == HookState.IDLE) {
                phase += ANGULAR_SPEED_RAD_S * (dtMs / 1000f)
            }
        }
    }

    // ---- UI ----
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OcbcRedDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 380.dp)
                .background(OcbcRedDark)
                .padding(top = 16.dp)
        ) {
            // ---- HUD ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                // Dashed border at the top
                Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    drawLine(
                        color = OcbcGold,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(end = 4.dp).size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = OcbcGold
                            )
                        }
                        Text(
                            "POINSERU GRAPPLE", 
                            fontFamily = PressStart2P,
                            color = OcbcGold, 
                            fontSize = 11.sp
                        )
                    }
                    val secs = ceil(timeLeftMs / 1000f).toInt()
                    Box(
                        modifier = Modifier
                            .background(Ink)
                            .border(2.dp, OcbcGold)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "$secs",
                            fontFamily = PressStart2P,
                            color = if (secs <= 5) OcbcRed else Cream,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatBox("SCORE", "$score", Modifier.weight(1f))
                    StatBox("COINS", "$coins", Modifier.weight(1f))
                    StatBox("TOKENS", "$tokens", Modifier.weight(1f))
                    StatBox("ROCKS", "$rocks", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                // Dashed border at the bottom of HUD
                Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                    drawLine(
                        color = OcbcGold,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }

            // ---- Play area ----
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTap() })
                    }
                    .onSizeChanged { size ->
                        playW = size.width.toFloat()
                        playH = size.height.toFloat()
                        pivotX = playW / 2f
                        pivotY = with(density) { 18.dp.toPx() }
                        armReachMax = max(60f, playH - pivotY - with(density) { 22.dp.toPx() })
                        if (!started) started = true
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (pivotX == 0f) return@Canvas

                    // Pivot dot
                    drawCircle(color = Ink, radius = 6f, center = Offset(pivotX, 14f))
                    drawCircle(color = OcbcGold, radius = 6f, center = Offset(pivotX, 14f), style = Stroke(width = 2f))

                    val angleDeg = MAX_ANGLE_DEG * sin(phase)
                    val armAngleRad = Math.toRadians(angleDeg.toDouble())
                    val clawTip = Offset(
                        x = pivotX + reach.value * sin(armAngleRad).toFloat(),
                        y = pivotY + reach.value * cos(armAngleRad).toFloat()
                    )

                    // Arm line (retro thick line)
                    drawLine(
                        color = Ink,
                        start = Offset(pivotX, pivotY),
                        end = clawTip,
                        strokeWidth = 6f
                    )

                    // Claw (retro pincer look)
                    val pincerSpread = if (clawClosed) 6f else 22f
                    drawLine(Ink, clawTip, Offset(clawTip.x - pincerSpread, clawTip.y - 18f), strokeWidth = 8f)
                    drawLine(Ink, clawTip, Offset(clawTip.x + pincerSpread, clawTip.y - 18f), strokeWidth = 8f)

                    // Items
                    items.forEach { item ->
                        val pos = polarToOffset(item.angleDeg, item.radius, pivotX, pivotY)
                        val boxSize = 38f
                        drawRect(
                            color = itemColor(item.type),
                            topLeft = Offset(pos.x - boxSize / 2f, pos.y - boxSize / 2f),
                            size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                        )
                        drawRect(
                            color = Ink,
                            topLeft = Offset(pos.x - boxSize / 2f, pos.y - boxSize / 2f),
                            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
                            style = Stroke(width = 4f)
                        )
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = if (item.type == ItemType.TOKEN) OcbcRedDark.toArgbInt() else Ink.toArgbInt()
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 26f
                                isFakeBoldText = true
                            }
                            drawText(glyph(item.type), pos.x, pos.y + 10f, paint)
                        }
                    }

                    // Floating score/miss text
                    floatingTexts.forEach { ft ->
                        val ageMs = System.currentTimeMillis() - ft.createdAtMs
                        val p = (ageMs / 750f).coerceIn(0f, 1f)
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = (if (ft.isRock) Cream else OcbcGold).toArgbInt()
                                alpha = ((1f - p) * 255).toInt().coerceIn(0, 255)
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11.sp.value * density.density
                                isFakeBoldText = true
                            }
                            drawText(ft.text, ft.x, ft.y - 20f - (p * 34f), paint)
                        }
                    }
                }

                Text(
                    "TAP ANYWHERE TO SWING & GRAB",
                    fontFamily = PressStart2P,
                    color = Cream.copy(alpha = 0.55f),
                    fontSize = 8.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            }
        }

        // ---- Round-over summary ----
        if (roundOver) {
            val tier = PRIZE_TIERS.lastOrNull { score >= it.min } ?: PRIZE_TIERS.first()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Ink.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Cream)
                        .border(4.dp, Ink)
                        .padding(24.dp)
                        .widthIn(max = 300.dp)
                ) {
                    Text("ROUND OVER", fontFamily = PressStart2P, color = OcbcRedDark, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("$score", fontFamily = PressStart2P, color = Ink, fontSize = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("POINSERU POINTS EARNED", fontFamily = PressStart2P, color = OcbcRed, fontSize = 8.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryBox("Coins", "$coins", Modifier.weight(1f))
                        SummaryBox("Tokens", "$tokens", Modifier.weight(1f))
                        SummaryBox("Rocks", "$rocks", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .background(OcbcRedDark)
                            .border(3.dp, Ink)
                            .padding(12.dp, 10.dp)
                            .fillMaxWidth()
                    ) {
                        Text("YOUR REWARD", fontFamily = PressStart2P, color = OcbcGold, fontSize = 9.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(tier.text, fontFamily = PressStart2P, color = Cream, fontSize = 8.sp, lineHeight = 12.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .background(OcbcGold)
                            .border(3.dp, Ink)
                            .padding(horizontal = 26.dp, vertical = 14.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { startRound() })
                            }
                    ) {
                        Text("PLAY AGAIN", fontFamily = PressStart2P, color = Ink, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Ink)
            .border(2.dp, OcbcGold)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontFamily = PressStart2P, color = OcbcGold, fontSize = 6.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, fontFamily = PressStart2P, color = Cream, fontSize = 10.sp)
    }
}

@Composable
private fun SummaryBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(OcbcGold)
            .border(2.dp, Ink)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = PressStart2P, color = Ink, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, fontFamily = PressStart2P, color = OcbcRedDark, fontSize = 7.sp)
    }
}

private fun Color.toArgbInt(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
