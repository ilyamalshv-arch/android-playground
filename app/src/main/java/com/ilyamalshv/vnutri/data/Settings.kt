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
}

private class BoolPref(private val prefs: SharedPreferences, private val key: String, default: Boolean) {
    private var state by mutableStateOf(prefs.getBoolean(key, default))

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = state

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        state = value
        prefs.edit().putBoolean(key, value).apply()
    }
}
