package com.ilyamalshv.vnutri.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A quiet threshold before the app: a circle that breathes (4 s in, 6 s out),
 * with a line of guidance. Tap anywhere or "Войти" to continue.
 */
@Composable
fun IntroScreen(onEnter: () -> Unit) {
    val breath = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    var inhale by remember { mutableStateOf(true) }
    var cycles by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(2500, easing = LinearEasing)) }
        while (true) {
            inhale = true
            breath.animateTo(1f, tween(4000, easing = FastOutSlowInEasing))
            inhale = false
            breath.animateTo(0f, tween(6000, easing = FastOutSlowInEasing))
            cycles++
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onEnter() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(320.dp).alpha(fade.value)) {
            val base = size.minDimension / 2
            val r = base * (0.45f + 0.4f * breath.value)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primary.copy(alpha = 0.35f), secondary.copy(alpha = 0.12f), Color.Transparent),
                    center = center,
                    radius = r * 1.6f,
                ),
                radius = r * 1.6f,
            )
            drawCircle(color = primary.copy(alpha = 0.18f + 0.12f * breath.value), radius = r)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.alpha(fade.value),
        ) {
            Text("Внутри", style = MaterialTheme.typography.displaySmall)
            Text(
                if (inhale) "вдох…" else "выдох…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Сделайте пару вдохов вместе с кругом.\nНикуда не нужно спешить.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(fade.value),
            )
            Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
                AnimatedVisibility(visible = cycles >= 1, enter = fadeIn(tween(1500))) {
                    OutlinedButton(onClick = onEnter) { Text("Войти") }
                }
            }
        }
    }
}
