package com.ilyamalshv.vnutri.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The splash's world, carried through the app: velvet, pearl, and coloured light. */
object Lodge {
    val velvet = Color(0xFF140708)
    val velvetHigh = Color(0xFF2A0E11)
    val pearl = Color(0xFFF3E9DA)
    val pearlDim = Color(0xFFCDB8A6)
    val gold = Color(0xFFE9C9A0)
    val ember = Color(0xFFFF7A6B)

    /** Each group of feelings has its own light. */
    val groupColors = mapOf(
        "fear" to Color(0xFF8C9BFF),
        "loss" to Color(0xFF63C9DA),
        "others" to Color(0xFFFF6B5E),
        "self" to Color(0xFFFFB84D),
        "drained" to Color(0xFFB9ADE0),
        "body" to Color(0xFFFF63A8),
        "awkward" to Color(0xFFA6E77F),
        "light" to Color(0xFFFFDF8A),
    )

    fun colorOfGroup(group: String?): Color = groupColors[group] ?: gold

    /** The mood of the current screen: tints the moving background lights. */
    var mood by mutableStateOf(gold)
}

/** Slow velvet folds and drifting lights behind every screen, tinted by [Lodge.mood]. */
@Composable
fun VelvetBackground(modifier: Modifier = Modifier) {
    val tint by animateColorAsState(Lodge.mood, tween(1600), label = "mood")
    val t by rememberInfiniteTransition(label = "velvet").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(48_000, easing = LinearEasing)),
        label = "t",
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1C0A0C), Lodge.velvet, Color(0xFF0C0405))))
        // Curtain folds, barely moving.
        val folds = 9
        val stops = (0..folds * 4).map { i ->
            val x = i / (folds * 4f)
            val k = 0.5f + 0.5f * sin(x * folds * 2 * PI.toFloat() + t * 2 * PI.toFloat())
            x to Color(0.55f, 0.06f, 0.08f, 0.05f + 0.07f * k)
        }
        drawRect(Brush.horizontalGradient(*stops.toTypedArray()))
        // Drifting lights: three in the mood colour, one pearl.
        val a = t * 2 * PI.toFloat()
        val lights = listOf(
            Triple(Offset(w * (0.25f + 0.12f * sin(a)), h * (0.22f + 0.08f * cos(a * 2))), w * 0.7f, tint.copy(alpha = 0.22f)),
            Triple(Offset(w * (0.8f + 0.1f * cos(a * 3)), h * (0.55f + 0.1f * sin(a))), w * 0.6f, tint.copy(alpha = 0.16f)),
            Triple(Offset(w * (0.4f + 0.2f * sin(a * 2)), h * (0.9f + 0.05f * cos(a))), w * 0.8f, tint.copy(alpha = 0.12f)),
            Triple(Offset(w * (0.65f + 0.15f * cos(a)), h * (0.12f + 0.06f * sin(a * 3))), w * 0.45f, Lodge.pearl.copy(alpha = 0.06f)),
        )
        lights.forEach { (c, r, col) -> drawCircle(Brush.radialGradient(listOf(col, Color.Transparent), c, r), r, c) }
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), center, size.maxDimension * 0.8f))
    }
}

/**
 * A soft light blooms from the exact point of touch and fades, and the element sinks a little and springs
 * back. Does not consume the touch, so clickable elements underneath keep working.
 */
fun Modifier.glowTouch(color: Color = Lodge.pearl, shape: Shape = RoundedCornerShape(18.dp), sink: Float = 0.97f): Modifier = composed {
    val scope = rememberCoroutineScope()
    val glow = remember { Animatable(0f) }
    val radius = remember { Animatable(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) sink else 1f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "sink")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                center = down.position
                pressed = true
                scope.launch { glow.snapTo(1f) }
                scope.launch {
                    radius.snapTo(size.width * 0.05f)
                    radius.animateTo(size.width * 1.3f, tween(900, easing = FastOutSlowInEasing))
                }
                waitForUpOrCancellation(PointerEventPass.Initial)
                pressed = false
                scope.launch { glow.animateTo(0f, tween(800)) }
            }
        }
        .drawWithContent {
            drawContent()
            val r = radius.value
            if (glow.value > 0.01f && r > 1f) {
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = 0.42f * glow.value), color.copy(alpha = 0.12f * glow.value), Color.Transparent), center, r),
                    r,
                    center,
                )
            }
        }
}

/** Fades and floats an item in, staggered by its position in a list. */
fun Modifier.appear(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L * index.coerceAtMost(8))
        progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 40f
    }
}

/** A thin pearl edge with a highlight that slowly travels around it. */
fun Modifier.pearlEdge(shape: Shape = RoundedCornerShape(18.dp), glowColor: Color = Lodge.pearl): Modifier = composed {
    val t by rememberInfiniteTransition(label = "edge").animateFloat(
        0f, 1f, infiniteRepeatable(tween(7_000, easing = LinearEasing)), label = "edgeT",
    )
    border(
        1.dp,
        Brush.linearGradient(
            listOf(glowColor.copy(alpha = 0.08f), glowColor.copy(alpha = 0.55f), glowColor.copy(alpha = 0.08f)),
            start = Offset(-400f + 1400f * t, 0f),
            end = Offset(-100f + 1400f * t, 300f),
        ),
        shape,
    )
}

/** Deep translucent velvet for cards, lit from the upper left. */
fun velvetFill(tint: Color = Lodge.pearl): Brush = Brush.linearGradient(
    listOf(tint.copy(alpha = 0.10f), Color(0xFF2A0E11).copy(alpha = 0.72f), Color(0xFF16080A).copy(alpha = 0.82f)),
)

/** A feeling chip that lights up in its group's colour and bounces when chosen. */
@Composable
fun GlowChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val lit by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 300f), label = "lit")
    Box(
        Modifier
            .padding(vertical = 3.dp)
            .graphicsLayer { scaleX = 1f + 0.05f * lit; scaleY = 1f + 0.05f * lit }
            .drawWithContent {
                if (lit > 0.01f) {
                    drawRoundRect(
                        Brush.radialGradient(listOf(color.copy(alpha = 0.35f * lit), Color.Transparent), center, size.width * 0.9f),
                        topLeft = Offset(-size.width * 0.15f, -size.height * 0.4f),
                        size = androidx.compose.ui.geometry.Size(size.width * 1.3f, size.height * 1.8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                    )
                }
                drawContent()
            }
            .glowTouch(color, shape, sink = 0.92f)
            .background(if (selected) color.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) color.copy(alpha = 0.85f) else Lodge.pearl.copy(alpha = 0.18f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 14.sp, color = if (selected) Lodge.pearl else Lodge.pearlDim)
    }
}

/** The main action: a pearl with a warm glow. */
@Composable
fun PearlButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, glow: Color = Lodge.gold) {
    val shape = RoundedCornerShape(50)
    val breath by rememberInfiniteTransition(label = "pearl").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath",
    )
    Box(
        modifier
            .height(54.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .drawWithContent {
                if (enabled) {
                    drawRoundRect(
                        Brush.radialGradient(listOf(glow.copy(alpha = 0.35f * breath), Color.Transparent), center, size.width * 0.7f),
                        topLeft = Offset(-24f, -24f),
                        size = androidx.compose.ui.geometry.Size(size.width + 48f, size.height + 48f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                    )
                }
                drawContent()
            }
            .glowTouch(Color.White, shape, sink = 0.95f)
            .background(Brush.radialGradient(listOf(Color(0xFFFFFBF4), Color(0xFFE8DAC6), Color(0xFFCDB89E)), Offset(120f, 20f), 700f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 19.sp, color = Color(0xFF3A1416))
    }
}

/** A breathing sphere of light in a feeling's colour, with its name in the middle. */
@Composable
fun EmotionOrb(name: String, color: Color, size: Dp = 150.dp) {
    val pulse by rememberInfiniteTransition(label = "orb").animateFloat(
        0.9f, 1.06f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val tint by animateColorAsState(color, tween(900), label = "orbColor")
    Box(Modifier.fillMaxWidth().height(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2 * pulse
            drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.18f), Color.Transparent), center, r * 1.4f), r * 1.4f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), tint.copy(alpha = 0.45f), tint.copy(alpha = 0.1f)), Offset(center.x - r * 0.3f, center.y - r * 0.35f), r), r * 0.62f)
        }
        Text(
            name,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = 24.sp,
            color = Lodge.pearl,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
