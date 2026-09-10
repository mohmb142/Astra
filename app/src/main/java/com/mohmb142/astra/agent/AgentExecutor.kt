package com.mohmb142.astra.agent

import kotlinx.coroutines.delay

class AgentExecutor {
    suspend fun run(goal: String, apiKey: String, model: String, log: (String) -> Unit): Boolean {
        if (goal.isBlank()) { log("❌ اكتب هدفًا أولاً."); return false }
        val service = AstraAccessibilityService.instance ?: run { log("❌ فعّل خدمة إمكانية الوصول أولاً."); return false }
        if (apiKey.isBlank()) { log("❌ أضف مفتاح OpenRouter."); return false }
        val client = OpenRouterClient(apiKey, model)
        repeat(8) { cycle ->
            val before = service.snapshot()
            log("👁️ ملاحظة الشاشة ${cycle + 1}: ${before.text.take(160)}")
            val plan = runCatching { client.plan(goal, before) }.getOrElse { log("❌ خطأ التخطيط: ${it.message}"); return false }
            log("🧠 ${plan.summary}")
            if (plan.steps.isEmpty()) { log("⚠️ لم تُنتج خطة؛ إعادة الملاحظة."); return@repeat }

            for (step in plan.steps) {
                if (step.action == AgentAction.FINISH) { log("✅ المهمة انتهت."); return true }
                val safety = SafetyPolicy.check(step)
                if (safety.decision == SafetyPolicy.Decision.BLOCKED) { log("🛑 محظور: ${safety.reason}"); return false }
                if (safety.decision == SafetyPolicy.Decision.REQUIRES_CONFIRMATION) {
                    log("⏸️ مطلوب تأكيد المستخدم قبل: ${step.action} ${step.value}")
                    log("ℹ️ السبب: ${safety.reason}")
                    return false
                }
                if (step.action == AgentAction.WAIT) { delay(1000); continue }
                val ok = when (step.action) {
                    AgentAction.TAP_TEXT -> service.clickText(step.value)
                    AgentAction.TYPE -> service.typeIntoFocused(step.value)
                    AgentAction.BACK -> service.back()
                    AgentAction.HOME -> service.home()
                    AgentAction.SCROLL_FORWARD -> service.scroll(true)
                    AgentAction.SCROLL_BACKWARD -> service.scroll(false)
                    AgentAction.OPEN_URL -> service.openUrl(step.value)
                    AgentAction.OPEN_APP -> service.openApp(step.value)
                    AgentAction.WAIT, AgentAction.FINISH -> true
                }
                log(if (ok) "▶️ ${step.action}: ${step.value.take(80)}" else "⚠️ فشل ${step.action}: ${step.value.take(80)}")
                if (!ok) { log("🔄 سأعيد الملاحظة والتخطيط بدل الاستمرار بخطة قديمة."); break }
                delay(700)
                val after = service.snapshot()
                if (after.packageName.isNotBlank() && before.packageName != after.packageName) log("🔎 تغيّر التطبيق: ${after.packageName}")
            }
        }
        log("⏹️ انتهت دورات الوكيل دون تأكيد نهائي.")
        return false
    }
}
