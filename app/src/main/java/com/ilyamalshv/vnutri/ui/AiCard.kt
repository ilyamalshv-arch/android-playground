package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.ilyamalshv.vnutri.data.AiTurn
import com.ilyamalshv.vnutri.data.Families
import com.ilyamalshv.vnutri.data.JournalEntry
import com.ilyamalshv.vnutri.data.Lens
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.Safety
import com.ilyamalshv.vnutri.data.School
import com.ilyamalshv.vnutri.data.Settings
import com.ilyamalshv.vnutri.sound.LocalFeedback
import kotlinx.coroutines.launch

private val DEFAULT_SCHOOLS = listOf("byung_chul_han", "existentialism", "deleuze_guattari", "stoicism", "spinoza", "butler")
private const val MAX_SCHOOLS = 4

val AI_STYLES = listOf("gentle" to "Мягче", "deep" to "Глубже", "practical" to "Практичнее")

/** Schools the person opened or saved come first; a varied default set fills the rest. */
fun defaultSchoolIds(library: Library, entry: JournalEntry, opened: Collection<String>): List<String> {
    val ids = entry.saved.map { it.schoolId } + opened.map { it.substringBefore(':') } + DEFAULT_SCHOOLS
    return ids.distinct().filter { library.school(it) != null }.take(MAX_SCHOOLS)
}

/** For each chosen school, its text on the strongest feeling of the entry. */
private fun lensesFor(library: Library, entry: JournalEntry, schoolIds: List<String>): List<Triple<School, String, Lens>> {
    val order = entry.emotions.sortedByDescending { it.intensity }.map { it.emotionId }
    return schoolIds.mapNotNull { sid ->
        val school = library.school(sid) ?: return@mapNotNull null
        val emotionId = order.firstOrNull { school.lenses.containsKey(it) } ?: return@mapNotNull null
        Triple(school, emotionId, school.lenses.getValue(emotionId))
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val conversation = entry.conversation
    var open by rememberSaveable { mutableStateOf(conversation.isNotEmpty()) }
    var note by rememberSaveable { mutableStateOf(entry.note) }
    var reply by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var askConsent by remember { mutableStateOf(false) }
    var pickSchool by remember { mutableStateOf(false) }
    val schools = remember { mutableStateListOf<String>() }
    LaunchedEffect(open) {
        if (open && schools.isEmpty()) schools.addAll(defaultSchoolIds(library, entry, opened))
    }

    fun run(history: List<AiTurn>, onDone: (String) -> Unit) {
        val latest = history.lastOrNull { it.role == "user" }?.text ?: note
        if (Safety.isCrisis(latest)) {
            onCrisis()
            return
        }
        loading = true
        error = null
        scope.launch {
            val result = ai.reflect(entry.emotions, note.trim(), lensesFor(library, entry, schools), history)
            loading = false
            when (result) {
                is AiReply.Text -> {
                    feedback?.confirm()
                    onDone(result.text)
                }
                AiReply.Crisis -> onCrisis()
                is AiReply.Failure -> error = result.message
            }
        }
    }

    fun start() = run(emptyList()) { text ->
        onUpdate(entry.copy(note = note.trim(), ai = text, aiTurns = listOf(AiTurn("assistant", text))))
    }

    fun continueWith(message: String) {
        val history = conversation + AiTurn("user", message)
        run(history) { text ->
            reply = ""
            onUpdate(entry.copy(aiTurns = history + AiTurn("assistant", text)))
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
                Text("ИИ свяжет взгляды школ с тем, что происходит именно у вас.", style = MaterialTheme.typography.bodyMedium)
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

            if (conversation.isEmpty()) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Что происходит?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text("Школы для разбора", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                schools.forEach { sid ->
                    val title = library.school(sid)?.title ?: sid
                    InputChip(
                        selected = true,
                        onClick = { if (schools.size > 1) schools.remove(sid) },
                        label = { Text("$title  ✕") },
                    )
                }
                if (schools.size < MAX_SCHOOLS) {
                    AssistChip(onClick = { pickSchool = true }, label = { Text("+ школа") })
                }
            }

            Text("Тон", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AI_STYLES.forEach { (key, label) ->
                    FilterChip(selected = settings.aiStyle == key, onClick = { settings.aiStyle = key }, label = { Text(label) })
                }
            }

            conversation.forEach { turn ->
                if (turn.role == "assistant") {
                    Text(turn.text, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(turn.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (conversation.isNotEmpty()) {
                Text(
                    "Ответы пишет ИИ, он может ошибаться. Опирайтесь на то, что откликается.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            if (conversation.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { if (settings.aiConsent) start() else askConsent = true }, enabled = !loading) {
                        Text("Разобрать")
                    }
                    if (loading) CircularProgressIndicator(Modifier.padding(start = 16.dp).size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Ответить или спросить ещё") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { continueWith(reply.trim()) }, enabled = !loading && reply.isNotBlank()) {
                        Text("Отправить")
                    }
                    TextButton(onClick = { start() }, enabled = !loading) { Text("Разобрать заново") }
                    if (loading) CircularProgressIndicator(Modifier.padding(start = 8.dp).size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }

    if (pickSchool) {
        AlertDialog(
            onDismissRequest = { pickSchool = false },
            title = { Text("Добавить школу") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(library.schools.filter { it.id !in schools }, key = { it.id }) { s ->
                        TextButton(
                            onClick = { schools.add(s.id); pickSchool = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(s.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    Families.name(s.family),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickSchool = false }) { Text("Закрыть") } },
        )
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
                    start()
                }) { Text("Согласен(на)") }
            },
            dismissButton = { TextButton(onClick = { askConsent = false }) { Text("Отмена") } },
        )
    }
}
