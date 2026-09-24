package com.ilyamalshv.vnutri.ui

import com.ilyamalshv.vnutri.data.tr
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ilyamalshv.vnutri.data.Safety

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrisisScreen(fromText: Boolean, onContinue: (() -> Unit)?, onBack: () -> Unit) {
    val context = LocalContext.current
    fun open(uri: String, action: String) {
        runCatching { context.startActivity(Intent(action, Uri.parse(uri))) }
    }

    Scaffold(containerColor = Color.Transparent, topBar = { BackTopBar(tr("Поддержка", "Support"), onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (fromText) tr("Похоже, сейчас очень тяжело. Спасибо, что написали об этом.", "It sounds very hard right now. Thank you for writing about it.") else tr("Вы не обязаны справляться с этим в одиночку.", "You don't have to face this alone."),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                tr("Если есть мысли причинить себе вред или кажется, что не выдержать, — пожалуйста, поговорите с живым человеком прямо сейчас. Звонок анонимный, и вам не нужно заранее знать, что сказать.", "If you have thoughts of hurting yourself or feel you can't hold on, please talk to a real person right now. Calls are anonymous, and you don't need to know in advance what to say."),
                style = MaterialTheme.typography.bodyLarge,
            )
            Safety.resources.forEach { r ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium)
                        Text(r.details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        r.phone?.let { phone ->
                            Button(onClick = { open("tel:$phone", Intent.ACTION_DIAL) }, modifier = Modifier.padding(top = 8.dp)) {
                                Text(tr("Позвонить", "Call"))
                            }
                        }
                        r.url?.let { url ->
                            OutlinedButton(onClick = { open(url, Intent.ACTION_VIEW) }, modifier = Modifier.padding(top = 8.dp)) {
                                Text(tr("Открыть сайт", "Open website"))
                            }
                        }
                    }
                }
            }
            Text(
                tr("Можно также написать или позвонить тому, кому вы доверяете: другу, родственнику, врачу. Просто «мне сейчас плохо, побудь со мной» — уже достаточно.", "You can also text or call someone you trust: a friend, a relative, a doctor. Just “I'm not OK right now, stay with me” is enough."),
                style = MaterialTheme.typography.bodyLarge,
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Прямо сейчас, на минуту", "Right now, for a minute"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        tr("Поставьте ноги на пол и почувствуйте опору. Сделайте медленный вдох на 4 счёта и ещё более медленный выдох на 6. Повторите пять раз. Назовите про себя пять вещей, которые вы видите вокруг.", "Put your feet on the floor and feel the support. Breathe in slowly for 4 counts and out even more slowly for 6. Repeat five times. Name to yourself five things you can see around you."),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (onContinue != null) {
                OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Продолжить к размышлениям", "Continue to reflection"))
                }
            }
        }
    }
}
