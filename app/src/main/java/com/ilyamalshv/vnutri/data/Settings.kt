package com.ilyamalshv.vnutri.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.reflect.KProperty

/** User preferences backed by SharedPreferences and exposed as Compose state. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("vnutri", Context.MODE_PRIVATE)

    var music by BoolPref(prefs, "music", true)
    var sounds by BoolPref(prefs, "sounds", true)
    var haptics by BoolPref(prefs, "haptics", true)
    var intro by BoolPref(prefs, "intro", true)
    var lang by StringPref(prefs, "lang", if (java.util.Locale.getDefault().language == "ru") "ru" else "en")

    // AI mode: the person's own Cloudflare Worker. Never hardcoded — the repo is public.
    var aiUrl by StringPref(prefs, "ai_url")
    var aiToken by StringPref(prefs, "ai_token")
    var aiConsent by BoolPref(prefs, "ai_consent", false)
    var aiModel by StringPref(prefs, "ai_model", "llama33")
    var aiStyle by StringPref(prefs, "ai_style", "gentle")

    val aiConfigured: Boolean get() = aiUrl.startsWith("https://") && aiToken.length >= 16
}

private class StringPref(private val prefs: SharedPreferences, private val key: String, default: String = "") {
    private var state by mutableStateOf(prefs.getString(key, default) ?: default)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = state

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        state = value
        prefs.edit().putString(key, value).apply()
    }
}

private class BoolPref(private val prefs: SharedPreferences, private val key: String, default: Boolean) {
    private var state by mutableStateOf(prefs.getBoolean(key, default))

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = state

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        state = value
        prefs.edit().putBoolean(key, value).apply()
    }
}
