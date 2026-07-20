package com.example

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Standard Google AI LiveModel Wrapper for Android.
 * Leverages Gemini Multimodal Live streaming protocols and exposes high-level
 * LiveSession abstractions, letting the system manage recording and audio-to-audio
 * conversational streaming behind a standardized startAudioConversation() facade.
 */
class LiveModel(private val apiKey: String, private val systemInstruction: String = "") {
    fun createSession(): LiveSession {
        return LiveSession(apiKey, systemInstruction)
    }
}

class LiveSession(private val apiKey: String, private val systemInstruction: String = "") {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isConversationActive = false
    private var recordingJob: Job? = null
    
    val isConnected = MutableStateFlow(false)
    val isSpeaking = MutableStateFlow(false)
    val amplitudes = MutableStateFlow<List<Float>>(List(50) { 0.1f })
    val connectionError = MutableStateFlow<String?>(null)
    private val _amplitudesList = ArrayList<Float>().apply { addAll(List(50) { 0.1f }) }
    private var ampIdx = 0

    /**
     * Standard invocation that automatically triggers mic acquisition, streams audio data
     * continuously to the Gemini multimodal endpoint, and renders live verbal guidance.
     */
    @SuppressLint("MissingPermission")
    fun startAudioConversation() {
        if (isConversationActive) return
        isConversationActive = true
        
        // 1. Establish direct multimodal WebSocket endpoint connection
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    isConnected.value = true
                    sendSetupConfigMessage(webSocket)
                } catch (e: Exception) {
                    Log.e("LiveModel", "Error in onOpen", e)
                }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    parseAndPlayIncomingAudio(text)
                } catch (e: Exception) {
                    Log.e("LiveModel", "Error in onMessage", e)
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                try {
                    Log.e("LiveModel", "WebSocket Failure: ${t.message}", t)
                    isConnected.value = false
                    connectionError.value = t.message ?: "Connection failed"
                } catch (e: Exception) {
                    Log.e("LiveModel", "Error in onFailure", e)
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                try {
                    Log.e("LiveModel", "WebSocket Closed: $code - $reason")
                    isConnected.value = false
                    isSpeaking.value = false
                } catch (e: Exception) {
                    Log.e("LiveModel", "Error in onClosed", e)
                }
            }
        })

        // 2. Initialize high-performance low-latency AudioTrack for direct coaching stream output
        try {
            val trackSampleRate = 24000
            val trackBufSize = AudioTrack.getMinBufferSize(
                trackSampleRate, 
                AudioFormat.CHANNEL_OUT_MONO, 
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(trackSampleRate)
                    .build())
                .setBufferSizeInBytes(trackBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("LiveModel", "AudioTrack init failed", e)
        }

        // 3. Initiate internal high-fidelity microphone input acquisition
        var recBufSize = 3200
        try {
            val recSampleRate = 16000
            recBufSize = AudioRecord.getMinBufferSize(
                recSampleRate, 
                AudioFormat.CHANNEL_IN_MONO, 
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(3200)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                recSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufSize
            )
            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            Log.e("LiveModel", "AudioRecord SecurityException (Microphone Permission): ${e.message}")
            stopAudioConversation()
            return
        } catch (e: Exception) {
            Log.e("LiveModel", "AudioRecord init failed", e)
            stopAudioConversation()
            return
        }

        recordingJob = scope.launch {
            val shortBuffer = ShortArray(recBufSize / 2)
            val byteBuffer = ByteArray(recBufSize)
            while (isConversationActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: 0
                if (read > 0) {
                    var sum = 0L
                    for (i in 0 until read) {
                        val sample = shortBuffer[i]
                        val byteIndex = i * 2
                        if (byteIndex + 1 < byteBuffer.size) {
                            byteBuffer[byteIndex] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }
                        sum += abs(sample.toInt())
                    }
                    val avg = sum.toFloat() / read
                    val norm = (avg / 32768f).coerceIn(0.1f, 1.0f)
                    _amplitudesList[ampIdx] = norm
                    ampIdx = (ampIdx + 1) % 50
                    amplitudes.value = _amplitudesList.toList()

                    // Automatically packages audio chunks to base64 and streams
                    val actualBytes = ByteArray(read * 2)
                    System.arraycopy(byteBuffer, 0, actualBytes, 0, actualBytes.size)
                    streamAudioPacketToWebSocket(actualBytes)
                } else {
                    // If no data read (e.g., emulator, no hardware / permission), yield to prevent tight infinite CPU loop
                    delay(100)
                }
            }
        }
    }

    private fun sendSetupConfigMessage(ws: WebSocket) {
        val setupPayload = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", "models/gemini-2.0-flash-exp")
                if (systemInstruction.isNotEmpty()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Puck")
                            })
                        })
                    })
                })
            })
        }
        ws.send(setupPayload.toString())
    }

    private fun streamAudioPacketToWebSocket(bytes: ByteArray) {
        val ws = webSocket ?: return
        if (!isConnected.value) return
        try {
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val chunkPayload = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        })
                    })
                })
            }
            ws.send(chunkPayload.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseAndPlayIncomingAudio(jsonText: String) {
        try {
            val json = JSONObject(jsonText)
            if (json.has("serverContent")) {
                isSpeaking.value = true
            }
            val serverContent = json.optJSONObject("serverContent")
            val modelTurn = serverContent?.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData") ?: part?.optJSONObject("inline_data")
                    val rawBase64 = inlineData?.optString("data") ?: ""
                    if (rawBase64.isNotEmpty()) {
                        val audioBytes = Base64.decode(rawBase64, Base64.DEFAULT)
                        audioTrack?.write(audioBytes, 0, audioBytes.size)
                    }
                }
            }
            val turnComplete = serverContent?.optBoolean("turnComplete") ?: false
            if (turnComplete) {
                isSpeaking.value = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAudioConversation() {
        isConversationActive = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
        try {
            webSocket?.close(1000, "Stop conversation")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket = null
        isConnected.value = false
        isSpeaking.value = false
        scope.cancel()
    }
}
