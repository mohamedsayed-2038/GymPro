package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Continuous Foreground Service handling bi-directional real-time audio-to-audio streaming 
 * with the Gemini Multimodal Live API using Google Standard LiveModel & LiveSession abstractions.
 */
class VoiceCoachService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private var liveSession: LiveSession? = null

    companion object {
        private const val TAG = "VoiceCoachService"
        private const val CHANNEL_ID = "voice_coach_channel_id"
        private const val NOTIFICATION_ID = 8812

        // Live API state observables matching screen bindings
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _isLiveConnected = MutableStateFlow(false)
        val isLiveConnected: StateFlow<Boolean> = _isLiveConnected.asStateFlow()

        private val _isSpeaking = MutableStateFlow(false)
        val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

        private val _wakeWordSpotted = MutableStateFlow(false)
        val wakeWordSpotted: StateFlow<Boolean> = _wakeWordSpotted.asStateFlow()

        private val _amplitudes = MutableStateFlow<List<Float>>(List(50) { 0.0f })
        val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, VoiceCoachService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VoiceCoachService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        val hasRecordPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasRecordPermission) {
            _isServiceActive.value = false
            stopSelf()
            return
        }

        initializeLiveSession()
    }

    private fun initializeLiveSession() {
        var apiKey = ""
        try {
            apiKey = BuildConfig.VOICE_COACH_API_KEY
        } catch (e: Exception) {
            // Ignoring if not in BuildConfig
        }

        if (apiKey.isEmpty() || apiKey == "MY_VOICE_COACH_API_KEY" || apiKey == "null") {
            try {
                val properties = java.util.Properties()
                applicationContext.assets.open("secrets.properties").use { properties.load(it) }
                val assetKey = properties.getProperty("VOICE_COACH_API_KEY")
                if (!assetKey.isNullOrEmpty()) {
                    apiKey = assetKey
                }
            } catch (e: Exception) {
                Log.w(TAG, "assets/secrets.properties not found for VOICE_COACH_API_KEY.")
            }
        }

        if (apiKey.isEmpty() || apiKey == "MY_VOICE_COACH_API_KEY" || apiKey == "null") {
            val isArabic = java.util.Locale.getDefault().language == "ar"
            android.widget.Toast.makeText(
                applicationContext,
                if (isArabic) "عذراً يا بطل! مفتاح الـ VOICE_COACH_API_KEY للذكاء الاصطناعي مفقود أو غير مفعّل. يرجى تهيئته لتشغيل الكابتن."
                else "Voice Coach API key is missing or invalid. Please configure VOICE_COACH_API_KEY to run the coach.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            _isServiceActive.value = false
            stopSelf()
            return
        }

        // Real start after key check pass
        _isServiceActive.value = true
        createNotificationChannel()
        startServiceForeground()

        // 1. Instantiating standard LiveModel with fallback key
        val coachInstruction = "أنت كابتن جيم مصري محترف. تتحدث باللهجة المصرية الحماسية. مهمتك تحميس المتدرب، متابعة عداته، والرد بإجابات قصيرة جداً ومباشرة وحماسية في صالة الجيم."
        val liveModel = LiveModel(apiKey, coachInstruction)
        liveSession = liveModel.createSession()

        // 2. Direct bindings capturing session states and feeding visualizers
        scope.launch {
            liveSession?.isConnected?.collect { connected ->
                _isLiveConnected.value = connected
            }
        }

        scope.launch {
            liveSession?.connectionError?.collect { errorMsg ->
                if (errorMsg != null) {
                    val isArabic = java.util.Locale.getDefault().language == "ar"
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            applicationContext,
                            if (isArabic) "فشل الاتصال بالكابتن الذكي. يرجى التأكد من الاتصال بالإنترنت وصلاحية مفتاح الـ API."
                            else "Coach connection failed. Please check internet connection and key validity.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        _isServiceActive.value = false
                        _isLiveConnected.value = false
                        _isSpeaking.value = false
                        stopSelf()
                    }
                }
            }
        }

        scope.launch {
            liveSession?.isSpeaking?.collect { speaking ->
                _isSpeaking.value = speaking
            }
        }

        scope.launch {
            liveSession?.amplitudes?.collect { amps ->
                _amplitudes.value = amps
                
                // Audio-based automatic wake-word pattern detection or intensity peaks
                val maxAmp = amps.maxOrNull() ?: 0f
                if (maxAmp > 0.65f && !_wakeWordSpotted.value) {
                    _wakeWordSpotted.value = true
                    launch {
                        delay(3500)
                        _wakeWordSpotted.value = false
                    }
                }
            }
        }

        // 3. Kickstart standardized high-performance audio conversation loop
        liveSession?.startAudioConversation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Live Voice Coach Broadcaster"
            val descriptionText = "Continuous high fidelity audio stream channel for AI Voice Coach"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startServiceForeground() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VoiceCoachService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("جيم برو - المدرب الصوتي")
            .setContentText("الكابتن يستمع إليك الآن... أنطق 'يا كابتن' لتنشيط التوجيه")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "إيقاف المدرب",
                stopPendingIntent
            )
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            _isServiceActive.value = false
            _isLiveConnected.value = false
            _isSpeaking.value = false
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        _isLiveConnected.value = false
        _isSpeaking.value = false
        _wakeWordSpotted.value = false
        _amplitudes.value = List(50) { 0.0f }
        
        liveSession?.stopAudioConversation()
        scope.cancel()
        Log.d(TAG, "Standard Continuous Foreground Audio Service fully terminated.")
    }
}
