package com.ilyamalshv.vnutri.data

/**
 * Offline guess of feelings from free text by Russian word stems.
 * Only ever used as a suggestion the person can accept or ignore.
 */
object EmotionGuess {
    // (?<!\p{L}) = start of a word, so "рад" does not fire inside "парад".
    private fun w(vararg stems: String) = stems.map { Regex("(?<!\\p{L})$it") }

    private val rules: List<Pair<String, List<Regex>>> = listOf(
        "anxiety" to w("тревож", "тревог", "беспоко", "волнуюсь", "волнуе", "нервнич", "нервы", "паник", "места себе не", "напряж", "переживаю", "накруч"),
        "fear" to w("страх", "страш", "боюсь", "боится", "боязн", "пуга", "испуг", "ужас"),
        "sadness" to w("грус", "грущ", "печал", "тоск", "плачу", "плакать", "плач", "слез", "уныл", "хандр", "тяжело на душе"),
        "grief" to w("умер", "умерл", "смерт", "похорон", "потерял", "потеряла", "потеря", "утрат", "горе(?!\\p{L})", "скорб", "не стало", "погиб", "развод", "расстал"),
        "loneliness" to w("одинок", "никому не нуж", "не с кем", "никто не понима", "никто не пишет", "покинут", "брошен", "сам по себе", "одна дома", "один дома"),
        "anger" to w("зл(ость|юсь|ой|ая|ит|ишь|юс)", "бесит", "бесят", "бешус", "раздраж", "ярост", "гнев", "ненавиж", "обид", "достал", "взбес"),
        "shame" to w("стыд", "стыж", "позор", "неловк", "опозор", "унизи", "униж", "краснею"),
        "guilt" to w("винова", "вина(?!\\p{L})", "виню", "совест", "не могу себе простить", "подвел", "подвела", "из-за меня"),
        "envy" to w("завид", "завист", "у всех есть", "у других получается", "везет же"),
        "emptiness" to w("пуст", "апати", "ничего не хочу", "ничего не чувству", "безразлич", "все равно", "без разницы", "онемел"),
        "confusion" to w("растерян", "запута", "не понимаю", "не знаю что", "не знаю как", "не знаю, что", "не знаю, как", "сомнева", "не могу решить", "потерялся", "потерялась"),
        "fatigue" to w("устал", "усталост", "выгор", "нет сил", "сил нет", "измот", "вымота", "истощ", "не высыпа", "замучил", "выдохся", "выдохлась"),
        "boredom" to w("скуч(?!аю по)", "скук", "однообраз", "рутин", "нечем заняться", "день сурка"),
        "nostalgia" to w("ностальг", "скучаю по", "вспоминаю", "раньше было", "в детстве", "прошл(ое|ого|ым)", "как раньше"),
        "joy" to w("рад(а|ость|остн|уюсь|уе|)(?!\\p{L})", "счаст", "весел", "восторг", "кайф", "ликую", "здорово", "прекрасно"),
        "love" to w("люблю", "любов", "любим", "влюб", "нежн", "обожаю"),
        "gratitude" to w("благодар", "спасибо", "признател", "ценю"),
        "hope" to w("надеж", "надеюсь", "надея", "верю", "мечт", "получится", "все наладится"),
        "craving" to w("тянет", "тяга", "хочется выпить", "хочу выпить", "сорваться", "сорвался", "сорвалась", "хочу курить", "хочется курить", "не могу оторваться"),
        "addiction" to w("зависим", "подсел", "подсела", "бухаю", "запой", "не могу бросить", "не могу завязать", "торчу", "игроман"),
        "hangover" to w("похмел", "бодун", "перепил", "перепила", "вчера напил", "ничего не помню", "отходняк"),
        "euphoria" to w("эйфори", "на подъеме", "все могу", "лечу", "окрыл", "меня прет"),
        "crash" to w("откат", "накрыло", "упало настроение", "резко стало плохо", "после праздника", "сдулся", "сдулась", "спад"),
        "panic" to w("паник", "задыхаюсь", "сердце колотится", "сердце выпрыгивает", "не хватает воздуха", "сейчас умру", "схожу с ума"),
        "dread" to w("зачем все это", "бездн", "экзистенц", "конечност", "все умрем", "в чем смысл"),
        "fomo" to w("упускаю", "упустить", "все живут", "у всех жизнь", "жизнь проходит мимо", "фомо", "fomo"),
        "sleepless" to w("не могу уснуть", "не спится", "бессонниц", "не сплю", "3 часа ночи", "три часа ночи", "проснулся ночью", "проснулась ночью"),
        "betrayal" to w("предал", "предательств", "изменил", "изменила", "обманул", "обманула", "нож в спину", "за спиной"),
        "jealousy" to w("ревну", "ревност", "ревнив"),
        "resentment" to w("обид", "не могу простить", "до сих пор злюсь"),
        "heartbreak" to w("расстал", "бросил", "бросила", "разбито сердце", "разбил сердце", "разлюбил", "бывш", "ушла от меня", "ушел от меня"),
        "impostor" to w("самозван", "не заслуж", "разоблач", "недостаточно хорош", "мне просто повезло"),
        "self_hatred" to w("ненавижу себя", "ненависть к себе", "я ничтожеств", "я никчем", "я урод", "противен себе", "противна себе", "я ужасный", "я ужасная"),
        "helplessness" to w("беспомощ", "бессили", "ничего не могу сделать", "ничего не изменить", "от меня ничего не зависит", "руки опускаются"),
        "procrastination" to w("прокрастин", "не могу начать", "откладываю", "не могу заставить себя", "дедлайн"),
        "uprooted" to w("эмигр", "переехал", "переехала", "релокац", "чужая страна", "чужой стране", "скучаю по дому", "нет дома", "уехал", "уехала"),
        "overload" to w("перегруз", "слишком много информации", "лента", "думскрол", "новост", "уведомлен", "голова пухнет", "шум в голове"),
        "unreality" to w("нереальн", "не по-настоящему", "как во сне", "как в тумане", "как будто не я", "дереализ", "деперсонализ", "как в кино"),
        "awe" to w("трепет", "благоговен", "дух захватывает", "величествен", "мурашки"),
        "relief" to w("облегчен", "отпустило", "гора с плеч", "выдохнул", "выдохнула", "наконец-то"),
        "calm" to w("спокойн", "покой", "умиротвор", "тихо на душе", "безмятеж"),
        "inspiration" to w("вдохнов", "муза", "хочу творить", "идея", "загорел"),
        "schadenfreude" to w("злорад", "так ему и надо", "так ей и надо", "так им и надо", "приятно, что у него не", "приятно, что у нее не"),
        "contempt" to w("презира", "презрен", "жалкие люди", "ничтожеств(о|а) они"),
        "disgust" to w("отвращен", "противно", "мерзко", "тошнит от", "гадко"),
        "revenge" to w("отомст", "месть", "мщени", "поквитаться", "расплатится", "пусть почувствует"),
        "anger_at_loved" to w("наорал", "наорала", "сорвался на", "сорвалась на", "злюсь на (мужа|жену|маму|папу|мать|отца|ребенка|детей|сына|дочь|партнер)", "бесит (муж|жена|мама|папа|ребенок|сын|дочь)"),
        "ambivalence" to w("люблю и ненавижу", "и люблю, и", "смешанные чувства", "двоякое чувство", "не знаю, люблю ли"),
        "guilty_relief" to w("стыдно, что стало легче", "мне стало легче, и", "облегчение и вина", "облегчение, и стыдно"),
        "indifference" to w("равнодуш", "ничего не почувствовал", "ничего не почувствовала", "мне все равно на", "не трогает"),
        "escape" to w("все бросить", "сбежать", "уехать куда", "уволиться и", "уйти от всего", "начать с нуля"),
        "forbidden_attraction" to w("влюбил(ся|ась) в женат", "влюбил(ся|ась) в замуж", "нельзя, но тянет", "запретн", "не тот человек", "нравится чужой", "нравится чужая"),
        "social_anxiety" to w("стесня", "стеснен", "неловко при людях", "боюсь людей", "боюсь выступ", "что обо мне подумают", "осудят"),
        "uncertainty" to w("неопредел", "неизвестност", "непонятно, что будет", "не знаю, что будет", "подвешен"),
        "waiting" to w("жду ответ", "жду результ", "жду новост", "в ожидании", "ожидани", "жду, когда"),
        "regret" to w("сожалею", "сожален", "жалею", "надо было", "зря я", "упустил", "упустила", "если бы я тогда"),
        "longing" to w("скучаю по (нему|ней|тебе|маме|папе)", "тоскую по", "не хватает (его|ее|тебя)", "хочу, чтобы (он|она) был"),
        "world_pain" to w("войн", "новост", "за мир", "несправедлив", "обстрел", "беженц", "страшно за людей", "больно за"),
        "rejection" to w("отверг", "отказал(и|а)? мне", "не взяли", "отказ ", "меня не выбрал", "игнорир", "не отвечает"),
        "abandonment" to w("бросит меня", "боюсь, что уйдет", "боюсь, что он уйдет", "боюсь, что она уйдет", "оставят одн", "брошенн"),
        "unrequited" to w("безответн", "не любит меня", "не взаимн", "невзаимн", "любовь без ответа"),
        "humiliation" to w("униж", "унизил", "опозор", "высмеял", "смеялись надо мной", "растоптал"),
        "body_shame" to w("ненавижу свое тело", "толст", "некрасив", "уродлив", "стыдно за тело", "мое тело", "в зеркало"),
        "illness" to w("болею", "болезн", "диагноз", "больниц", "здоровье", "болит", "врач сказал", "операци"),
        "pride" to w("горжусь", "гордост", "у меня получилось", "справился", "справилась", "я смог", "я смогла"),
        "melancholy" to w("меланхол", "светлая печаль", "светлая грусть", "осенн", "грустно, но хорошо"),
    )

    // English cues (word starts, lowercase). Kept short: the AI button handles subtle cases.
    private fun e(vararg stems: String) = stems.map { Regex("(?<![a-z])$it") }

    private val rulesEn: List<Pair<String, List<Regex>>> = listOf(
        "anxiety" to e("anxi", "worr", "nervous", "on edge", "overthink"),
        "fear" to e("afraid", "scared", "fear", "terrif", "frighten"),
        "panic" to e("panic", "can'?t breathe", "heart (is )?racing", "going crazy"),
        "dread" to e("dread", "what'?s the point", "meaningless", "mortality"),
        "fomo" to e("missing out", "fomo", "everyone else is", "life is passing"),
        "sleepless" to e("can'?t sleep", "insomnia", "awake at", "3 ?am"),
        "social_anxiety" to e("shy", "awkward around", "what people think", "judged", "social anxiety"),
        "uncertainty" to e("uncertain", "don'?t know what will happen", "unknown", "limbo"),
        "waiting" to e("waiting", "waiting for", "still no answer"),
        "sadness" to e("sad", "cry", "crying", "tears", "down", "blue"),
        "grief" to e("died", "death", "passed away", "funeral", "grief", "griev", "loss of"),
        "loneliness" to e("lonely", "alone", "no one to talk", "nobody (cares|understands)", "isolated"),
        "heartbreak" to e("broke up", "breakup", "break-up", "dumped", "heartbr", "my ex"),
        "uprooted" to e("emigrat", "moved abroad", "relocat", "homesick", "foreign country"),
        "regret" to e("regret", "should have", "wish i had", "if only"),
        "longing" to e("miss (him|her|you|them|my)", "long for", "yearn"),
        "world_pain" to e("war", "the news", "injustice", "refugee", "the world is"),
        "anger" to e("angry", "anger", "furious", "pissed", "mad at", "rage"),
        "resentment" to e("resent", "can'?t forgive", "still hurt by", "grudge"),
        "betrayal" to e("betray", "cheated on", "lied to me", "stabbed in the back"),
        "jealousy" to e("jealous"),
        "envy" to e("envy", "envious", "they have everything"),
        "rejection" to e("rejected", "rejection", "ignored me", "turned me down", "ghosted"),
        "abandonment" to e("will leave me", "afraid (he|she|they) will leave", "abandon"),
        "unrequited" to e("unrequited", "doesn'?t love me back", "one-sided"),
        "humiliation" to e("humiliat", "laughed at me", "made fun of"),
        "shame" to e("ashamed", "shame", "embarrass"),
        "guilt" to e("guilt", "my fault", "blame myself", "let (them|her|him) down"),
        "self_hatred" to e("hate myself", "i'?m worthless", "i'?m disgusting", "i'?m a failure"),
        "impostor" to e("impostor", "imposter", "don'?t deserve", "found out", "fraud"),
        "helplessness" to e("helpless", "powerless", "nothing i can do"),
        "confusion" to e("confused", "lost", "don'?t know what to do", "torn"),
        "body_shame" to e("hate my body", "fat", "ugly", "my body"),
        "fatigue" to e("tired", "exhausted", "burn(ed|t)? ?out", "drained", "no energy"),
        "emptiness" to e("empty", "numb", "apath", "don'?t care about anything"),
        "boredom" to e("bored", "boring", "nothing to do", "same every day"),
        "overload" to e("overwhelm", "too much information", "doomscroll", "notifications", "overload"),
        "procrastination" to e("procrastinat", "can'?t start", "keep putting off", "deadline"),
        "unreality" to e("unreal", "not real", "like a dream", "derealiz", "depersonaliz", "foggy"),
        "craving" to e("craving", "crave", "want a drink", "relapse", "urge to"),
        "addiction" to e("addict", "can'?t quit", "can'?t stop drinking", "hooked", "binge"),
        "hangover" to e("hangover", "hungover", "drank too much", "blackout", "last night i drank"),
        "euphoria" to e("euphori", "on top of the world", "high as", "buzzing"),
        "crash" to e("crash", "come ?down", "after the high", "mood dropped"),
        "illness" to e("sick", "ill", "illness", "diagnos", "hospital", "pain in"),
        "schadenfreude" to e("serves (him|her|them) right", "glad (he|she|they) failed", "schadenfreude"),
        "contempt" to e("contempt", "despise", "pathetic"),
        "disgust" to e("disgust", "gross", "repuls", "sickening"),
        "revenge" to e("revenge", "get back at", "make (him|her|them) pay"),
        "anger_at_loved" to e("yelled at (my )?(kid|child|son|daughter|partner|wife|husband|mom|dad)", "snapped at", "angry (at|with) my (mom|dad|mother|father|partner|wife|husband|kid|child)"),
        "ambivalence" to e("love and hate", "mixed feelings", "torn about (him|her|them)"),
        "guilty_relief" to e("relieved and guilty", "guilty for feeling relieved", "feel relief and"),
        "indifference" to e("indifferent", "felt nothing", "should care"),
        "escape" to e("run away", "drop everything", "quit everything", "start over", "escape"),
        "forbidden_attraction" to e("married (man|woman)", "shouldn'?t want", "forbidden", "attracted to (my|a) (friend|colleague|boss)"),
        "nostalgia" to e("nostalg", "the old days", "when i was young", "childhood"),
        "melancholy" to e("melanchol", "bittersweet", "wistful"),
        "pride" to e("proud", "i did it", "managed to"),
        "joy" to e("happy", "joy", "glad", "delight"),
        "love" to e("love", "tender", "in love", "adore"),
        "gratitude" to e("grateful", "thankful", "gratitude", "thank"),
        "hope" to e("hope", "hopeful", "optimis"),
        "awe" to e("awe", "breathtaking", "vast", "sublime"),
        "relief" to e("relief", "relieved", "finally over", "weight off"),
        "calm" to e("calm", "peaceful", "serene", "at peace"),
        "inspiration" to e("inspir", "creative", "new idea", "motivated"),
    )

    // Broad feelings get matched by many everyday words («страшно», «грустно»); specific states
    // («похмелье», «предательство») usually appear once but say much more — so they weigh more.
    private val broad = setOf(
        "anxiety", "fear", "sadness", "anger", "joy", "love", "hope", "shame", "guilt",
        "confusion", "fatigue", "emptiness", "loneliness", "calm", "boredom",
    )

    /** Up to [limit] feelings, ordered by weighted number of cues. */
    fun guess(text: String, limit: Int = 4): List<String> {
        val t = text.lowercase().replace('ё', 'е')
        if (t.isBlank()) return emptyList()
        val cyrillic = t.count { it in 'а'..'я' } >= t.count { it in 'a'..'z' }
        return (if (cyrillic) rules else rulesEn)
            .map { (id, patterns) ->
                val hits = patterns.sumOf { p -> p.findAll(t).count() }
                id to if (id in broad) hits else hits * 3
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
