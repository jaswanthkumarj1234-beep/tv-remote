package com.example.tvremote

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * A tiny HTTP server on the home Wi-Fi.
 *  GET  /              -> the phone controller page (assets/index.html)
 *  GET  /api/status    -> basic info, no secret needed
 *  POST /api/cmd       -> JSON command, requires header X-Pair-Code
 */
class LocalServer(
    private val context: Context,
    private val port: Int,
    private val pairCode: String
) {
    private var server: ServerSocket? = null

    @Volatile
    private var running = false

    fun start() {
        val ss = ServerSocket(port)
        server = ss
        running = true
        thread(isDaemon = true, name = "tv-remote-accept") {
            while (running) {
                try {
                    val socket = ss.accept()
                    thread(isDaemon = true) { handle(socket) }
                } catch (e: IOException) {
                    if (running) e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        running = false
        server?.close()
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val out = s.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrElse(0) { "" }
            val path = parts.getOrElse(1) { "/" }.substringBefore("?")

            val headers = HashMap<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val idx = line.indexOf(':')
                if (idx > 0) {
                    headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                }
            }

            val length = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (length > 0) {
                val buf = CharArray(length)
                var read = 0
                while (read < length) {
                    val n = reader.read(buf, read, length - read)
                    if (n < 0) break
                    read += n
                }
                String(buf, 0, read)
            } else ""

            when {
                method == "GET" && path == "/" -> {
                    val html = context.assets.open("index.html").use { it.readBytes() }
                    sendBytes(out, 200, "text/html; charset=utf-8", html)
                }

                method == "GET" && path == "/api/status" -> {
                    sendJson(out, 200, JSONObject().put("ok", true).put("name", android.os.Build.MODEL))
                }

                method == "POST" && path == "/api/cmd" -> {
                    if (headers["x-pair-code"] != pairCode) {
                        sendJson(out, 401, JSONObject().put("ok", false).put("error", "bad pairing code"))
                    } else {
                        val ok = try {
                            RemoteController.handle(JSONObject(body), context)
                        } catch (e: Exception) {
                            false
                        }
                        sendJson(out, if (ok) 200 else 400, JSONObject().put("ok", ok))
                    }
                }

                else -> sendJson(out, 404, JSONObject().put("ok", false).put("error", "not found"))
            }
        }
    }

    private fun sendJson(out: OutputStream, code: Int, json: JSONObject) {
        sendBytes(out, code, "application/json", json.toString().toByteArray(Charsets.UTF_8))
    }

    private fun sendBytes(out: OutputStream, code: Int, contentType: String, body: ByteArray) {
        val status = when (code) {
            200 -> "200 OK"
            400 -> "400 Bad Request"
            401 -> "401 Unauthorized"
            else -> "404 Not Found"
        }
        // No CORS header on purpose: other websites cannot call this server from a browser.
        val header = "HTTP/1.1 $status\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(body)
        out.flush()
    }
}
