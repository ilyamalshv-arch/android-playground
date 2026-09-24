package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.EmotionGuess
import com.ilyamalshv.vnutri.data.EmotionMark
import com.ilyamalshv.vnutri.data.Emotions
import com.ilyamalshv.vnutri.data.Safety
import com.ilyamalshv.vnutri.data.intensityLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    marks: List<EmotionMark>,
    onToggle: (String) -> Unit,
    onIntensity: (String, Int) -> Unit,
    note: String,
    onNote: (String) -> Unit,
    onSubmit: (suggested: List<String>) -> Unit,
    aiAvailable: Boolean,
    aiIds: List<String>?,
    aiLoading: Boolean,
    onAiClassify: () -> Unit,
    onJournal: () -> Unit,
    onLibrary: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sincerer") },
                actions = {
                    TextButton(onClick = onJournal) { Text("Дневник") }
                    TextButton(onClick = onLibrary) { Text("Школы") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Настройки") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Что вы чувствуете сейчас?", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Можно выбрать несколько. Любое чувство имеет право быть.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            val selectedIds = marks.map { it.emotionId }.toSet()
            Emotions.groups.forEach { (group, label) ->
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Emotions.all.filter { it.group == group }.forEach { e ->
                        SoftChip(selected = e.id in selectedIds, label = e.name, onClick = { onToggle(e.id) })
                    }
                }
            }

            if (marks.isNotEmpty()) {
                Text("Насколько сильно?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                marks.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(Emotions.name(m.emotionId), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        (1..3).forEach { level ->
                            SoftChip(
                                selected = m.intensity == level,
                                label = intensityLabel(level),
                                onClick = { onIntensity(m.emotionId, level) },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = onNote,
                label = { Text("Своими словами") },
                placeholder = { Text("Что происходит? Что вы замечаете в себе?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            val suggestions = remember(note, selectedIds, aiIds) {
                (aiIds ?: EmotionGuess.guess(note)).filter { it !in selectedIds }
            }
            if (aiAvailable && note.trim().length >= 30) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onAiClassify, enabled = !aiLoading) {
                        Text(if (aiIds == null) "✦ Распознать точнее с ИИ" else "✦ Распознано ИИ — обновить")
                    }
                    if (aiLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            if (suggestions.isNotEmpty()) {
                Text(
                    if (marks.isEmpty()) "Похоже на это — нажмите, чтобы отметить:" else "Возможно, ещё:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { id ->
                        SoftChip(selected = false, label = "+ " + Emotions.name(id), onClick = { onToggle(id) })
                    }
                }
            }

            val crisis = remember(note) { Safety.isCrisis(note) }
            val canSubmit = marks.isNotEmpty() || suggestions.isNotEmpty() || crisis
            Spacer(Modifier.height(16.dp))
            val (submitInteraction, submitPress) = rememberSoftPress(0.96f)
            Button(
                onClick = { onSubmit(suggestions) },
                enabled = canSubmit,
                interactionSource = submitInteraction,
                modifier = Modifier.fillMaxWidth().then(submitPress),
            ) {
                Text("Осмыслить")
            }
            if (!canSubmit) {
                Text(
                    if (note.isBlank()) "Отметьте чувство или опишите, что происходит."
                    else "Не получилось узнать чувство по тексту — отметьте его выше, и я подберу взгляды философов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else if (marks.isEmpty() && suggestions.isNotEmpty()) {
                Text(
                    "Возьму чувства из подсказки: " + suggestions.joinToString(", ") { Emotions.name(it).lowercase() } + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Мне очень плохо — нужна помощь", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "Это пространство для размышлений, а не замена психологу. Всё, что вы пишете, хранится только на этом телефоне.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
