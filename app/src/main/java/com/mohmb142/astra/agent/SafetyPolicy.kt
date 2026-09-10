package com.mohmb142.astra.agent

/** طبقة أمان قبل تنفيذ أفعال الوكيل. */
object SafetyPolicy {
    enum class Decision { SAFE, REQUIRES_CONFIRMATION, BLOCKED }
    data class Result(val decision: Decision, val reason: String)

    private val blocked = listOf("format device", "factory reset", "إعادة ضبط المصنع", "مسح الهاتف بالكامل")
    private val sensitive = listOf(
        "send", "إرسال", "delete", "حذف", "remove", "شراء", "buy", "payment", "دفع",
        "transfer", "تحويل", "withdraw", "سحب", "publish", "نشر", "post", "نشر منشور"
    )

    fun check(step: AgentStep): Result {
        val value = step.value.trim().lowercase()
        if (step.action == AgentAction.FINISH || step.action == AgentAction.WAIT) return Result(Decision.SAFE, "لا يوجد إجراء حساس")
        if (blocked.any { value.contains(it) }) return Result(Decision.BLOCKED, "الإجراء قد يسبب ضررًا واسعًا أو فقدان بيانات")
        if (sensitive.any { value.contains(it) }) return Result(Decision.REQUIRES_CONFIRMATION, "الإجراء قد يرسل أو يحذف أو يشتري أو يغيّر بيانات خارجية")
        return Result(Decision.SAFE, "إجراء منخفض المخاطر")
    }
}
