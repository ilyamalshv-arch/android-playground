package com.ilyamalshv.vnutri.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface AiReply {
    data class Text(val text: String) : AiReply
    data object Crisis : AiReply
    data class Failure(val message: String) : AiReply
    data class States(val ids: List<String>) : AiReply
}

/**
 * Talks to the person's own Cloudflare Worker (see worker/worker.js).
 * Sends only: feeling names with intensity, the note, and the chosen school texts.
 */
class AiClient(private val settings: Settings) {

    suspend fun health(): AiReply = request("GET", "/health", null) { AiReply.Text("ok") }

    /** Asks the model which of the app's states fit the whole text best. */
    suspend fun classify(text: String): AiReply {
        val body = JSONObject()
            .put("text", text)
            .put("model", settings.aiModel)
            .put("states", JSONArray().apply {
                Emotions.all.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
            })
        return request("POST", "/classify", body.toString()) { json ->
            when {
                json.optBoolean("crisis") -> AiReply.Crisis
                else -> {
                    val arr = json.optJSONArray("ids") ?: JSONArray()
                    AiReply.States(List(arr.length()) { arr.getString(it) })
                }
            }
        }
    }

    suspend fun reflect(
        emotions: List<EmotionMark>,
        note: String,
        lenses: List<Triple<School, String, Lens>>,
        history: List<AiTurn> = emptyList(),
    ): AiReply {
        val body = JSONObject()
            .put("note", note)
            .put("style", settings.aiStyle)
            .put("model", settings.aiModel)
            .put("history", JSONArray().apply {
                history.forEach { put(JSONObject().put("role", it.role).put("text", it.text)) }
            })
            .put("emotions", JSONArray().apply {
                emotions.forEach { put(JSONObject().put("name", Emotions.name(it.emotionId)).put("intensity", intensityLabel(it.intensity))) }
            })
            .put("lenses", JSONArray().apply {
                lenses.forEach { (school, emotionId, lens) ->
                    put(
                        JSONObject()
                            .put("school", school.title)
                            .put("tradition", school.tradition)
                            .put("emotion", Emotions.name(emotionId))
                            .put("text", lens.text),
                    )
                }
            })
        return request("POST", "/reflect", body.toString()) { json ->
            when {
                json.optBoolean("crisis") -> AiReply.Crisis
                json.optString("text").isNotBlank() -> AiReply.Text(json.getString("text"))
                else -> AiReply.Failure("Пустой ответ")
            }
        }
    }

    private suspend fun request(method: String, path: String, body: String?, parse: (JSONObject) -> AiReply): AiReply =
        withContext(Dispatchers.IO) {
            val conn = runCatching { URL(settings.aiUrl.trimEnd('/') + path).openConnection() as HttpURLConnection }
                .getOrElse { return@withContext AiReply.Failure("Неверный адрес сервера") }
            try {
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                conn.setRequestProperty("x-app-token", settings.aiToken)
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("content-type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                when (code) {
                    in 200..299 -> parse(JSONObject(text))
                    401 -> AiReply.Failure("Сервер не принял ключ доступа. Проверьте его в настройках.")
                    429 -> AiReply.Failure("Бесплатный лимит на сегодня исчерпан. Он обновится в 00:00 UTC (3:00 по Москве).")
                    502 -> AiReply.Failure("Модель не ответила. Попробуйте ещё раз или выберите другую модель в настройках.")
                    else -> AiReply.Failure("Сервер ответил ошибкой ($code). Попробуйте позже.")
                }
            } catch (e: Exception) {
                AiReply.Failure("Нет связи с сервером. Проверьте интернет и адрес в настройках.")
            } finally {
                conn.disconnect()
            }
        }
}
