package com.ilyamalshv.vnutri.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.Settings

@Composable
fun SettingsScreen(settings: Settings, onMusic: (Boolean) -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar("Настройки", onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Toggle("Фоновая музыка", "Тихий эмбиент, пока открыто приложение", settings.music, onMusic)
            Toggle("Звуки касаний", "Мягкие звуки при выборе и нажатиях", settings.sounds) { settings.sounds = it }
            Toggle("Вибрация", "Лёгкий тактильный отклик", settings.haptics) { settings.haptics = it }
            Toggle("Интро при запуске", "Дыхательная пауза перед входом", settings.intro) { settings.intro = it }
        }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
