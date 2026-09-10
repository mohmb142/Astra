package com.mohmb142.astra.agent

import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyPolicyTest {
    @Test fun normalTapIsSafe() {
        assertEquals(SafetyPolicy.Decision.SAFE, SafetyPolicy.check(AgentStep(AgentAction.TAP_TEXT, "فتح الإعدادات")).decision)
    }

    @Test fun sendingRequiresConfirmation() {
        assertEquals(SafetyPolicy.Decision.REQUIRES_CONFIRMATION, SafetyPolicy.check(AgentStep(AgentAction.TAP_TEXT, "إرسال الرسالة")).decision)
    }

    @Test fun factoryResetIsBlocked() {
        assertEquals(SafetyPolicy.Decision.BLOCKED, SafetyPolicy.check(AgentStep(AgentAction.TAP_TEXT, "إعادة ضبط المصنع")).decision)
    }
}
