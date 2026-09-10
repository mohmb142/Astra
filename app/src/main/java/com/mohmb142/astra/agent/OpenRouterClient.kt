package com.mohmb142.astra.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterClient(private val apiKey: String, private val model: String = "google/gemini-2.5-flash") {
    suspend fun plan(goal: String, screen: ScreenSnapshot): AgentPlan = withContext(Dispatchers.IO) {
        val prompt = """أنت Astra، وكيل Android. حلّل الهدف والشاشة الحالية ثم أعد JSON فقط.
الهدف: $goal
الحزمة: ${screen.packageName}
النص الظاهر: ${screen.text.take(7000)}
العناصر القابلة للنقر: ${screen.clickableTexts.take(80)}
العناصر القابلة للكتابة: ${screen.editableTexts.take(30)}
الأفعال المسموحة: TAP_TEXT, TYPE, BACK, SCROLL_FORWARD, SCROLL_BACKWARD, OPEN_URL, WAIT, FINISH.
لا تنفذ شراء/تحويل/حذف/إرسال رسالة دون طلب تأكيد واضح من المستخدم.
الصيغة: {"summary":"...","steps":[{"action":"TAP_TEXT","value":"...","reason":"..."}]}"""
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.1)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user"); put("content", prompt)
            }))
        }
        val conn = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 20000; readTimeout = 30000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("HTTP-Referer", "https://github.com/mohmb142/Astra")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val response = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream).bufferedReader().readText()
        if (conn.responseCode !in 200..299) error("OpenRouter ${conn.responseCode}: ${response.take(300)}")
        val content = JSONObject(response).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            .replace("```json", "").replace("```", "").trim()
        parsePlan(content)
    }

    private fun parsePlan(raw: String): AgentPlan {
        val o = JSONObject(raw)
        val a = o.optJSONArray("steps") ?: JSONArray()
        val steps = buildList {
            for (i in 0 until a.length()) {
                val s = a.getJSONObject(i)
                val action = runCatching { AgentAction.valueOf(s.optString("action").uppercase()) }.getOrNull() ?: AgentAction.WAIT
                add(AgentStep(action, s.optString("value"), s.optString("reason")))
            }
        }.take(12)
        return AgentPlan(o.optString("summary", "خطة Astra"), steps)
    }
}
