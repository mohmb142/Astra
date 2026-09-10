package com.mohmb142.astra.agent

import android.os.SystemClock

class AgentExecutor {
    suspend fun run(goal: String, apiKey: String, model: String, log: (String) -> Unit): Boolean {
        val service = AstraAccessibilityService.instance ?: run { log("❌ فعّل خدمة إمكانية الوصول أولاً."); return false }
        if (apiKey.isBlank()) { log("❌ أضف مفتاح OpenRouter."); return false }
        val client = OpenRouterClient(apiKey, model)
        repeat(8) { cycle ->
            val screen = service.snapshot()
            log("👁️ ملاحظة الشاشة ${cycle + 1}: ${screen.text.take(160)}")
            val plan = runCatching { client.plan(goal, screen) }.getOrElse { log("❌ خطأ التخطيط: ${it.message}"); return false }
            log("🧠 ${plan.summary}")
            if (plan.steps.isEmpty()) { log("⚠️ لم تُنتج خطة."); return false }
            for (step in plan.steps) {
                if (step.action == AgentAction.FINISH) { log("✅ المهمة انتهت."); return true }
                if (step.action == AgentAction.WAIT) { SystemClock.sleep(1000); continue }
                val ok = when (step.action) {
                    AgentAction.TAP_TEXT -> service.clickText(step.value)
                    AgentAction.TYPE -> service.typeIntoFocused(step.value)
                    AgentAction.BACK -> service.back()
                    AgentAction.SCROLL_FORWARD -> service.scroll(true)
                    AgentAction.SCROLL_BACKWARD -> service.scroll(false)
                    AgentAction.OPEN_URL -> service.openUrl(step.value)
                    else -> true
                }
                log(if (ok) "▶️ ${step.action}: ${step.value.take(80)}" else "⚠️ فشل ${step.action}: ${step.value.take(80)}")
                if (!ok) break
                SystemClock.sleep(700)
            }
        }
        log("⏹️ انتهت دورات الوكيل دون تأكيد نهائي.")
        return false
    }
}
