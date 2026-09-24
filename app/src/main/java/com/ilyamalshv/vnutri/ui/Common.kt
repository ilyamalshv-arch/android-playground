package com.ilyamalshv.vnutri.ui

import androidx.compose.animation.core.animateFloatAsState
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                    "Читать дальше",
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
                            Text("Попробуйте", style = MaterialTheme.typography.labelLarge)
                            Text(lens.practice, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (saved != null) {
                    TextButton(onClick = { feedback?.select(); onToggleSave() }, modifier = Modifier.padding(top = 4.dp)) {
                        Text(if (saved) "★ Мысль сохранена в дневник" else "☆ Сохранить эту мысль")
                    }
                }
            }
        }
    }
}
