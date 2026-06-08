package com.ussd.gateway

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class USSDAccessibilityService : AccessibilityService() {

    companion object {
        var lastUSSDResponse = ""
        var isWaitingForUSSD = false
        var instance: USSDAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
        Log.d("USSD", "Accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isWaitingForUSSD || event == null) return
        val root = rootInActiveWindow ?: return
        val text = extractText(root)
        if (text.isNotEmpty()) {
            lastUSSDResponse = text
            isWaitingForUSSD = false
            closeDialog(root)
        }
    }

    private fun extractText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        fun scan(n: AccessibilityNodeInfo?) {
            if (n == null) return
            val t = n.text?.toString()
            if (!t.isNullOrEmpty() && t != "OK" && t != "Cancel" && t != "Annuler") {
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(t)
            }
            for (i in 0 until n.childCount) scan(n.getChild(i))
        }
        scan(node)
        return sb.toString().trim()
    }

    private fun closeDialog(node: AccessibilityNodeInfo) {
        fun click(n: AccessibilityNodeInfo?): Boolean {
            if (n == null) return false
            val t = n.text?.toString()?.lowercase()
            if (t == "ok" || t == "dismiss" || t == "موافق") {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            for (i in 0 until n.childCount) if (click(n.getChild(i))) return true
            return false
        }
        click(node)
    }

    override fun onInterrupt() { instance = null }
    override fun onDestroy() { instance = null; super.onDestroy() }
}