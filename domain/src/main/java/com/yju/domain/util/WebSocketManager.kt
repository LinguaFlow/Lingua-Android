package com.yju.domain.util

import android.util.Log
import com.yju.domain.pdf.model.UploadStatusMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "WebSocketManager"
        private const val WS_ENDPOINT = "ws"
        private const val TOPIC_PREFIX = "/topic/upload/"
    }

    private var stompSession: StompSession? = null
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }

    suspend fun connect(): Result<Unit> {
        return try {
            if (stompSession != null) {
                Log.d(TAG, "Already connected")
                return Result.success(Unit)
            }

            _connectionState.value = ConnectionState.CONNECTING

            val wsUrl = baseUrl.replace("http", "ws") + WS_ENDPOINT
            Log.d(TAG, "Connecting to WebSocket: $wsUrl")

            val webSocketClient = OkHttpWebSocketClient(okHttpClient)
            val stompClient = StompClient(webSocketClient)

            stompSession = stompClient.connect(wsUrl)
            _connectionState.value = ConnectionState.CONNECTED

            Log.d(TAG, "WebSocket connected successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "WebSocket connection failed", e)
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    suspend fun subscribeToUploadStatus(taskId: Long): Flow<UploadStatusMessage> {
        val session = stompSession ?: throw IllegalStateException("Not connected to WebSocket")
        val destination = "$TOPIC_PREFIX$taskId"

        Log.d(TAG, "Subscribing to: $destination")

        // subscribeText는 Flow<String>을 반환
        return session.subscribeText(destination)
            .map { textContent ->
                // textContent는 이미 String 타입
                json.decodeFromString(UploadStatusMessage.serializer(), textContent)
            }
    }

    suspend fun disconnect() {
        try {
            stompSession?.disconnect()
            stompSession = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.d(TAG, "WebSocket disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting WebSocket", e)
        }
    }
}