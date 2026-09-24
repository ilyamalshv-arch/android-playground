package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.Emotions
import com.ilyamalshv.vnutri.data.Families
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.School

@Composable
fun LibraryScreen(library: Library, onOpen: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar("Школы и мыслители", onBack) }) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Families.all.forEach { family ->
                val schools = library.schools.filter { it.family == family.id }
                if (schools.isNotEmpty()) {
                    item(key = "h:" + family.id) {
                        Text(family.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                    items(schools, key = { it.id }) { s ->
                        Card(
                            onClick = { onOpen(s.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium)
                                Text("${s.period} · ${s.tradition}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(s.summary, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchoolScreen(school: School, onBack: () -> Unit) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    Scaffold(topBar = { BackTopBar(school.title, onBack) }) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("${school.period} · ${school.tradition}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (school.thinkers.isNotEmpty()) {
                    Text(school.thinkers.joinToString(", "), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                }
                if (school.keyWorks.isNotEmpty()) {
                    Text(school.keyWorks.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Text(school.summary, style = MaterialTheme.typography.bodyLarge) }
            item {
                Text("Как эта школа видит чувства", style = MaterialTheme.typography.titleSmall)
                Text(school.onEmotions, style = MaterialTheme.typography.bodyLarge)
            }
            item { Text("Взгляд на каждое чувство", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
            items(Emotions.all.filter { school.lenses.containsKey(it.id) }, key = { it.id }) { e ->
                LensCard(
                    heading = e.name,
                    subheading = "",
                    lens = school.lenses.getValue(e.id),
                    expanded = expanded[e.id] == true,
                    onToggle = { expanded[e.id] = expanded[e.id] != true },
                )
            }
        }
    }
}
