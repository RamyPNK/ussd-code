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

        // قائمة تطبيقات الهاتف/الاتصال المعروفة
        val PHONE_PACKAGES = setOf(
            "com.android.phone",
            "com.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.huawei.phone",
            "com.google.android.dialer",
            "com.motorola.incallui",
            "com.xiaomi.incallui",
            "com.oppo.phone",
            "com.vivo.incallui"
        )
    }

    override fun onServiceConnected() {
        instance = this
        Log.d("USSD", "Accessibility connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isWaitingForUSSD || event == null) return

        // ★ الشرط الأهم: نقبل فقط من تطبيقات الهاتف
        val pkg = event.packageName?.toString() ?: return
        val isPhoneApp = PHONE_PACKAGES.any { pkg.contains(it) } ||
                pkg.contains("phone") ||
                pkg.contains("dialer") ||
                pkg.contains("incall")

        if (!isPhoneApp) {
            Log.d("USSD", "Ignored event from: $pkg")
            return
        }

        // ★ نقبل فقط نوافذ Dialog أو Alert (النوافذ المنبثقة)
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return

        // ★ تحقق أن النافذة الحالية من تطبيق الهاتف
        val rootPkg = root.packageName?.toString() ?: return
        val isRootPhone = PHONE_PACKAGES.any { rootPkg.contains(it) } ||
                rootPkg.contains("phone") ||
                rootPkg.contains("dialer")

        if (!isRootPhone) {
            Log.d("USSD", "Root window not phone app: $rootPkg")
            return
        }

        val text = extractText(root)
        if (text.isEmpty()) return

        // ★ تجاهل رسائل التحميل المؤقتة
        val lower = text.lowercase()
        val isLoading = lower.contains("running") ||
                lower.contains("en cours") ||
                lower.contains("patientez") ||
                lower.contains("please wait") ||
                lower.contains("chargement") ||
                lower.contains("تشغيل") ||
                lower.contains("انتظر") ||
                (text.length < 5)

        if (isLoading) {
            Log.d("USSD", "Ignored loading message: $text")
            return
        }

        // ★ تجاهل النص إذا لا يحتوي على أرقام (العروض دائماً تحتوي أرقام)
        val hasNumbers = text.any { it.isDigit() }
        if (!hasNumbers) {
            Log.d("USSD", "Ignored text without numbers: $text")
            return
        }

        Log.d("USSD", "✅ Got USSD response: $text")
        lastUSSDResponse = text
        isWaitingForUSSD = false
        closeDialog(root)
    }

    private fun extractText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        fun scan(n: AccessibilityNodeInfo?) {
            if (n == null) return
            val t = n.text?.toString()
            if (!t.isNullOrEmpty() &&
                t != "OK" && t != "Cancel" &&
                t != "Annuler" && t != "Send" &&
                t != "إرسال" && t != "إلغاء") {
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