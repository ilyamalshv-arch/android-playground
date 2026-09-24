package com.ilyamalshv.vnutri.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.AiClient
import com.ilyamalshv.vnutri.data.AiReply
import com.ilyamalshv.vnutri.data.Settings
import kotlinx.coroutines.launch
import java.security.SecureRandom

private val AI_MODELS = listOf(
    Triple("llama33", "Llama 3.3 70B", "По умолчанию. Крупная и надёжная, ~50 разборов в день"),
    Triple("llama4", "Llama 4 Scout", "Новее, быстрее, больше разборов в день"),
    Triple("mistral", "Mistral Small 3.1", "Европейская модель, хорошо держит стиль"),
    Triple("gemma", "Gemma 3 12B", "Модель Google, небольшая и быстрая"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings, ai: AiClient, onMusic: (Boolean) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var check by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackTopBar("Настройки", onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Toggle("Фоновая музыка", "Тихий эмбиент, пока открыто приложение", settings.music, onMusic)
            Toggle("Звуки касаний", "Мягкие звуки при выборе и нажатиях", settings.sounds) { settings.sounds = it }
            Toggle("Вибрация", "Лёгкий тактильный отклик", settings.haptics) { settings.haptics = it }
            Toggle("Интро при запуске", "Дыхательная пауза перед входом", settings.intro) { settings.intro = it }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("ИИ-разбор", style = MaterialTheme.typography.titleMedium)
            Text(
                "Работает через ваш собственный бесплатный сервер Cloudflare. Как его создать — в файле worker/README.md в репозитории. Без этих настроек приложение не выходит в интернет.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = settings.aiUrl,
                onValueChange = { settings.aiUrl = it.trim(); check = null },
                label = { Text("Адрес сервера") },
                placeholder = { Text("https://vnutri.имя.workers.dev") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = settings.aiToken,
                onValueChange = { settings.aiToken = it.trim(); check = null },
                label = { Text("Ключ доступа (APP_TOKEN)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                OutlinedButton(onClick = {
                    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
                    val random = SecureRandom()
                    val token = (1..40).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
                    settings.aiToken = token
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("APP_TOKEN", token))
                    check = "Новый ключ скопирован. Вставьте его в Cloudflare как секрет APP_TOKEN."
                }) { Text("Создать ключ") }
                OutlinedButton(
                    enabled = settings.aiConfigured,
                    onClick = {
                        check = "Проверяю…"
                        scope.launch {
                            check = when (val r = ai.health()) {
                                is AiReply.Failure -> r.message
                                else -> "Соединение работает ✓"
                            }
                        }
                    },
                ) { Text("Проверить соединение") }
            }
            check?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            Text("Модель", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Все бесплатные. Если ответы кажутся слабыми или модель не отвечает — попробуйте другую.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AI_MODELS.forEach { (key, title, hint) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { settings.aiModel = key }.padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = settings.aiModel == key, onClick = { settings.aiModel = key })
                    Column {
                        Text(title, style = MaterialTheme.typography.bodyLarge)
                        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (settings.aiUrl.isNotBlank() && !settings.aiUrl.startsWith("https://")) {
                Text("Адрес должен начинаться с https://", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Text(" ", Modifier.padding(bottom = 24.dp))
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
