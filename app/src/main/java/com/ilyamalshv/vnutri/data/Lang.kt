package com.ilyamalshv.vnutri.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Current UI language: "ru", "en" or "es" (Spanish of Argentina). Compose state, so the UI recomposes on switch. */
object Lang {
    var current by mutableStateOf("ru")
    val isEn: Boolean get() = current == "en"
    val isEs: Boolean get() = current == "es"

    /** Locale tag for dates and similar. */
    val tag: String get() = if (current == "es") "es-AR" else current
}

/**
 * Picks the string for the current language; usable in composables and plain code alike.
 * Spanish comes from [es] when given (needed for strings with templates), else from [EsStrings] by the English text.
 */
fun tr(ru: String, en: String, es: String? = null): String = when (Lang.current) {
    "en" -> en
    "es" -> es ?: EsStrings.map[en] ?: en
    else -> ru
}
