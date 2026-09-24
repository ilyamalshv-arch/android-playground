package com.ilyamalshv.vnutri.ui

import com.ilyamalshv.vnutri.data.tr
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.Quote
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val SPLASH_EMOTIONS = listOf("joy", "hope", "calm", "gratitude", "awe", "inspiration", "relief", "love", "pride", "melancholy")

/** A short, light quote from the library, different on every launch. */
fun pickSplashQuote(library: Library): Quote? =
    library.schools.flatMap { s -> SPLASH_EMOTIONS.mapNotNull { s.lenses[it]?.quote } }
        .filter { it.text.length in 12..120 }
        .randomOrNull()

/**
 * A thick, glossy, pearl-white mass covers the screen; the finger wipes it away like wet paint,
 * pushing ridges aside, and it slowly flows back when left alone. Underneath: a red room.
 * The mass is a height field simulated on a grid and lit per pixel (diffuse + specular).
 */
private class Mass(val w: Int, val h: Int) {
    var height = FloatArray(w * h)
    private var scratch = FloatArray(w * h)
    private val rest = FloatArray(w * h)
    val pixels = IntArray(w * h)
    val bitmap: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    var melting = false

    init {
        val rnd = Random(System.nanoTime())
        val waves = List(7) { floatArrayOf(rnd.nextFloat() * 0.09f + 0.02f, rnd.nextFloat() * 0.09f + 0.02f, rnd.nextFloat() * 6.28f, rnd.nextFloat() * 0.08f + 0.03f) }
        for (y in 0 until h) for (x in 0 until w) {
            var v = 1f
            for (wv in waves) v += wv[3] * sin(x * wv[0] + y * wv[1] + wv[2])
            rest[y * w + x] = v
            height[y * w + x] = v
        }
    }

    /** Wipe along a segment; removed material piles up in a ridge around the brush. */
    fun wipe(x0: Float, y0: Float, x1: Float, y1: Float, radius: Float) {
        val len = hypot(x1 - x0, y1 - y0)
        val steps = max(1, (len / (radius * 0.35f)).toInt())
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            stamp(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, radius)
        }
    }

    private fun stamp(cx: Float, cy: Float, r: Float) {
        val outer = r * 1.45f
        val minX = max(1, (cx - outer).toInt()); val maxX = min(w - 2, (cx + outer).toInt() + 1)
        val minY = max(1, (cy - outer).toInt()); val maxY = min(h - 2, (cy + outer).toInt() + 1)
        var removed = 0f
        var ringWeight = 0f
        for (y in minY..maxY) for (x in minX..maxX) {
            val d = hypot(x - cx, y - cy) / r
            if (d < 1f) {
                val k = (1f - d * d).let { it * it } * 0.35f
                val i = y * w + x
                val take = height[i] * k
                height[i] -= take
                removed += take
            } else if (d < 1.45f) {
                ringWeight += 1f - abs(d - 1.2f) / 0.25f
            }
        }
        if (ringWeight <= 0f) return
        val share = removed * 0.85f / ringWeight
        for (y in minY..maxY) for (x in minX..maxX) {
            val d = hypot(x - cx, y - cy) / r
            if (d in 1f..1.45f) height[y * w + x] += share * (1f - abs(d - 1.2f) / 0.25f)
        }
    }

    /** Viscous smoothing, slow healing towards the resting surface, or melting away. */
    fun step() {
        val src = height; val dst = scratch
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            if (x == 0 || y == 0 || x == w - 1 || y == h - 1) { dst[i] = src[i]; continue }
            val lap = src[i - 1] + src[i + 1] + src[i - w] + src[i + w] - 4f * src[i]
            var v = src[i] + 0.11f * lap
            v = if (melting) v * 0.93f else v + (rest[i] - v) * 0.0012f
            dst[i] = max(0f, v)
        }
        height = dst; scratch = src
    }

    fun coverage(): Float {
        var covered = 0
        for (v in height) if (v > 0.25f) covered++
        return covered / height.size.toFloat()
    }

    fun render() {
        val hf = height
        // Light from the upper left, viewer straight on; half vector precomputed.
        val lx = -0.45f; val ly = -0.62f; val lz = 0.64f
        val hxv = lx; val hyv = ly; val hzv = lz + 1f
        val hn = sqrt(hxv * hxv + hyv * hyv + hzv * hzv)
        val hx = hxv / hn; val hy = hyv / hn; val hz = hzv / hn
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            val v = hf[i]
            val a = ((v - 0.03f) / 0.22f).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
            if (a <= 0f) { pixels[i] = 0; continue }
            val l = hf[if (x > 0) i - 1 else i]; val r = hf[if (x < w - 1) i + 1 else i]
            val u = hf[if (y > 0) i - w else i]; val d = hf[if (y < h - 1) i + w else i]
            val nx = (l - r) * 5f; val ny = (u - d) * 5f
            val inv = 1f / sqrt(nx * nx + ny * ny + 1f)
            val nX = nx * inv; val nY = ny * inv; val nZ = inv
            val diffuse = max(0f, nX * lx + nY * ly + nZ * lz)
            var s = max(0f, nX * hx + nY * hy + nZ * hz)
            s *= s; s *= s; s *= s; s *= s; s *= s // ^32: a wet, glossy highlight
            val cavity = ((l + r + u + d) * 0.25f - v) * 1.6f // valleys a touch darker
            val shade = 0.52f + 0.52f * diffuse - cavity
            val warm = (v - 1f) * 0.06f
            val rr = (0.95f * shade + 0.75f * s + warm).coerceIn(0f, 1f)
            val gg = (0.925f * shade + 0.75f * s).coerceIn(0f, 1f)
            val bb = (0.885f * shade + 0.78f * s - warm).coerceIn(0f, 1f)
            pixels[i] = ((a * 255).toInt() shl 24) or ((rr * 255).toInt() shl 16) or ((gg * 255).toInt() shl 8) or (bb * 255).toInt()
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}

@Composable
fun SplashScreen(quote: Quote?, lang: String, onLang: (String) -> Unit, onEnter: () -> Unit) {
    val view = LocalView.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var mass by remember { mutableStateOf<Mass?>(null) }
    var frame by remember { mutableIntStateOf(0) }
    var time by remember { mutableFloatStateOf(0f) }
    var ready by remember { mutableStateOf(false) }
    val intro = remember { Animatable(0f) }
    val quoteAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1800)) }
    LaunchedEffect(Unit) { quoteAlpha.animateTo(1f, tween(2500, delayMillis = 1200)) }

    LaunchedEffect(size) {
        if (size.width == 0) return@LaunchedEffect
        val gw = 200
        val gh = (gw * size.height / size.width.toFloat()).toInt().coerceIn(200, 480)
        val m = Mass(gw, gh)
        mass = m
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time += (now - last) / 1e9f
                last = now
                m.step()
                m.render()
                frame++
            }
            if (frame % 30 == 0 && !m.melting && m.coverage() < 0.62f) {
                m.melting = true
                ready = true
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(size, mass) {
                val m = mass ?: return@pointerInput
                val sx = m.w / size.width.toFloat()
                val sy = m.h / size.height.toFloat()
                val radius = m.w * 0.075f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var prev = down.position
                    var travelled = 0f
                    m.wipe(prev.x * sx, prev.y * sy, prev.x * sx, prev.y * sy, radius)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val p = change.position
                        m.wipe(prev.x * sx, prev.y * sy, p.x * sx, p.y * sy, radius)
                        travelled += hypot(p.x - prev.x, p.y - prev.y)
                        if (travelled > 70f) {
                            travelled = 0f
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        prev = p
                        if (change.positionChange() != Offset.Zero) change.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawRedRoom(time) }
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Sincerer",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 56.sp,
                color = Color(0xFFF3E9DA),
            )
            Text(
                tr("искренность начинается внутри", "sincerity begins within"),
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                color = Color(0xFFE6D3C0).copy(alpha = 0.85f),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            frame // redraw on every simulation frame
            val m = mass ?: return@Canvas
            drawImage(
                m.bitmap.asImageBitmap(),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(m.w, m.h),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
                alpha = intro.value,
                filterQuality = FilterQuality.High,
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    center = center,
                    radius = this.size.maxDimension * 0.75f,
                ),
            )
        }

        Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)) {
            LangSwitch(lang, onLang, onSplash = true)
        }

        TextButton(
            onClick = onEnter,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
        ) { Text(tr("Пропустить", "Skip"), color = Color(0xFF6B5E55)) }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ready) {
                OutlinedButton(onClick = onEnter, modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(tr("Войти", "Enter"), color = Color(0xFFF3E9DA))
                }
            }
            quote?.let { q ->
                val drift = sin(time * 0.6f) * 6f
                Surface(
                    color = Color(0xFF1B0B0C).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .alpha(quoteAlpha.value * (0.85f + 0.15f * sin(time * 0.8f)))
                        .padding(top = drift.coerceAtLeast(0f).dp, bottom = (-drift).coerceAtLeast(0f).dp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "«${q.text.trim('«', '»', '"')}»",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            color = Color(0xFFF3E9DA),
                            textAlign = TextAlign.Center,
                        )
                        Text(q.author, fontSize = 12.sp, color = Color(0xFFD9C3AE), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/** Velvet curtain folds and a zigzag floor, breathing very slightly. */
private fun DrawScope.drawRedRoom(time: Float) {
    val w = size.width; val h = size.height
    val floorTop = h * 0.74f
    val folds = 13
    val stops = (0..folds * 4).map { i ->
        val t = i / (folds * 4f)
        val phase = sin(t * folds * 2 * PI.toFloat() + time * 0.15f)
        val k = 0.5f + 0.5f * phase
        t to Color(0.26f + 0.36f * k, 0.02f + 0.03f * k, 0.03f + 0.04f * k)
    }
    drawRect(Brush.horizontalGradient(*stops.toTypedArray()), size = Size(w, floorTop))
    // Light falling from above onto the curtains.
    drawRect(
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.35f)), endY = floorTop),
        size = Size(w, floorTop),
    )
    // Chevron floor.
    drawRect(Color(0xFFEDE6DA), topLeft = Offset(0f, floorTop), size = Size(w, h - floorTop))
    val band = (h - floorTop) / 7f
    val tooth = w / 9f
    for (b in 0 until 8 step 2) {
        val y0 = floorTop + b * band
        val path = Path().apply {
            moveTo(-tooth, y0)
            var x = -tooth
            var up = true
            while (x <= w + tooth) {
                x += tooth / 2
                lineTo(x, if (up) y0 + band * 0.5f else y0)
                up = !up
            }
            x = w + tooth
            val y1 = y0 + band
            lineTo(x, y1)
            up = true
            while (x >= -tooth) {
                x -= tooth / 2
                lineTo(x, if (up) y1 + band * 0.5f else y1)
                up = !up
            }
            close()
        }
        drawPath(path, Color(0xFF15110F))
    }
    drawRect(
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent), startY = floorTop, endY = floorTop + band * 2),
        topLeft = Offset(0f, floorTop),
        size = Size(w, band * 2),
    )
}
