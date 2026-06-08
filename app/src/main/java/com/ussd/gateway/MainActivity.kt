package com.ussd.gateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        val etServer  = findViewById<EditText>(R.id.etServer)
        val etSecret  = findViewById<EditText>(R.id.etSecret)
        val spSim     = findViewById<Spinner>(R.id.spSim)
        val btnSave   = findViewById<Button>(R.id.btnSave)
        val btnStart  = findViewById<Button>(R.id.btnStart)
        val btnStop   = findViewById<Button>(R.id.btnStop)
        val btnAccess = findViewById<Button>(R.id.btnAccessibility)
        val tvStatus  = findViewById<TextView>(R.id.tvStatus)

        val prefs = getSharedPreferences("config", MODE_PRIVATE)
        etServer.setText(prefs.getString("server", "http://192.168.1.9"))
        etSecret.setText(prefs.getString("secret", "MY_SECRET_2024"))
        spSim.setSelection(prefs.getInt("sim", 0))

        btnSave.setOnClickListener {
            val server = etServer.text.toString().trimEnd('/')
            val secret = etSecret.text.toString()
            val sim    = spSim.selectedItemPosition

            Config.SERVER_URL = server
            Config.SECRET_KEY = secret
            Config.SIM_SLOT   = sim

            prefs.edit()
                .putString("server", server)
                .putString("secret", secret)
                .putInt("sim", sim)
                .apply()

            tvStatus.text = "✅ تم الحفظ"
            Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener {
            startForegroundService(Intent(this, GatewayService::class.java))
            tvStatus.text = "🟢 الخدمة تعمل"
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, GatewayService::class.java))
            tvStatus.text = "🔴 الخدمة متوقفة"
        }

        btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }
}