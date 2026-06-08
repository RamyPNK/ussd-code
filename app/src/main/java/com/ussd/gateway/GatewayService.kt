package com.ussd.gateway

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GatewayService : Service() {

    private val CHANNEL_ID = "ussd_ch"
    private val NOTIF_ID = 1
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif("جاري التشغيل..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        executor.submit { loop() }
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        running = false
        executor.shutdown()
        super.onDestroy()
    }

    private fun loop() {
        while (running) {
            try {
                checkOffers()
                Thread.sleep(Config.POLL_INTERVAL)
                checkPurchase()
                Thread.sleep(Config.POLL_INTERVAL)
            } catch (e: Exception) {
                Log.e("GW", e.message ?: "error")
                Thread.sleep(Config.POLL_INTERVAL)
            }
        }
    }

    private fun checkOffers() {
        val res = get(Config.OFFERS_POLL_URL) ?: return
        if (res == "EMPTY" || res == "DENIED") return

        val parts = res.split("||")
        if (parts.size < 3) return

        val code = parts[0].trim()
        val reqId = parts[1].trim()
        val sourceCode = parts[2].trim()

        updateNotif("جلب العروض...")

        // تنفيذ كود واحد فقط
        call(code)
        val r1 = waitUSSD(15000)

        get("${Config.OFFERS_REPORT_URL}?secret=${Config.SECRET_KEY}&request_id=$reqId&source_code=$sourceCode&success=1&raw_response=${Uri.encode(r1)}")

        updateNotif("جاري التشغيل...")
    }

    private fun checkPurchase() {
        val res = get(Config.PURCHASE_POLL_URL) ?: return
        if (res == "EMPTY" || res == "DENIED") return
        updateNotif("تنفيذ الشراء...")
        call(res.trim())
        val result = waitUSSD(9000)
        val success = if (result.isNotEmpty()) "1" else "0"
        get("${Config.PURCHASE_REPORT_URL}?secret=${Config.SECRET_KEY}&success=$success&result_msg=${Uri.encode(result)}")
        updateNotif("جاري التشغيل...")
    }

    private fun call(code: String) {
        Log.d("GW", "Calling: $code")
        USSDAccessibilityService.isWaitingForUSSD = true
        USSDAccessibilityService.lastUSSDResponse = ""
        handler.post {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${Uri.encode(code)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("com.android.phone.extra.slot", Config.SIM_SLOT)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("GW", "Call error: ${e.message}")
                USSDAccessibilityService.isWaitingForUSSD = false
            }
        }
    }

    private fun waitUSSD(timeout: Long): String {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            if (!USSDAccessibilityService.isWaitingForUSSD)
                return USSDAccessibilityService.lastUSSDResponse
            Thread.sleep(500)
        }
        USSDAccessibilityService.isWaitingForUSSD = false
        return ""
    }

    private fun get(url: String): String? {
        return try {
            val r = client.newCall(Request.Builder().url(url).build()).execute()
            r.body?.string()?.trim()
        } catch (e: Exception) { null }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "USSD Gateway", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotif(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("USSD Gateway")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotif(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotif(text))
    }
}