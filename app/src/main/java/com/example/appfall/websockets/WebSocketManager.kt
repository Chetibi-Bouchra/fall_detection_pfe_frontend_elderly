package com.example.appfall.websockets

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okhttp3.*
import okio.ByteString
import com.example.appfall.utils.url

class WebSocketManager private constructor() {
    companion object {
        private var webSocket: WebSocket? = null
        private val _receivedMessage = MutableLiveData<String>()
        val receivedMessage: LiveData<String>
            get() = _receivedMessage

        fun connectWebSocket() {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    println("WebSocket connection opened")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    println("WebSocket message received: $text")
                    // Update LiveData with received message
                    _receivedMessage.postValue(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                    println("WebSocket binary message received: $bytes")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosing(webSocket, code, reason)
                    println("WebSocket connection closing: $reason")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                    println("WebSocket connection closed: $reason")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    println("WebSocket connection failed: ${t.message}")
                }
            })
        }

        fun sendMessage(message: String) {
            webSocket?.send(message)
        }
    }
}