package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.AiClient
import com.ilyamalshv.vnutri.data.AiReply
import com.ilyamalshv.vnutri.data.JournalEntry
import com.ilyamalshv.vnutri.data.Lens
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.Safety
import com.ilyamalshv.vnutri.data.School
import com.ilyamalshv.vnutri.data.Settings
import com.ilyamalshv.vnutri.sound.LocalFeedback
import kotlinx.coroutines.launch

private val DEFAULT_SCHOOLS = listOf("byung_chul_han", "existentialism", "deleuze_guattari", "stoicism", "spinoza", "butler")

/** Schools the person opened or saved come first; a varied default set fills the rest. */
fun pickLenses(library: Library, entry: JournalEntry, opened: Collection<String>): List<Triple<School, String, Lens>> {
    val keys = entry.saved.map { it.schoolId to it.emotionId } +
        opened.map { it.substringBefore(':') to it.substringAfter(':') }
    val picked = keys.mapNotNull { (sid, eid) ->
        library.school(sid)?.let { s -> s.lenses[eid]?.let { Triple(s, eid, it) } }
    }.toMutableList()
    for (mark in entry.emotions) {
        for (sid in DEFAULT_SCHOOLS) {
            if (picked.size >= 4) break
            if (picked.any { it.first.id == sid }) continue
            library.school(sid)?.let { s -> s.lenses[mark.emotionId]?.let { picked += Triple(s, mark.emotionId, it) } }
        }
    }
    return picked.distinctBy { it.first.id }.take(4)
}

@Composable
fun AiCard(
    library: Library,
    entry: JournalEntry,
    opened: Collection<String>,
    settings: Settings,
    ai: AiClient,
    onUpdate: (JournalEntry) -> Unit,
    onCrisis: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val feedback = LocalFeedback.current
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(entry.ai.isNotBlank()) }
    var note by rememberSaveable { mutableStateOf(entry.note) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var askConsent by remember { mutableStateOf(false) }

    fun send() {
        if (Safety.isCrisis(note)) {
            onCrisis()
            return
        }
        loading = true
        error = null
        scope.launch {
            val reply = ai.reflect(entry.emotions, note.trim(), pickLenses(library, entry, opened))
            loading = false
            when (reply) {
                is AiReply.Text -> {
                    feedback?.confirm()
                    onUpdate(entry.copy(ai = reply.text, note = note.trim()))
                }
                AiReply.Crisis -> onCrisis()
                is AiReply.Failure -> error = reply.message
            }
        }
    }

    Card(
        onClick = { if (!open) { feedback?.tap(); open = true } },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✦ Разобрать мою ситуацию", style = MaterialTheme.typography.titleMedium)
            if (!open) {
                Text(
                    "ИИ свяжет взгляды школ с тем, что происходит именно у вас.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            if (!settings.aiConfigured) {
                Text(
                    "ИИ-режим ещё не настроен: нужен ваш бесплатный сервер Cloudflare. Инструкция — в репозитории, в папке worker.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onOpenSettings) { Text("Открыть настройки") }
                return@Column
            }
            if (entry.ai.isNotBlank()) {
                Text(entry.ai, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Ответ написан ИИ и может ошибаться. Опирайтесь на то, что откликается.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Что происходит?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "ИИ опирается на школы, которые вы раскрыли или сохранили на этом экране, а если таких нет — на подборку по умолчанию.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { if (settings.aiConsent) send() else askConsent = true },
                    enabled = !loading,
                ) { Text(if (entry.ai.isBlank()) "Разобрать" else "Разобрать заново") }
                if (loading) {
                    CircularProgressIndicator(Modifier.padding(start = 16.dp).size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }

    if (askConsent) {
        AlertDialog(
            onDismissRequest = { askConsent = false },
            title = { Text("Перед первым разбором") },
            text = {
                Text(
                    "Выбранные чувства, ваш текст и несколько текстов школ будут отправлены на ваш сервер Cloudflare и обработаны открытой моделью ИИ. " +
                        "Дневник, имя и другие записи не отправляются, сервер ничего не сохраняет.\n\n" +
                        "Лучше не писать имён, адресов и других личных данных.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.aiConsent = true
                    askConsent = false
                    send()
                }) { Text("Согласен(на)") }
            },
            dismissButton = { TextButton(onClick = { askConsent = false }) { Text("Отмена") } },
        )
    }
}
