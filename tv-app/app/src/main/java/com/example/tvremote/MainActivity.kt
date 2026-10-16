package com.example.tvremote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : Activity() {

    // A fresh 6-digit pairing code is generated every time the app starts.
    private val pairCode = (100000..999999).random().toString()
    private var server: LocalServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ip = localIp() ?: "unknown"
        findViewById<TextView>(R.id.url).text = "http://$ip:8080"
        findViewById<TextView>(R.id.code).text = pairCode

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        server = LocalServer(this, 8080, pairCode).also { it.start() }
    }

    override fun onDestroy() {
        server?.stop()
        super.onDestroy()
    }

    private fun localIp(): String? = runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .flatMap { Collections.list(it.inetAddresses) }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    }.getOrNull()
}
