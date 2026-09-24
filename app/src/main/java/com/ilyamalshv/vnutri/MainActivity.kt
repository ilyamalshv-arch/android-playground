package com.ilyamalshv.vnutri

import com.ilyamalshv.vnutri.data.tr
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.ilyamalshv.vnutri.data.AiClient
import com.ilyamalshv.vnutri.data.AiReply
import com.ilyamalshv.vnutri.data.EmotionGuess
import com.ilyamalshv.vnutri.data.EmotionMark
import com.ilyamalshv.vnutri.data.JournalEntry
import com.ilyamalshv.vnutri.data.Lang
import com.ilyamalshv.vnutri.data.JournalStore
import com.ilyamalshv.vnutri.data.Library
import com.ilyamalshv.vnutri.data.Safety
import com.ilyamalshv.vnutri.data.Settings
import com.ilyamalshv.vnutri.sound.Ambient
import com.ilyamalshv.vnutri.sound.Feedback
import com.ilyamalshv.vnutri.sound.LocalFeedback
import com.ilyamalshv.vnutri.ui.SplashScreen
import com.ilyamalshv.vnutri.ui.pickSplashQuote
import com.ilyamalshv.vnutri.ui.SettingsScreen
import com.ilyamalshv.vnutri.ui.CrisisScreen
import com.ilyamalshv.vnutri.ui.HomeScreen
import com.ilyamalshv.vnutri.ui.JournalDetailScreen
import com.ilyamalshv.vnutri.ui.JournalScreen
import com.ilyamalshv.vnutri.ui.LibraryScreen
import com.ilyamalshv.vnutri.ui.ResultScreen
import com.ilyamalshv.vnutri.ui.SchoolScreen
import com.ilyamalshv.vnutri.ui.VnutriTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var settings: Settings
    private lateinit var ambient: Ambient
    private lateinit var feedback: Feedback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = Settings(this)
        ambient = Ambient(this)
        feedback = Feedback(this, settings)
        ambient.setEnabled(settings.music)
        Lang.current = settings.lang
        setContent {
            feedback.view = LocalView.current
            VnutriTheme {
                CompositionLocalProvider(LocalFeedback provides feedback) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        App(settings, ambient, feedback)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ambient.onForeground(true)
    }

    override fun onStop() {
        ambient.onForeground(false)
        super.onStop()
    }

    override fun onDestroy() {
        ambient.release()
        feedback.release()
        super.onDestroy()
    }
}

private sealed interface Screen {
    data object Intro : Screen
    data object Settings : Screen
    data object Home : Screen
    data class Result(val entryId: Long) : Screen
    data class Crisis(val fromText: Boolean, val continueTo: Long?) : Screen
    data object Journal : Screen
    data class JournalDetail(val entryId: Long) : Screen
    data object Library : Screen
    data class School(val schoolId: String) : Screen
}

@Composable
private fun App(settings: Settings, ambient: Ambient, feedback: Feedback) {
    val context = LocalContext.current
    val lang = Lang.current
    val library by produceState<Library?>(null, lang) {
        value = withContext(Dispatchers.IO) { Library.load(context, lang) }
    }
    val store = remember { JournalStore(context) }
    val ai = remember { AiClient(settings) }
    val journal = remember { mutableStateListOf<JournalEntry>().apply { addAll(store.load()) } }
    val stack = remember { mutableStateListOf<Screen>(if (settings.intro) Screen.Intro else Screen.Home) }
    val marks = remember { mutableStateListOf<EmotionMark>() }
    var note by remember { mutableStateOf("") }
    var aiIds by remember { mutableStateOf<List<String>?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var askConsent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun classify() {
        aiLoading = true
        scope.launch {
            when (val r = ai.classify(note)) {
                is AiReply.States -> aiIds = r.ids.ifEmpty { null }
                AiReply.Crisis -> stack.add(Screen.Crisis(fromText = true, continueTo = null))
                else -> Unit
            }
            aiLoading = false
        }
    }

    fun push(s: Screen) = stack.add(s)
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replaceTop(s: Screen) { stack[stack.lastIndex] = s }
    fun upsert(e: JournalEntry) {
        val i = journal.indexOfFirst { it.id == e.id }
        if (i >= 0) journal[i] = e else journal.add(e)
        store.save(journal.toList())
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    val current = stack.last()
    LaunchedEffect(current is Screen.Intro) {
        ambient.setLevel(if (current is Screen.Intro) 0.7f else 0.22f)
    }

    val lib = library
    if (lib == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when (val screen = current) {
        Screen.Intro -> {
            val quote = remember(lib) { pickSplashQuote(lib) }
            SplashScreen(quote = quote, lang = lang, onLang = { settings.lang = it; Lang.current = it }, onEnter = {
                feedback.confirm()
                replaceTop(Screen.Home)
            })
        }

        Screen.Settings -> SettingsScreen(
            settings = settings,
            ai = ai,
            onLang = { settings.lang = it; Lang.current = it },
            onMusic = {
                settings.music = it
                ambient.setEnabled(it)
            },
            onBack = ::pop,
        )

        Screen.Home -> HomeScreen(
            marks = marks,
            onToggle = { id ->
                val i = marks.indexOfFirst { it.emotionId == id }
                if (i >= 0) marks.removeAt(i) else marks.add(EmotionMark(id, 2))
                feedback.select()
            },
            onIntensity = { id, level ->
                val i = marks.indexOfFirst { it.emotionId == id }
                if (i >= 0) marks[i] = EmotionMark(id, level)
                feedback.tap()
            },
            note = note,
            onNote = { note = it; aiIds = null },
            onSubmit = { suggested ->
                // Text alone is enough: fall back to the suggestions shown under the text field.
                val chosen = marks.toList().ifEmpty { suggested.map { EmotionMark(it, 2) } }
                val crisis = Safety.isCrisis(note)
                val entry = JournalEntry(id = System.currentTimeMillis(), emotions = chosen, note = note.trim())
                upsert(entry)
                feedback.confirm()
                marks.clear()
                note = ""
                aiIds = null
                val next = entry.id.takeIf { chosen.isNotEmpty() }
                push(
                    when {
                        crisis -> Screen.Crisis(fromText = true, continueTo = next)
                        next != null -> Screen.Result(next)
                        else -> Screen.Home
                    },
                )
            },
            aiAvailable = settings.aiConfigured,
            aiIds = aiIds,
            aiLoading = aiLoading,
            onAiClassify = {
                if (!settings.aiConsent) {
                    askConsent = true
                } else {
                    classify()
                }
            },
            onJournal = { push(Screen.Journal) },
            onLibrary = { push(Screen.Library) },
            onHelp = { push(Screen.Crisis(fromText = false, continueTo = null)) },
            onSettings = { push(Screen.Settings) },
        )

        is Screen.Result -> {
            val entry = journal.firstOrNull { it.id == screen.entryId }
            if (entry == null || entry.emotions.isEmpty()) LaunchedEffect(screen) { pop() } else ResultScreen(
                library = lib,
                entry = entry,
                onUpdate = ::upsert,
                onBack = ::pop,
                onHelp = { push(Screen.Crisis(fromText = false, continueTo = null)) },
                settings = settings,
                ai = ai,
                onOpenSettings = { push(Screen.Settings) },
            )
        }

        is Screen.Crisis -> CrisisScreen(
            fromText = screen.fromText,
            onContinue = screen.continueTo?.let { id -> { replaceTop(Screen.Result(id)) } },
            onBack = ::pop,
        )

        Screen.Journal -> JournalScreen(
            entries = journal,
            onOpen = { push(Screen.JournalDetail(it)) },
            onBack = ::pop,
        )

        is Screen.JournalDetail -> {
            val entry = journal.firstOrNull { it.id == screen.entryId }
            if (entry == null) LaunchedEffect(screen) { pop() } else JournalDetailScreen(
                library = lib,
                entry = entry,
                onReopen = { push(Screen.Result(entry.id)) },
                onDelete = {
                    journal.removeAll { it.id == entry.id }
                    store.save(journal.toList())
                    pop()
                },
                onBack = ::pop,
            )
        }

        Screen.Library -> LibraryScreen(lib, onOpen = { push(Screen.School(it)) }, onBack = ::pop)

        is Screen.School -> {
            val school = lib.school(screen.schoolId)
            if (school == null) LaunchedEffect(screen) { pop() } else SchoolScreen(school, onBack = ::pop)
        }
    }

    if (current !is Screen.Intro) WelcomeDialog()

    if (askConsent) {
        AlertDialog(
            onDismissRequest = { askConsent = false },
            title = { Text(tr("ИИ прочитает текст", "AI will read the text")) },
            text = {
                Text(
                    tr("Чтобы распознать состояния точнее, ваш текст будет отправлен на ваш сервер Cloudflare и обработан открытой моделью ИИ. ", "To recognise your states more precisely, your text will be sent to your Cloudflare server and processed by an open AI model. ") +
                        tr("Сервер ничего не сохраняет. Лучше не писать имён, адресов и других личных данных.", "The server stores nothing. Better not to include names, addresses or other personal details."),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.aiConsent = true
                    askConsent = false
                    classify()
                }) { Text(tr("Согласен(на)", "I agree")) }
            },
            dismissButton = { TextButton(onClick = { askConsent = false }) { Text(tr("Отмена", "Cancel")) } },
        )
    }
}

@Composable
private fun WelcomeDialog() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vnutri", Context.MODE_PRIVATE) }
    var show by remember { mutableStateOf(!prefs.getBoolean("welcomed", false)) }
    if (!show) return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(tr("Добро пожаловать", "Welcome")) },
        text = {
            Text(
                tr("Sincerer помогает посмотреть на свои чувства глазами философов — от стоиков до мыслителей XXI века — и принять их, а не бороться с ними.\n\n", "Sincerer helps you look at your feelings through the eyes of philosophers — from the Stoics to 21st-century thinkers — and accept them rather than fight them.\n\n") +
                    tr("Это не терапия и не замена психологу. Если вам очень плохо, на главном экране всегда есть кнопка помощи.\n\n", "This is not therapy and not a replacement for a psychologist. If you feel really bad, there is always a help button on the home screen.\n\n") +
                    tr("Всё, что вы пишете, остаётся только на этом телефоне. Исключение — необязательный ИИ-разбор: он включается отдельно и перед первым использованием всё объяснит.", "Everything you write stays on this phone. The only exception is the optional AI reflection: it is switched on separately and explains everything before its first use."),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.edit().putBoolean("welcomed", true).apply()
                show = false
            }) { Text(tr("Понятно", "Got it")) }
        },
    )
}
