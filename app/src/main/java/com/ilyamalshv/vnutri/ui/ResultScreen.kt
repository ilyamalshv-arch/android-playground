package com.ilyamalshv.vnutri.ui

import com.ilyamalshv.vnutri.data.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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

@OptIn(ExperimentalLayoutApi::class)
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

    Scaffold(topBar = { BackTopBar(tr("Взгляды на чувства", "Views on feelings"), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
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
                item { FilterChip(selected = family == null, onClick = { family = null }, label = { Text(tr("Все", "All")) }) }
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
                                    tr("Похоже, сейчас вам по-настоящему тяжело. Философия может помочь осмыслить, но не обязана справляться с этим одна — живой человек рядом или на линии поддержки тоже может помочь.", "It seems things are really hard right now. Philosophy can help you make sense of it, but it doesn't have to carry this alone — a person nearby or on a support line can help too."),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                TextButton(onClick = onHelp) { Text(tr("Куда обратиться", "Where to turn")) }
                            }
                        }
                    }
                }
                item {
                    Text(
                        tr("Здесь нет правильного ответа. Прочитайте несколько взглядов и заметьте, какой из них отзывается.", "There is no right answer here. Read a few views and notice which one resonates."),
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
                if (emotionId in setOf("craving", "addiction", "hangover")) {
                    item(key = "addiction-note") {
                        Text(
                            tr("Зависимость — не слабость характера. Если захочется поддержки, помогают люди, которые прошли через то же: группы взаимопомощи (например, АА или АН, есть и онлайн-встречи) и врач-нарколог — в том числе анонимно.", "Addiction is not a weakness of character. If you want support, people who have been through the same can help: mutual-help groups (such as AA or NA, including online meetings) and addiction doctors — anonymously too."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (lenses.isEmpty()) {
                    item { Text(tr("Для этого чувства в выбранной группе пока нет текстов.", "No texts for this feeling in the selected group yet.")) }
                }
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(tr("Что откликнулось?", "What resonated?"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                tr("Одна-две фразы для себя: какая мысль задела, что стало чуть понятнее.", "A line or two for yourself: which thought touched you, what became a little clearer."),
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
                            }) { Text(if (reflectionSaved) tr("Сохранено ✓", "Saved ✓") else tr("Сохранить в дневник", "Save to journal")) }
                        }
                    }
                }
            }
        }
    }
}
