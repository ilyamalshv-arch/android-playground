package com.ilyamalshv.vnutri.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Emotion(val id: String, val ru: String, val group: String, val en: String = ru, val es: String = en) {
    val name: String get() = tr(ru, en, es)
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
        Emotion("anxiety", "Тревога", "fear", "Anxiety", "Ansiedad"),
        Emotion("fear", "Страх", "fear", "Fear", "Miedo"),
        Emotion("panic", "Паника", "fear", "Panic", "Pánico"),
        Emotion("dread", "Экзистенциальный ужас", "fear", "Existential dread", "Angustia existencial"),
        Emotion("fomo", "Страх упустить", "fear", "Fear of missing out", "Miedo a perderme algo"),
        Emotion("sleepless", "Бессонница, мысли ночью", "fear", "Sleepless, night thoughts", "Insomnio, pensamientos de noche"),
        Emotion("social_anxiety", "Стеснение, страх людей", "fear", "Shyness, fear of people", "Timidez, miedo a la gente"),
        Emotion("uncertainty", "Неопределённость", "fear", "Uncertainty", "Incertidumbre"),
        Emotion("waiting", "Ожидание", "fear", "Waiting", "Espera"),
        Emotion("sadness", "Грусть", "loss", "Sadness", "Tristeza"),
        Emotion("grief", "Горе, утрата", "loss", "Grief, loss", "Duelo, pérdida"),
        Emotion("loneliness", "Одиночество", "loss", "Loneliness", "Soledad"),
        Emotion("heartbreak", "Разбитое сердце", "loss", "Heartbreak", "Corazón roto"),
        Emotion("uprooted", "Оторванность от дома", "loss", "Uprooted from home", "Desarraigo"),
        Emotion("regret", "Сожаление", "loss", "Regret", "Arrepentimiento"),
        Emotion("longing", "Тоска по человеку", "loss", "Missing someone", "Extrañar a alguien"),
        Emotion("world_pain", "Боль за мир, новости", "loss", "Pain for the world, the news", "Dolor por el mundo, las noticias"),
        Emotion("anger", "Злость", "others", "Anger", "Enojo"),
        Emotion("resentment", "Обида", "others", "Resentment", "Resentimiento"),
        Emotion("betrayal", "Предательство", "others", "Betrayal", "Traición"),
        Emotion("jealousy", "Ревность", "others", "Jealousy", "Celos"),
        Emotion("envy", "Зависть", "others", "Envy", "Envidia"),
        Emotion("rejection", "Отвержение", "others", "Rejection", "Rechazo"),
        Emotion("abandonment", "Страх быть брошенным", "others", "Fear of being left", "Miedo a que me dejen"),
        Emotion("unrequited", "Безответная любовь", "others", "Unrequited love", "Amor no correspondido"),
        Emotion("humiliation", "Унижение", "others", "Humiliation", "Humillación"),
        Emotion("shame", "Стыд", "self", "Shame", "Vergüenza"),
        Emotion("guilt", "Вина", "self", "Guilt", "Culpa"),
        Emotion("self_hatred", "Ненависть к себе", "self", "Self-hatred", "Odio hacia mí"),
        Emotion("impostor", "Синдром самозванца", "self", "Impostor syndrome", "Síndrome del impostor"),
        Emotion("helplessness", "Беспомощность", "self", "Helplessness", "Impotencia"),
        Emotion("confusion", "Растерянность", "self", "Confusion", "Confusión"),
        Emotion("body_shame", "Недовольство телом", "self", "Unhappy with my body", "Disconformidad con mi cuerpo"),
        Emotion("fatigue", "Усталость, выгорание", "drained", "Fatigue, burnout", "Cansancio, burnout"),
        Emotion("emptiness", "Пустота, апатия", "drained", "Emptiness, apathy", "Vacío, apatía"),
        Emotion("boredom", "Скука", "drained", "Boredom", "Aburrimiento"),
        Emotion("overload", "Перегруз, инфошум", "drained", "Overload, info noise", "Saturación, ruido"),
        Emotion("procrastination", "Не могу начать", "drained", "Can't get started", "No puedo arrancar"),
        Emotion("unreality", "Всё как не по-настоящему", "drained", "Nothing feels real", "Nada parece real"),
        Emotion("craving", "Тяга", "body", "Craving", "Ganas, antojo"),
        Emotion("addiction", "Зависимость", "body", "Addiction", "Adicción"),
        Emotion("hangover", "Похмелье", "body", "Hangover", "Resaca"),
        Emotion("euphoria", "Вспышка эйфории", "body", "Burst of euphoria", "Pico de euforia"),
        Emotion("crash", "Резкий спад, откат", "body", "Sudden crash", "Bajón repentino"),
        Emotion("illness", "Болезнь, тело подводит", "body", "Illness, body letting me down", "Enfermedad, el cuerpo me falla"),
        Emotion("schadenfreude", "Злорадство", "awkward", "Schadenfreude", "Alegrarme del mal ajeno"),
        Emotion("contempt", "Презрение", "awkward", "Contempt", "Desprecio"),
        Emotion("disgust", "Отвращение", "awkward", "Disgust", "Asco"),
        Emotion("revenge", "Желание отомстить", "awkward", "Wanting revenge", "Ganas de venganza"),
        Emotion("anger_at_loved", "Злость на близких", "awkward", "Anger at loved ones", "Enojo con los míos"),
        Emotion("ambivalence", "Люблю и не выношу", "awkward", "Love and can't stand", "Lo quiero y no lo soporto"),
        Emotion("guilty_relief", "Облегчение, за которое стыдно", "awkward", "Relief I feel guilty about", "Alivio que me da culpa"),
        Emotion("indifference", "Равнодушие, где «должно» трогать", "awkward", "Indifference where I “should” care", "Indiferencia donde «debería» importarme"),
        Emotion("escape", "Всё бросить и сбежать", "awkward", "Wanting to drop everything and run", "Ganas de dejar todo e irme"),
        Emotion("forbidden_attraction", "Влечение, которое «нельзя»", "awkward", "An attraction I “shouldn't” feel", "Una atracción que «no debería»"),
        Emotion("nostalgia", "Ностальгия", "light", "Nostalgia", "Nostalgia"),
        Emotion("melancholy", "Светлая печаль", "light", "Bright sadness", "Tristeza luminosa"),
        Emotion("pride", "Гордость", "light", "Pride", "Orgullo"),
        Emotion("joy", "Радость", "light", "Joy", "Alegría"),
        Emotion("love", "Любовь, нежность", "light", "Love, tenderness", "Amor, ternura"),
        Emotion("gratitude", "Благодарность", "light", "Gratitude", "Gratitud"),
        Emotion("hope", "Надежда", "light", "Hope", "Esperanza"),
        Emotion("awe", "Трепет", "light", "Awe", "Asombro"),
        Emotion("relief", "Облегчение", "light", "Relief", "Alivio"),
        Emotion("calm", "Покой", "light", "Calm", "Calma"),
        Emotion("inspiration", "Вдохновение", "light", "Inspiration", "Inspiración"),
    )
    private val byId = all.associateBy { it.id }

    fun name(id: String): String = byId[id]?.name ?: id
}

data class Family(val id: String, val ru: String, val en: String, val es: String = en) {
    val name: String get() = tr(ru, en, es)
}

object Families {
    val all = listOf(
        Family("classic", "Античность, Средневековье и Восток", "Antiquity, Middle Ages and the East", "Antigüedad, Edad Media y Oriente"),
        Family("early_modern", "XVI–XVIII века", "16th–18th centuries", "Siglos XVI–XVIII"),
        Family("nineteenth", "XIX век", "19th century", "Siglo XIX"),
        Family("phenomenology_existential", "Феноменология и экзистенциализм", "Phenomenology and existentialism", "Fenomenología y existencialismo"),
        Family("psychoanalysis_critical", "Психоанализ и критическая теория", "Psychoanalysis and critical theory", "Psicoanálisis y teoría crítica"),
        Family("analytic", "Аналитическая философия и прагматизм", "Analytic philosophy and pragmatism", "Filosofía analítica y pragmatismo"),
        Family("poststructuralism", "Постструктурализм", "Poststructuralism", "Postestructuralismo"),
        Family("affect_theory", "Теория аффекта и наследники", "Affect theory and its heirs", "Teoría de los afectos y herederos"),
        Family("contemporary", "XXI век", "21st century", "Siglo XXI"),
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
        private const val DIR_ES = "content/schools_es"

        /** English texts come from schools_en; a school without a translation falls back to Russian. */
        fun load(context: Context, lang: String): Library {
            val files = context.assets.list(DIR).orEmpty().filter { it.endsWith(".json") }
            val langDir = when (lang) { "en" -> DIR_EN; "es" -> DIR_ES; else -> null }
            val translated = langDir?.let { context.assets.list(it).orEmpty().toSet() }.orEmpty()
            // In a translated language, show only translated schools rather than mixing languages.
            val shown = if (translated.isNotEmpty()) files.filter { it in translated } else files
            val schools = shown.mapNotNull { name ->
                runCatching {
                    val dir = if (name in translated && langDir != null) langDir else DIR
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
