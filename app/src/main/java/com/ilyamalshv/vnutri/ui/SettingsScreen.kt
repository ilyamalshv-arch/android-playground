package com.ilyamalshv.vnutri.ui

import com.ilyamalshv.vnutri.data.tr
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

private fun aiModels() = listOf(
    Triple("llama33", "Llama 3.3 70B", tr("По умолчанию. Крупная и надёжная, ~50 разборов в день", "Default. Large and reliable, ~50 reflections a day")),
    Triple("llama4", "Llama 4 Scout", tr("Новее, быстрее, больше разборов в день", "Newer, faster, more reflections per day")),
    Triple("mistral", "Mistral Small 3.1", tr("Европейская модель, хорошо держит стиль", "European model, keeps the style well")),
    Triple("gemma", "Gemma 3 12B", tr("Модель Google, небольшая и быстрая", "Google's model, small and fast")),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings, ai: AiClient, onLang: (String) -> Unit, onMusic: (Boolean) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var check by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackTopBar(tr("Настройки", "Settings"), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                Text(tr("Язык", "Language"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                LangSwitch(settings.lang, onLang)
            }
            Toggle(tr("Фоновая музыка", "Background music"), tr("Тихий эмбиент, пока открыто приложение", "Quiet ambient while the app is open"), settings.music, onMusic)
            Toggle(tr("Звуки касаний", "Touch sounds"), tr("Мягкие звуки при выборе и нажатиях", "Soft sounds on selection and taps"), settings.sounds) { settings.sounds = it }
            Toggle(tr("Вибрация", "Vibration"), tr("Лёгкий тактильный отклик", "Light haptic feedback"), settings.haptics) { settings.haptics = it }
            Toggle(tr("Заставка при запуске", "Splash on launch"), tr("Живая масса, которую можно стереть пальцем", "A living mass you can wipe away with your finger"), settings.intro) { settings.intro = it }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(tr("ИИ-разбор", "AI reflection"), style = MaterialTheme.typography.titleMedium)
            Text(
                tr("Работает через ваш собственный бесплатный сервер Cloudflare. Как его создать — в файле worker/README.md в репозитории. Без этих настроек приложение не выходит в интернет.", "Works through your own free Cloudflare server. How to create it is in worker/README.md in the repository. Without these settings the app never goes online."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = settings.aiUrl,
                onValueChange = { settings.aiUrl = it.trim(); check = null },
                label = { Text(tr("Адрес сервера", "Server address")) },
                placeholder = { Text("https://….workers.dev") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = settings.aiToken,
                onValueChange = { settings.aiToken = it.trim(); check = null },
                label = { Text(tr("Ключ доступа (APP_TOKEN)", "Access key (APP_TOKEN)")) },
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
                    check = tr("Новый ключ скопирован. Вставьте его в Cloudflare как секрет APP_TOKEN.", "New key copied. Paste it into Cloudflare as the APP_TOKEN secret.")
                }) { Text(tr("Создать ключ", "Create key")) }
                OutlinedButton(
                    enabled = settings.aiConfigured,
                    onClick = {
                        check = tr("Проверяю…", "Checking…")
                        scope.launch {
                            check = when (val r = ai.health()) {
                                is AiReply.Failure -> r.message
                                else -> tr("Соединение работает ✓", "Connection works ✓")
                            }
                        }
                    },
                ) { Text(tr("Проверить соединение", "Check connection")) }
            }
            check?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            Text(tr("Модель", "Model"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            Text(
                tr("Все бесплатные. Если ответы кажутся слабыми или модель не отвечает — попробуйте другую.", "All free. If answers feel weak or a model doesn't respond, try another one."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            aiModels().forEach { (key, title, hint) ->
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
                Text(tr("Адрес должен начинаться с https://", "The address must start with https://"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
