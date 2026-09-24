package com.ilyamalshv.vnutri.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Emotion(val id: String, val ru: String, val group: String, val en: String = ru) {
    val name: String get() = tr(ru, en)
}

object Emotions {
    /** Home screen sections, in display order. */
    val groups get() = listOf(
        "fear" to tr("Тревога и страх", "Anxiety and fear"),
        "loss" to tr("Грусть и потеря", "Sadness and loss"),
        "others" to tr("Злость и отношения", "Anger and relationships"),
        "self" to tr("Про себя", "About myself"),
        "drained" to tr("Истощение", "Drained"),
        "body" to tr("Тело, тяга, качели", "Body, cravings, swings"),
        "awkward" to tr("Неудобные чувства", "Uncomfortable feelings"),
        "light" to tr("Светлое и смешанное", "Light and mixed"),
    )

    val all = listOf(
        Emotion("anxiety", "Тревога", "fear", "Anxiety"),
        Emotion("fear", "Страх", "fear", "Fear"),
        Emotion("panic", "Паника", "fear", "Panic"),
        Emotion("dread", "Экзистенциальный ужас", "fear", "Existential dread"),
        Emotion("fomo", "Страх упустить", "fear", "Fear of missing out"),
        Emotion("sleepless", "Бессонница, мысли ночью", "fear", "Sleepless, night thoughts"),
        Emotion("social_anxiety", "Стеснение, страх людей", "fear", "Shyness, fear of people"),
        Emotion("uncertainty", "Неопределённость", "fear", "Uncertainty"),
        Emotion("waiting", "Ожидание", "fear", "Waiting"),
        Emotion("sadness", "Грусть", "loss", "Sadness"),
        Emotion("grief", "Горе, утрата", "loss", "Grief, loss"),
        Emotion("loneliness", "Одиночество", "loss", "Loneliness"),
        Emotion("heartbreak", "Разбитое сердце", "loss", "Heartbreak"),
        Emotion("uprooted", "Оторванность от дома", "loss", "Uprooted from home"),
        Emotion("regret", "Сожаление", "loss", "Regret"),
        Emotion("longing", "Тоска по человеку", "loss", "Missing someone"),
        Emotion("world_pain", "Боль за мир, новости", "loss", "Pain for the world, the news"),
        Emotion("anger", "Злость", "others", "Anger"),
        Emotion("resentment", "Обида", "others", "Resentment"),
        Emotion("betrayal", "Предательство", "others", "Betrayal"),
        Emotion("jealousy", "Ревность", "others", "Jealousy"),
        Emotion("envy", "Зависть", "others", "Envy"),
        Emotion("rejection", "Отвержение", "others", "Rejection"),
        Emotion("abandonment", "Страх быть брошенным", "others", "Fear of being left"),
        Emotion("unrequited", "Безответная любовь", "others", "Unrequited love"),
        Emotion("humiliation", "Унижение", "others", "Humiliation"),
        Emotion("shame", "Стыд", "self", "Shame"),
        Emotion("guilt", "Вина", "self", "Guilt"),
        Emotion("self_hatred", "Ненависть к себе", "self", "Self-hatred"),
        Emotion("impostor", "Синдром самозванца", "self", "Impostor syndrome"),
        Emotion("helplessness", "Беспомощность", "self", "Helplessness"),
        Emotion("confusion", "Растерянность", "self", "Confusion"),
        Emotion("body_shame", "Недовольство телом", "self", "Unhappy with my body"),
        Emotion("fatigue", "Усталость, выгорание", "drained", "Fatigue, burnout"),
        Emotion("emptiness", "Пустота, апатия", "drained", "Emptiness, apathy"),
        Emotion("boredom", "Скука", "drained", "Boredom"),
        Emotion("overload", "Перегруз, инфошум", "drained", "Overload, info noise"),
        Emotion("procrastination", "Не могу начать", "drained", "Can't get started"),
        Emotion("unreality", "Всё как не по-настоящему", "drained", "Nothing feels real"),
        Emotion("craving", "Тяга", "body", "Craving"),
        Emotion("addiction", "Зависимость", "body", "Addiction"),
        Emotion("hangover", "Похмелье", "body", "Hangover"),
        Emotion("euphoria", "Вспышка эйфории", "body", "Burst of euphoria"),
        Emotion("crash", "Резкий спад, откат", "body", "Sudden crash"),
        Emotion("illness", "Болезнь, тело подводит", "body", "Illness, body letting me down"),
        Emotion("schadenfreude", "Злорадство", "awkward", "Schadenfreude"),
        Emotion("contempt", "Презрение", "awkward", "Contempt"),
        Emotion("disgust", "Отвращение", "awkward", "Disgust"),
        Emotion("revenge", "Желание отомстить", "awkward", "Wanting revenge"),
        Emotion("anger_at_loved", "Злость на близких", "awkward", "Anger at loved ones"),
        Emotion("ambivalence", "Люблю и не выношу", "awkward", "Love and can't stand"),
        Emotion("guilty_relief", "Облегчение, за которое стыдно", "awkward", "Relief I feel guilty about"),
        Emotion("indifference", "Равнодушие, где «должно» трогать", "awkward", "Indifference where I “should” care"),
        Emotion("escape", "Всё бросить и сбежать", "awkward", "Wanting to drop everything and run"),
        Emotion("forbidden_attraction", "Влечение, которое «нельзя»", "awkward", "An attraction I “shouldn't” feel"),
        Emotion("nostalgia", "Ностальгия", "light", "Nostalgia"),
        Emotion("melancholy", "Светлая печаль", "light", "Bright sadness"),
        Emotion("pride", "Гордость", "light", "Pride"),
        Emotion("joy", "Радость", "light", "Joy"),
        Emotion("love", "Любовь, нежность", "light", "Love, tenderness"),
        Emotion("gratitude", "Благодарность", "light", "Gratitude"),
        Emotion("hope", "Надежда", "light", "Hope"),
        Emotion("awe", "Трепет", "light", "Awe"),
        Emotion("relief", "Облегчение", "light", "Relief"),
        Emotion("calm", "Покой", "light", "Calm"),
        Emotion("inspiration", "Вдохновение", "light", "Inspiration"),
    )
    private val byId = all.associateBy { it.id }

    fun name(id: String): String = byId[id]?.name ?: id
}

data class Family(val id: String, val ru: String, val en: String) {
    val name: String get() = tr(ru, en)
}

object Families {
    val all = listOf(
        Family("classic", "Античность, Средневековье и Восток", "Antiquity, Middle Ages and the East"),
        Family("early_modern", "XVI–XVIII века", "16th–18th centuries"),
        Family("nineteenth", "XIX век", "19th century"),
        Family("phenomenology_existential", "Феноменология и экзистенциализм", "Phenomenology and existentialism"),
        Family("psychoanalysis_critical", "Психоанализ и критическая теория", "Psychoanalysis and critical theory"),
        Family("analytic", "Аналитическая философия и прагматизм", "Analytic philosophy and pragmatism"),
        Family("poststructuralism", "Постструктурализм", "Poststructuralism"),
        Family("affect_theory", "Теория аффекта и наследники", "Affect theory and its heirs"),
        Family("contemporary", "XXI век", "21st century"),
    )

    fun order(id: String): Int = all.indexOfFirst { it.id == id }.let { if (it < 0) all.size else it }
    fun name(id: String): String = all.firstOrNull { it.id == id }?.name ?: id
}

data class Quote(val text: String, val author: String, val source: String)

data class Lens(val text: String, val practice: String, val quote: Quote?)

data class School(
    val id: String,
    val title: String,
    val tradition: String,
    val family: String,
    val period: String,
    val sortYear: Int,
    val thinkers: List<String>,
    val keyWorks: List<String>,
    val summary: String,
    val onEmotions: String,
    val lenses: Map<String, Lens>,
)

class Library(val schools: List<School>) {
    private val byId = schools.associateBy { it.id }

    fun school(id: String): School? = byId[id]

    fun lensesFor(emotionId: String): List<Pair<School, Lens>> =
        schools.mapNotNull { s -> s.lenses[emotionId]?.let { s to it } }

    companion object {
        private const val DIR = "content/schools"
        private const val DIR_EN = "content/schools_en"

        /** English texts come from schools_en; a school without a translation falls back to Russian. */
        fun load(context: Context, lang: String): Library {
            val files = context.assets.list(DIR).orEmpty().filter { it.endsWith(".json") }
            val translated = if (lang == "en") context.assets.list(DIR_EN).orEmpty().toSet() else emptySet()
            // In English, show only translated schools rather than mixing languages.
            val shown = if (translated.isNotEmpty()) files.filter { it in translated } else files
            val schools = shown.mapNotNull { name ->
                runCatching {
                    val dir = if (name in translated) DIR_EN else DIR
                    val raw = context.assets.open("$dir/$name").bufferedReader().use { it.readText() }
                    parse(JSONObject(raw))
                }.getOrNull()
            }
            return Library(schools.sortedWith(compareBy({ Families.order(it.family) }, { it.sortYear })))
        }

        private fun parse(o: JSONObject): School {
            val entries = o.getJSONObject("entries")
            val lenses = mutableMapOf<String, Lens>()
            entries.keys().forEach { key ->
                val e = entries.getJSONObject(key)
                val quote = e.optJSONObject("quote")?.let { q ->
                    Quote(q.optString("text"), q.optString("author"), q.optString("source"))
                }?.takeIf { it.text.isNotBlank() }
                lenses[key] = Lens(e.getString("text"), e.optString("practice"), quote)
            }
            return School(
                id = o.getString("id"),
                title = o.getString("title"),
                tradition = o.optString("tradition"),
                family = o.optString("family"),
                period = o.optString("period"),
                sortYear = o.optInt("sortYear"),
                thinkers = o.optJSONArray("thinkers").strings(),
                keyWorks = o.optJSONArray("keyWorks").strings(),
                summary = o.optString("summary"),
                onEmotions = o.optString("onEmotions"),
                lenses = lenses,
            )
        }

        private fun JSONArray?.strings(): List<String> =
            if (this == null) emptyList() else List(length()) { getString(it) }
    }
}
