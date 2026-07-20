package com.example

import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

class LiveApiTest {
    @Test
    fun testLiveConnection() {
        val apiKey = BuildConfig.VOICE_COACH_API_KEY.takeIf { it != "MY_VOICE_COACH_API_KEY" } ?: BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY") {
            println("No API key available to test.")
            return
        }
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(url).build()
        val latch = CountDownLatch(1)
        val responseMessages = mutableListOf<String>()
        
        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Change to gemini-2.0-flash-exp to test that too, but here we test the one in LiveModel
                val setupPayload = """{"setup": {"model": "models/gemini-2.5-flash"}}"""
                webSocket.send(setupPayload)
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                println("Server Output WS MSG: $text")
                responseMessages.add(text)
                if (text.contains("setupComplete") || text.contains("error")) {
                    latch.countDown()
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("Server Output WS FAILED: ${t.message}")
                responseMessages.add("FAILURE: ${t.message}")
                latch.countDown()
            }
        })
        val success = latch.await(10, TimeUnit.SECONDS)
        val allMessages = responseMessages.joinToString("\n")
        assertTrue("Expected setupComplete but got:\n$allMessages", allMessages.contains("setupComplete"))
    }
}
