package com.ilyamalshv.vnutri.data

/**
 * Crisis detection for free text. Deliberately errs on the side of showing help:
 * a false positive costs one extra screen, a false negative can cost far more.
 */
object Safety {
    private val patterns = listOf(
        "суицид", "самоубий", "самоповрежд",
        "поконч\\p{L}*\\s+с\\s+собой",
        "(убить|убью|убиваю)\\s+себя",
        "не\\s+(хочу|хочется|могу|желаю)\\s+(больше\\s+)?жить",
        "жить\\s+(больше\\s+)?не\\s+(хочу|хочется)",
        "нет\\s+смысла\\s+жить", "зачем\\s+(мне\\s+)?(дальше\\s+)?жить",
        "(хочу|хочется)\\s+(просто\\s+)?(умереть|сдохнуть|исчезнуть)",
        "лучше\\s+бы\\s+меня\\s+не\\s+было", "(всем|им)\\s+будет\\s+лучше\\s+без\\s+меня",
        "повес\\p{L}*ся", "повешусь",
        "вскр\\p{L}*\\s+вены",
        "(порезать|порежу|резать|режу|резала|резал)\\s+себя",
        "причин\\p{L}*\\s+себе\\s+(боль|вред)",
        "наглотат\\p{L}*\\s+таблет", "выпилит\\p{L}*ся", "выпилюсь",
        "(выйти|шагнуть|выпрыгнуть)\\s+(в|из)\\s+окн",
        "(спрыгнуть|прыгнуть)\\s+с\\s+(крыши|моста)",
        "не\\s+проснуться",
        "suicid", "kill\\s+myself", "want\\s+to\\s+die", "self[- ]?harm", "end\\s+my\\s+life",
        "(don'?t|do\\s+not)\\s+want\\s+to\\s+(live|be\\s+alive|wake\\s+up)", "no\\s+reason\\s+to\\s+live",
        "(hurt|cut|harm)(ing)?\\s+myself", "better\\s+off\\s+without\\s+me", "end\\s+it\\s+all", "take\\s+my\\s+(own\\s+)?life",
    ).map { Regex(it) }

    private val heavy = setOf(
        "anxiety", "fear", "sadness", "grief", "loneliness", "shame", "guilt", "emptiness",
        "panic", "dread", "self_hatred", "helplessness", "heartbreak", "betrayal", "crash",
    )

    fun isCrisis(text: String): Boolean {
        val t = text.lowercase().replace('ё', 'е')
        return patterns.any { it.containsMatchIn(t) }
    }

    /** Several heavy feelings at full strength: show a gentle, non-blocking offer of support. */
    fun isHeavy(marks: List<EmotionMark>): Boolean =
        marks.count { it.intensity >= 3 && it.emotionId in heavy } >= 2

    data class Resource(val title: String, val details: String, val phone: String? = null, val url: String? = null)

    val resources: List<Resource>
        get() = if (Lang.isEn) resourcesEn else resourcesRu

    private val resourcesRu = listOf(
        Resource("Экстренные службы — 112", "Если есть непосредственная опасность. Работает в России, Украине, Казахстане, странах ЕС и многих других.", phone = "112"),
        Resource("Россия · Экстренная психологическая помощь МЧС", "+7 495 989-50-50 · круглосуточно, бесплатно", phone = "+74959895050"),
        Resource("Россия · Телефон доверия", "8 800 2000 122 · для детей, подростков и их родителей, бесплатно", phone = "88002000122"),
        Resource("Украина · Lifeline Ukraine", "7333 · круглосуточно, бесплатно с мобильных", phone = "7333"),
        Resource("Другие страны", "findahelpline.com — бесплатные линии помощи по всему миру", url = "https://findahelpline.com"),
    )

    private val resourcesEn = listOf(
        Resource("Emergency services — 112 / 911", "If there is immediate danger. 112 works across the EU and many countries; 911 in the US and Canada.", phone = "112"),
        Resource("US · 988 Suicide & Crisis Lifeline", "Call or text 988 · 24/7, free, confidential", phone = "988"),
        Resource("UK & Ireland · Samaritans", "116 123 · 24/7, free", phone = "116123"),
        Resource("Anywhere else", "findahelpline.com — free helplines worldwide", url = "https://findahelpline.com"),
    )
}
