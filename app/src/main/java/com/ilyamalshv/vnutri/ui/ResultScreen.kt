package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.AiClient
import com.ilyamalshv.vnutri.data.Emotions
import com.ilyamalshv.vnutri.data.Families
import com.ilyamalshv.vnutri.data.JournalEntry
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.Safety
import com.ilyamalshv.vnutri.data.SavedLens
import com.ilyamalshv.vnutri.data.Settings

@Composable
fun ResultScreen(
    library: Library,
    entry: JournalEntry,
    onUpdate: (JournalEntry) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    settings: Settings,
    ai: AiClient,
    onOpenSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var family by rememberSaveable { mutableStateOf<String?>(null) }
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var reflection by rememberSaveable { mutableStateOf(entry.reflection) }
    var reflectionSaved by remember { mutableStateOf(false) }

    val emotionId = entry.emotions.getOrNull(tab)?.emotionId ?: entry.emotions.first().emotionId
    val lenses = library.lensesFor(emotionId).filter { family == null || it.first.family == family }
    val presentFamilies = Families.all.filter { f -> library.schools.any { it.family == f.id } }

    Scaffold(topBar = { BackTopBar("Взгляды на чувства", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (entry.emotions.size > 1) {
                ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                    entry.emotions.forEachIndexed { i, m ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(Emotions.name(m.emotionId)) })
                    }
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterChip(selected = family == null, onClick = { family = null }, label = { Text("Все") }) }
                items(presentFamilies, key = { it.id }) { f ->
                    FilterChip(selected = family == f.id, onClick = { family = f.id }, label = { Text(f.name) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (Safety.isHeavy(entry.emotions)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Похоже, сейчас вам по-настоящему тяжело. Философия может помочь осмыслить, но не обязана справляться с этим одна — живой человек рядом или на линии поддержки тоже может помочь.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                TextButton(onClick = onHelp) { Text("Куда обратиться") }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Здесь нет правильного ответа. Прочитайте несколько взглядов и заметьте, какой из них отзывается.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "ai") {
                    AiCard(
                        library = library,
                        entry = entry,
                        opened = expanded.filterValues { it }.keys,
                        settings = settings,
                        ai = ai,
                        onUpdate = onUpdate,
                        onCrisis = onHelp,
                        onOpenSettings = onOpenSettings,
                    )
                }
                items(lenses, key = { it.first.id + ":" + emotionId }) { (school, lens) ->
                    val key = school.id + ":" + emotionId
                    val isSaved = entry.saved.any { it.schoolId == school.id && it.emotionId == emotionId }
                    LensCard(
                        heading = school.title,
                        subheading = "${school.period} · ${school.tradition}",
                        lens = lens,
                        expanded = expanded[key] == true,
                        onToggle = { expanded[key] = expanded[key] != true },
                        saved = isSaved,
                        onToggleSave = {
                            val s = SavedLens(school.id, emotionId)
                            onUpdate(entry.copy(saved = if (isSaved) entry.saved - s else entry.saved + s))
                        },
                    )
                }
                if (lenses.isEmpty()) {
                    item { Text("Для этого чувства в выбранной группе пока нет текстов.") }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Что откликнулось?", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Одна-две фразы для себя: какая мысль задела, что стало чуть понятнее.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = reflection,
                                onValueChange = { reflection = it; reflectionSaved = false },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            )
                            Button(onClick = {
                                onUpdate(entry.copy(reflection = reflection.trim()))
                                reflectionSaved = true
                            }) { Text(if (reflectionSaved) "Сохранено ✓" else "Сохранить в дневник") }
                        }
                    }
                }
            }
        }
    }
}
