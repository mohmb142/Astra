package com.mohmb142.astra.agent

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT
import android.content.Intent
import android.net.Uri

class AstraAccessibilityService : AccessibilityService() {
    companion object { var instance: AstraAccessibilityService? = null }
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }
    override fun onInterrupt() { }
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun snapshot(): ScreenSnapshot {
        val root = rootInActiveWindow ?: return ScreenSnapshot("", "", emptyList(), emptyList())
        val clickable = mutableListOf<String>(); val editable = mutableListOf<String>(); val all = mutableListOf<String>()
        walk(root, all, clickable, editable)
        return ScreenSnapshot(root.packageName?.toString().orEmpty(), all.distinct().joinToString(" | ").take(9000), clickable.distinct().take(100), editable.distinct().take(50))
    }

    private fun walk(node: AccessibilityNodeInfo, all: MutableList<String>, clickable: MutableList<String>, editable: MutableList<String>) {
        val value = (node.text ?: node.contentDescription)?.toString()?.trim().orEmpty()
        if (value.isNotEmpty()) { all += value; if (node.isClickable) clickable += value; if (node.isEditable) editable += value }
        for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, all, clickable, editable); it.recycle() }
    }

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()?.let { node ->
            val ok = if (node.isClickable) node.performAction(AccessibilityNodeInfo.ACTION_CLICK) else node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            node.recycle(); ok
        } ?: false
    }

    fun typeIntoFocused(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findEditable(root) ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        val ok = node.performAction(ACTION_SET_TEXT, args); node.recycle(); return ok
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) node.getChild(i)?.let { child ->
            val found = findEditable(child); child.recycle(); if (found != null) return found
        }
        return null
    }

    fun back(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun home(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findScrollable(root) ?: return false
        val ok = node.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        node.recycle(); return ok
    }
    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) node.getChild(i)?.let { child ->
            val found = findScrollable(child); child.recycle(); if (found != null) return found
        }
        return null
    }
    fun openUrl(url: String): Boolean = runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 80)).build()
        return dispatchGesture(gesture, null, null)
    }
}
