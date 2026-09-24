package com.ilyamalshv.vnutri.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class EmotionMark(val emotionId: String, val intensity: Int)

data class SavedLens(val schoolId: String, val emotionId: String)

/** One message of the AI conversation; role is "user" or "assistant". */
data class AiTurn(val role: String, val text: String)

data class JournalEntry(
    val id: Long,
    val emotions: List<EmotionMark>,
    val note: String,
    val reflection: String = "",
    val saved: List<SavedLens> = emptyList(),
    val ai: String = "",
    val aiTurns: List<AiTurn> = emptyList(),
) {
    /** The whole AI conversation; older entries stored only the first answer in [ai]. */
    val conversation: List<AiTurn>
        get() = aiTurns.ifEmpty { if (ai.isBlank()) emptyList() else listOf(AiTurn("assistant", ai)) }
}

fun intensityLabel(intensity: Int): String = when (intensity) {
    1 -> tr("слегка", "slightly")
    3 -> tr("сильно", "strongly")
    else -> tr("заметно", "noticeably")
}

/** The journal lives in one private JSON file; nothing leaves the phone. */
class JournalStore(context: Context) {
    private val file = File(context.filesDir, "journal.json")

    fun load(): List<JournalEntry> = runCatching {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        List(arr.length()) { fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun save(entries: List<JournalEntry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(toJson(it)) }
        val tmp = File(file.parentFile, "journal.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }

    private fun toJson(e: JournalEntry) = JSONObject().apply {
        put("id", e.id)
        put("note", e.note)
        put("reflection", e.reflection)
        put("ai", e.ai)
        put("aiTurns", JSONArray().apply {
            e.aiTurns.forEach { put(JSONObject().put("role", it.role).put("text", it.text)) }
        })
        put("emotions", JSONArray().apply {
            e.emotions.forEach { put(JSONObject().put("id", it.emotionId).put("intensity", it.intensity)) }
        })
        put("saved", JSONArray().apply {
            e.saved.forEach { put(JSONObject().put("school", it.schoolId).put("emotion", it.emotionId)) }
        })
    }

    private fun fromJson(o: JSONObject): JournalEntry {
        val emotions = o.getJSONArray("emotions")
        val saved = o.optJSONArray("saved") ?: JSONArray()
        val turns = o.optJSONArray("aiTurns") ?: JSONArray()
        return JournalEntry(
            id = o.getLong("id"),
            note = o.optString("note"),
            reflection = o.optString("reflection"),
            ai = o.optString("ai"),
            aiTurns = List(turns.length()) {
                val t = turns.getJSONObject(it)
                AiTurn(t.getString("role"), t.getString("text"))
            },
            emotions = List(emotions.length()) {
                val m = emotions.getJSONObject(it)
                EmotionMark(m.getString("id"), m.optInt("intensity", 2))
            },
            saved = List(saved.length()) {
                val s = saved.getJSONObject(it)
                SavedLens(s.getString("school"), s.getString("emotion"))
            },
        )
    }
}
