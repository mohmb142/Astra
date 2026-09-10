package com.mohmb142.astra.agent

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TaskHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("astra_history", Context.MODE_PRIVATE)

    fun add(goal: String, success: Boolean) {
        val old = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrDefault(JSONArray())
        val item = JSONObject().apply {
            put("goal", goal.take(500))
            put("success", success)
            put("time", System.currentTimeMillis())
        }
        val next = JSONArray().apply {
            put(item)
            for (i in 0 until minOf(old.length(), 49)) put(old.optJSONObject(i))
        }
        prefs.edit().putString("items", next.toString()).apply()
    }

    fun recent(limit: Int = 10): List<String> {
        val array = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until minOf(limit, array.length())) {
                val o = array.optJSONObject(i) ?: continue
                val status = if (o.optBoolean("success")) "نجاح" else "فشل"
                add("$status — ${o.optString("goal")}")
            }
        }
    }

    fun clear() { prefs.edit().remove("items").apply() }
}
