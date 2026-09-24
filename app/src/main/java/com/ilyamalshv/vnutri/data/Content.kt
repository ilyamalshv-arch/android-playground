package com.ilyamalshv.vnutri.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Emotion(val id: String, val name: String, val group: String)

object Emotions {
    /** Home screen sections, in display order. */
    val groups = listOf(
        "fear" to "Тревога и страх",
        "loss" to "Грусть и потеря",
        "others" to "Злость и отношения",
        "self" to "Про себя",
        "drained" to "Истощение",
        "body" to "Тело, тяга, качели",
        "awkward" to "Неудобные чувства",
        "light" to "Светлое и смешанное",
    )

    val all = listOf(
        Emotion("anxiety", "Тревога", "fear"),
        Emotion("fear", "Страх", "fear"),
        Emotion("panic", "Паника", "fear"),
        Emotion("dread", "Экзистенциальный ужас", "fear"),
        Emotion("fomo", "Страх упустить", "fear"),
        Emotion("sleepless", "Бессонница, мысли ночью", "fear"),
        Emotion("social_anxiety", "Стеснение, страх людей", "fear"),
        Emotion("uncertainty", "Неопределённость", "fear"),
        Emotion("waiting", "Ожидание", "fear"),
        Emotion("sadness", "Грусть", "loss"),
        Emotion("grief", "Горе, утрата", "loss"),
        Emotion("loneliness", "Одиночество", "loss"),
        Emotion("heartbreak", "Разбитое сердце", "loss"),
        Emotion("uprooted", "Оторванность от дома", "loss"),
        Emotion("regret", "Сожаление", "loss"),
        Emotion("longing", "Тоска по человеку", "loss"),
        Emotion("world_pain", "Боль за мир, новости", "loss"),
        Emotion("anger", "Злость", "others"),
        Emotion("resentment", "Обида", "others"),
        Emotion("betrayal", "Предательство", "others"),
        Emotion("jealousy", "Ревность", "others"),
        Emotion("envy", "Зависть", "others"),
        Emotion("rejection", "Отвержение", "others"),
        Emotion("abandonment", "Страх быть брошенным", "others"),
        Emotion("unrequited", "Безответная любовь", "others"),
        Emotion("humiliation", "Унижение", "others"),
        Emotion("shame", "Стыд", "self"),
        Emotion("guilt", "Вина", "self"),
        Emotion("self_hatred", "Ненависть к себе", "self"),
        Emotion("impostor", "Синдром самозванца", "self"),
        Emotion("helplessness", "Беспомощность", "self"),
        Emotion("confusion", "Растерянность", "self"),
        Emotion("body_shame", "Недовольство телом", "self"),
        Emotion("fatigue", "Усталость, выгорание", "drained"),
        Emotion("emptiness", "Пустота, апатия", "drained"),
        Emotion("boredom", "Скука", "drained"),
        Emotion("overload", "Перегруз, инфошум", "drained"),
        Emotion("procrastination", "Не могу начать", "drained"),
        Emotion("unreality", "Всё как не по-настоящему", "drained"),
        Emotion("craving", "Тяга", "body"),
        Emotion("addiction", "Зависимость", "body"),
        Emotion("hangover", "Похмелье", "body"),
        Emotion("euphoria", "Вспышка эйфории", "body"),
        Emotion("crash", "Резкий спад, откат", "body"),
        Emotion("illness", "Болезнь, тело подводит", "body"),
        Emotion("schadenfreude", "Злорадство", "awkward"),
        Emotion("contempt", "Презрение", "awkward"),
        Emotion("disgust", "Отвращение", "awkward"),
        Emotion("revenge", "Желание отомстить", "awkward"),
        Emotion("anger_at_loved", "Злость на близких", "awkward"),
        Emotion("ambivalence", "Люблю и не выношу", "awkward"),
        Emotion("guilty_relief", "Облегчение, за которое стыдно", "awkward"),
        Emotion("indifference", "Равнодушие, где «должно» трогать", "awkward"),
        Emotion("escape", "Всё бросить и сбежать", "awkward"),
        Emotion("forbidden_attraction", "Влечение, которое «нельзя»", "awkward"),
        Emotion("nostalgia", "Ностальгия", "light"),
        Emotion("melancholy", "Светлая печаль", "light"),
        Emotion("pride", "Гордость", "light"),
        Emotion("joy", "Радость", "light"),
        Emotion("love", "Любовь, нежность", "light"),
        Emotion("gratitude", "Благодарность", "light"),
        Emotion("hope", "Надежда", "light"),
        Emotion("awe", "Трепет", "light"),
        Emotion("relief", "Облегчение", "light"),
        Emotion("calm", "Покой", "light"),
        Emotion("inspiration", "Вдохновение", "light"),
    )
    private val byId = all.associateBy { it.id }

    fun name(id: String): String = byId[id]?.name ?: id
}

data class Family(val id: String, val name: String)

object Families {
    val all = listOf(
        Family("classic", "Античность, Средневековье и Восток"),
        Family("early_modern", "XVI–XVIII века"),
        Family("nineteenth", "XIX век"),
        Family("phenomenology_existential", "Феноменология и экзистенциализм"),
        Family("psychoanalysis_critical", "Психоанализ и критическая теория"),
        Family("analytic", "Аналитическая философия и прагматизм"),
        Family("poststructuralism", "Постструктурализм"),
        Family("affect_theory", "Теория аффекта и наследники"),
        Family("contemporary", "XXI век"),
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

        fun load(context: Context): Library {
            val files = context.assets.list(DIR).orEmpty().filter { it.endsWith(".json") }
            val schools = files.mapNotNull { name ->
                runCatching {
                    val raw = context.assets.open("$DIR/$name").bufferedReader().use { it.readText() }
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
