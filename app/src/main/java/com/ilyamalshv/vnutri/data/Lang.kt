package com.ilyamalshv.vnutri.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Current UI language ("ru" or "en"). Backed by Compose state, so the whole UI recomposes on switch. */
object Lang {
    var current by mutableStateOf("ru")
    val isEn: Boolean get() = current == "en"
}

/** Picks the string for the current language; usable in composables and in plain code alike. */
fun tr(ru: String, en: String): String = if (Lang.isEn) en else ru
