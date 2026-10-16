package com.example.tvremote

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Does the actual work on the TV. The user must turn this on in
 * Settings > Accessibility. It can:
 *  - type text into the focused text field (ACTION_SET_TEXT)
 *  - press Back / Home / Recents / Notifications
 *  - tap and swipe on screen (gestures)
 *  - click the focused item (OK)
 * It cannot send raw remote key codes to other apps; Android does not allow that
 * without system permissions. Focus movement is approximated with swipes.
 */
class RemoteAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: RemoteAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    // ---- Text -----------------------------------------------------------

    fun typeText(value: String, append: Boolean): Boolean {
        val node = focusedEditable() ?: return false
        val newText = if (append) (node.text?.toString() ?: "") + value else value
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun focusedEditable(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        return if (focused.isEditable) focused else null
    }

    // ---- System buttons -------------------------------------------------

    fun globalAction(name: String): Boolean {
        val action = when (name) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            else -> return false
        }
        return performGlobalAction(action)
    }

    // ---- Navigation -----------------------------------------------------

    /** OK: click whatever has focus. Other directions: a short swipe (approximation). */
    fun dpad(dir: String): Boolean {
        if (dir == "ok") {
            val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: return false
            return focused.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return swipe(dir, 250)
    }

    fun tapCenter(): Boolean {
        val (cx, cy) = center()
        val path = Path().apply {
            moveTo(cx, cy)
            lineTo(cx + 1f, cy + 1f)
        }
        return dispatch(path, 50)
    }

    /** dir is the direction the finger moves. "up" scrolls content down. */
    fun swipe(dir: String, distance: Int): Boolean {
        val (cx, cy) = center()
        val d = distance.coerceIn(50, 800).toFloat()
        val (dx, dy) = when (dir) {
            "up" -> 0f to -d
            "down" -> 0f to d
            "left" -> -d to 0f
            "right" -> d to 0f
            else -> return false
        }
        val endX = (cx + dx).coerceIn(0f, screenWidth().toFloat())
        val endY = (cy + dy).coerceIn(0f, screenHeight().toFloat())
        val path = Path().apply {
            moveTo(cx, cy)
            lineTo(endX, endY)
        }
        return dispatch(path, 250)
    }

    // ---- Helpers --------------------------------------------------------

    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels

    private fun center(): Pair<Float, Float> =
        (screenWidth() / 2f) to (screenHeight() / 2f)

    private fun dispatch(path: Path, durationMs: Long): Boolean {
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(gesture, null, null)
    }
}
