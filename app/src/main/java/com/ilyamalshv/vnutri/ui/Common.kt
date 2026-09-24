package com.ilyamalshv.vnutri.ui

import com.ilyamalshv.vnutri.data.tr
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.Lens
import com.ilyamalshv.vnutri.sound.LocalFeedback

/** Scales content down slightly while pressed, with a soft spring back. */
@Composable
fun rememberSoftPress(scaleTo: Float = 0.97f): Pair<MutableInteractionSource, Modifier> {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleTo else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "softPress",
    )
    return interaction to Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun SoftChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val (interaction, press) = rememberSoftPress(0.92f)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        interactionSource = interaction,
        modifier = press,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Назад", "Back"))
            }
        },
        actions = { actions() },
    )
}

/** One school's view on one feeling. Collapsed shows a preview; expanded adds quote and practice. */
@Composable
fun LensCard(
    heading: String,
    subheading: String,
    lens: Lens,
    expanded: Boolean,
    onToggle: () -> Unit,
    saved: Boolean? = null,
    onToggleSave: () -> Unit = {},
) {
    val feedback = LocalFeedback.current
    val (interaction, press) = rememberSoftPress(0.98f)
    Card(
        onClick = { feedback?.tap(); onToggle() },
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().then(press),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(heading, style = MaterialTheme.typography.titleMedium)
            if (subheading.isNotBlank()) {
                Text(
                    subheading,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                lens.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (!expanded) {
                Text(
                    tr("Читать дальше", "Read more"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                lens.quote?.let { q ->
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(3.dp).fillMaxHeight(),
                        ) {}
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("«${q.text.trim('«', '»', '"')}»", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                            Text(
                                listOf(q.author, q.source).filter { it.isNotBlank() }.joinToString(", "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (lens.practice.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(tr("Попробуйте", "Try this"), style = MaterialTheme.typography.labelLarge)
                            Text(lens.practice, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (saved != null) {
                    TextButton(onClick = { feedback?.select(); onToggleSave() }, modifier = Modifier.padding(top = 4.dp)) {
                        Text(if (saved) tr("★ Мысль сохранена в дневник", "★ Thought saved to journal") else tr("☆ Сохранить эту мысль", "☆ Save this thought"))
                    }
                }
            }
        }
    }
}

/**
 * RU | EN switch in the splash's mood: a pearl thumb that flows between the two sides on a soft spring.
 * [onSplash] uses the velvet-and-cream palette of the splash; otherwise it follows the app theme.
 */
@Composable
fun LangSwitch(lang: String, onLang: (String) -> Unit, onSplash: Boolean = false) {
    val feedback = LocalFeedback.current
    val en = lang == "en"
    val progress by animateFloatAsState(
        targetValue = if (en) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 220f),
        label = "langThumb",
    )
    val cream = Color(0xFFF3E9DA)
    val track = if (onSplash) Color(0xFF1B0B0C).copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (onSplash) cream.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
    val thumbLight = if (onSplash) Color(0xFFFFFBF4) else MaterialTheme.colorScheme.primaryContainer
    val thumbDark = if (onSplash) Color(0xFFE2D5C3) else MaterialTheme.colorScheme.primaryContainer
    val onThumb = if (onSplash) Color(0xFF3A1416) else MaterialTheme.colorScheme.onPrimaryContainer
    val idle = if (onSplash) cream.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
    val width = 116.dp
    val height = 40.dp
    val pad = 4.dp
    val thumbWidth = (width - pad * 2) / 2

    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(50))
            .background(track)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                feedback?.select()
                onLang(if (en) "ru" else "en")
            },
    ) {
        // The thumb: a small pearl with a soft highlight, like a drop of the splash mass.
        Box(
            Modifier
                .padding(pad)
                .offset(x = thumbWidth * progress)
                .size(thumbWidth, height - pad * 2)
                .shadow(6.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(Brush.radialGradient(listOf(thumbLight, thumbDark), center = Offset(30f, 12f), radius = 140f)),
        )
        Row(Modifier.fillMaxSize()) {
            listOf("RU" to 0f, "EN" to 1f).forEach { (label, at) ->
                val closeness = 1f - kotlin.math.abs(progress - at).coerceIn(0f, 1f)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        color = lerp(idle, onThumb, closeness),
                    )
                }
            }
        }
    }
}

/**
 * Keeps a growing text field above the keyboard: whenever its text changes or the keyboard appears while it
 * is focused, the surrounding scroll container brings the whole field into view.
 * Pair with a bounded maxLines so the field itself scrolls to the cursor once it is tall.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun Modifier.keepAboveKeyboard(value: String): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(value, imeVisible, focused) {
        if (focused) {
            delay(150) // let the IME padding settle first
            requester.bringIntoView()
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .onFocusEvent { focused = it.isFocused }
}
