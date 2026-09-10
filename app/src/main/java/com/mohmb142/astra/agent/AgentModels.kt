package com.mohmb142.astra.agent

enum class AgentAction {
    TAP_TEXT, TYPE, BACK, HOME, SCROLL_FORWARD, SCROLL_BACKWARD, OPEN_URL, OPEN_APP, WAIT, FINISH
}

data class AgentStep(val action: AgentAction, val value: String = "", val reason: String = "")

data class AgentPlan(val summary: String, val steps: List<AgentStep>)

data class ScreenSnapshot(
    val packageName: String,
    val text: String,
    val clickableTexts: List<String>,
    val editableTexts: List<String>
)
