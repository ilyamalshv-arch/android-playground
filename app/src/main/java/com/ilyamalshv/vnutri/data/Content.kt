package com.ilyamalshv.vnutri.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Emotion(val id: String, val name: String, val light: Boolean)

object Emotions {
    val all = listOf(
        Emotion("anxiety", "Тревога", false),
        Emotion("fear", "Страх", false),
        Emotion("sadness", "Грусть", false),
        Emotion("grief", "Горе, утрата", false),
        Emotion("loneliness", "Одиночество", false),
        Emotion("anger", "Злость", false),
        Emotion("shame", "Стыд", false),
        Emotion("guilt", "Вина", false),
        Emotion("envy", "Зависть", false),
        Emotion("emptiness", "Пустота, апатия", false),
        Emotion("confusion", "Растерянность", false),
        Emotion("fatigue", "Усталость, выгорание", false),
        Emotion("boredom", "Скука", false),
        Emotion("nostalgia", "Ностальгия", true),
        Emotion("joy", "Радость", true),
        Emotion("love", "Любовь, нежность", true),
        Emotion("gratitude", "Благодарность", true),
        Emotion("hope", "Надежда", true),
    )
    private val byId = all.associateBy { it.id }

    fun name(id: String): String = byId[id]?.name ?: id
}

data class Family(val id: String, val name: String)

object Families {
    val all = listOf(
        Family("classic", "Античность и Восток"),
        Family("early_modern", "XVII–XVIII века"),
        Family("nineteenth", "XIX век"),
        Family("phenomenology_existential", "Феноменология и экзистенциализм"),
        Family("psychoanalysis_critical", "Психоанализ и критическая теория"),
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
