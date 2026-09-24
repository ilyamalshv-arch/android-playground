package com.ilyamalshv.vnutri.ui

import com.ilyamalshv.vnutri.data.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.Emotions
import com.ilyamalshv.vnutri.data.JournalEntry
import com.ilyamalshv.vnutri.data.Lang
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.intensityLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag(Lang.current)).format(Date(millis))

@Composable
fun JournalScreen(entries: List<JournalEntry>, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    Scaffold(containerColor = Color.Transparent, topBar = { BackTopBar(tr("Дневник", "Journal"), onBack) }) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    tr("Здесь будут ваши записи: что вы чувствовали, какие мысли откликнулись.", "Your entries will live here: what you felt and which thoughts resonated."),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries.sortedByDescending { it.id }, key = { it.id }) { e ->
                Card(
                    onClick = { onOpen(e.id) },
                    modifier = Modifier.fillMaxWidth().glowTouch(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(formatDate(e.id), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(e.emotions.joinToString(" · ") { Emotions.name(it.emotionId) }, style = MaterialTheme.typography.titleMedium)
                        val preview = e.reflection.ifBlank { e.note }
                        if (preview.isNotBlank()) {
                            Text(preview, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalDetailScreen(
    library: Library,
    entry: JournalEntry,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(containerColor = Color.Transparent, topBar = { BackTopBar(formatDate(entry.id), onBack) }) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(tr("Чувства", "Feelings"), style = MaterialTheme.typography.titleSmall)
                Text(
                    entry.emotions.joinToString(", ") { "${Emotions.name(it.emotionId).lowercase()} (${intensityLabel(it.intensity)})" },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (entry.note.isNotBlank()) {
                item {
                    Text(tr("Своими словами", "In your own words"), style = MaterialTheme.typography.titleSmall)
                    Text(entry.note, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (entry.conversation.isNotEmpty()) {
                item {
                    Text(tr("Разбор ИИ", "AI reflection"), style = MaterialTheme.typography.titleSmall)
                    entry.conversation.forEach { turn ->
                        Text(
                            if (turn.role == "user") tr("Вы: ${turn.text}", "You: ${turn.text}") else turn.text,
                            style = if (turn.role == "user") MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            color = if (turn.role == "user") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            if (entry.reflection.isNotBlank()) {
                item {
                    Text(tr("Что откликнулось", "What resonated"), style = MaterialTheme.typography.titleSmall)
                    Text(entry.reflection, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (entry.saved.isNotEmpty()) {
                item { Text(tr("Сохранённые мысли", "Saved thoughts"), style = MaterialTheme.typography.titleSmall) }
                items(entry.saved, key = { it.schoolId + ":" + it.emotionId }) { s ->
                    val school = library.school(s.schoolId)
                    val lens = school?.lenses?.get(s.emotionId)
                    if (school != null && lens != null) {
                        val key = s.schoolId + ":" + s.emotionId
                        LensCard(
                            heading = school.title,
                            subheading = "${Emotions.name(s.emotionId)} · ${school.tradition}",
                            lens = lens,
                            expanded = expanded[key] == true,
                            onToggle = { expanded[key] = expanded[key] != true },
                        )
                    }
                }
            }
            item {
                Button(onClick = onReopen, modifier = Modifier.fillMaxWidth()) { Text(tr("Открыть взгляды снова", "Open the views again")) }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Удалить запись", "Delete entry"), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Удалить запись?", "Delete this entry?")) },
            text = { Text(tr("Её нельзя будет восстановить.", "It can't be restored.")) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(tr("Удалить", "Delete")) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}
