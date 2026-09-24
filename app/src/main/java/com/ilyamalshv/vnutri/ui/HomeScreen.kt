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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.EmotionMark
import com.ilyamalshv.vnutri.data.Emotions
import com.ilyamalshv.vnutri.data.intensityLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    marks: List<EmotionMark>,
    onToggle: (String) -> Unit,
    onIntensity: (String, Int) -> Unit,
    note: String,
    onNote: (String) -> Unit,
    onSubmit: () -> Unit,
    onJournal: () -> Unit,
    onLibrary: () -> Unit,
    onHelp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Внутри") },
                actions = {
                    TextButton(onClick = onJournal) { Text("Дневник") }
                    TextButton(onClick = onLibrary) { Text("Школы") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
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
            listOf(false to "Тяжёлое", true to "Светлое и смешанное").forEach { (light, label) ->
                Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Emotions.all.filter { it.light == light }.forEach { e ->
                        FilterChip(
                            selected = e.id in selectedIds,
                            onClick = { onToggle(e.id) },
                            label = { Text(e.name) },
                        )
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
                            FilterChip(
                                selected = m.intensity == level,
                                onClick = { onIntensity(m.emotionId, level) },
                                label = { Text(intensityLabel(level)) },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = onNote,
                label = { Text("Своими словами (необязательно)") },
                placeholder = { Text("Что происходит? Что вы замечаете в себе?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = onSubmit, enabled = marks.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Осмыслить")
            }
            if (marks.isEmpty() && note.isNotBlank()) {
                Text(
                    "Отметьте хотя бы одно чувство, чтобы подобрать взгляды философов.",
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
