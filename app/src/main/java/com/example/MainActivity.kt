package com.example

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// Enum for screens
enum class AppScreen {
    SPLASH,
    DASHBOARD,
    READY_COUNTDOWN,
    ACTIVE,
    COMPLETED,
    IMAGE_ANALYSIS,
    SMART_CHEF,
    IOS_GLASS_SHOWCASE
}

// Workout session sub-states
enum class WorkoutState {
    PREPHASE, // 3-2-1
    TRAINING, // energetic work
    RESTING,  // cold/teal rest
    PAUSED
}

// Localization lookup helper
object Loc {
    fun getString(key: String, isAr: Boolean): String {
        return if (isAr) {
            when (key) {
                "app_title" -> "Gym Pro"
                "app_subtitle" -> "المدرب والمؤقت الرياضي الذكي"
                "sets" -> "المجاميع"
                "set_label" -> "المجموعة"
                "workout_time" -> "وقت التمرين"
                "rest_time" -> "وقت الراحة"
                "counting_type" -> "نوع العد"
                "timer_mode" -> "مؤقت تنازلي (ثواني)"
                "counter_mode" -> "عداد يدوي (تكرارات)"
                "minute_abb" -> "د"
                "second_abb" -> "ث"
                "start" -> "ابدأ التمرين"
                "pause" -> "إيقاف مؤقت"
                "resume" -> "استئناف"
                "reset" -> "إعادة ضبط"
                "skip" -> "تخطي"
                "quit" -> "إنهاء"
                "congrats" -> "أحسنت!"
                "completed_msg" -> "لقد أتممت التمرين بنجاح!"
                "developer" -> "المطور"
                "about_desc" -> "جيم برو هو مدرب ومؤقت رياضي ذكي فاخر مصمم لأصحاب الأداء العالي. يوفر واجهة زجاجية مذهلة مع مدرب صوتي ذكي لمساعدتك في أداء التمارين بكل انضباط وترتيب."
                "whats_1" -> "واتساب أساسي"
                "whats_2" -> "واتساب احتياطي"
                "instagram" -> "حساب إنستجرام"
                "voice_coach" -> "المدرب الصوتي"
                "vibration" -> "الاهتزاز"
                "presets" -> "الأنماط السريعة"
                "hiit" -> "تدريب HIIT مكثف"
                "strength_preset" -> "تمارين القوة"
                "tabata" -> "نمط التاباتا الرياضي"
                "cardio" -> "كارديو مستمر"
                "prepare" -> "استعد للتمرين"
                "reps" -> "التكرارات"
                "add_rep" -> "+ سجل تكرار"
                "done" -> "تم بنجاح"
                "total_sets_done" -> "المجاميع المكتملة"
                "total_reps_done" -> "إجمالي التكرارات"
                "duration" -> "المدة الإجمالية"
                "back_to_config" -> "شاشة الإعدادات"
                "about_app" -> "حول التطبيق"
                "dev_section" -> "قسم المطور"
                "dev_name" -> "محمد سيد سالم"
                "language" -> "English"
                "analyze_image" -> "تحليل صورة بالذكاء الاصطناعي"
                "upload_image" -> "رفع صورة"
                "scan_ai" -> "بدء التحليل"
                "analyzing" -> "جاري التحليل..."
                "analysis_result" -> "نتيجة التحليل"
                "back" -> "رجوع"
                else -> key
            }
        } else {
            when (key) {
                "app_title" -> "Gym Pro"
                "app_subtitle" -> "Smart Luxury Workout Coach"
                "sets" -> "Sets"
                "set_label" -> "Set"
                "workout_time" -> "Workout Time"
                "rest_time" -> "Rest Time"
                "counting_type" -> "Counting Type"
                "timer_mode" -> "Timer (Countdown)"
                "counter_mode" -> "Manual Counter (Reps)"
                "minute_abb" -> "m"
                "second_abb" -> "s"
                "start" -> "START WORKOUT"
                "pause" -> "PAUSE"
                "resume" -> "RESUME"
                "reset" -> "RESET"
                "skip" -> "SKIP"
                "quit" -> "QUIT"
                "congrats" -> "CONGRATULATIONS!"
                "completed_msg" -> "You completed your session successfully!"
                "developer" -> "Developer"
                "about_desc" -> "Gym Pro is an elite, ultra-modern premium workout timer and reps counter designed for world-class athletes. Features a gorgeous glassmorphic interface, a smart voice coach, and physical tactile sync for flawless gym sessions."
                "whats_1" -> "Primary WhatsApp"
                "whats_2" -> "Secondary WhatsApp"
                "instagram" -> "Instagram Profile"
                "voice_coach" -> "Voice Coach"
                "vibration" -> "Vibration"
                "presets" -> "Quick Presets"
                "hiit" -> "HIIT Workout"
                "strength_preset" -> "Strength Routine"
                "tabata" -> "Tabata Protocol"
                "cardio" -> "Continuous Cardio"
                "prepare" -> "PREPARE..."
                "reps" -> "Reps Count"
                "add_rep" -> "+ TAP REP"
                "done" -> "DONE"
                "total_sets_done" -> "Sets Completed"
                "total_reps_done" -> "Total Reps"
                "duration" -> "Total Duration"
                "back_to_config" -> "Setup Page"
                "about_app" -> "About Gym Pro"
                "dev_section" -> "Developer Section"
                "dev_name" -> "Mohamed Sayed Salem"
                "language" -> "العربية"
                "analyze_image" -> "AI Image Analysis"
                "upload_image" -> "Upload Photo"
                "scan_ai" -> "Scan with AI"
                "analyzing" -> "Analyzing..."
                "analysis_result" -> "Analysis Result"
                "back" -> "Back"
                else -> key
            }
        }
    }
}

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var toneGen: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val key = BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            // Safely read api key to prevent initialization crashes
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Pre-initialize Room Database off the UI thread
                GymDatabase.getDatabase(this@MainActivity)
            } catch (e: Exception) {}
        }

        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Throwable) {}

        lifecycleScope.launch {
            try {
                // TTS constructor must be called on the main/UI thread where a Looper is present,
                // but launching it here asynchronously avoids blocking onCreate during startup.
                tts = TextToSpeech(this@MainActivity, this@MainActivity)
            } catch (e: Throwable) {
                isTtsReady = false
            }
        }
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GymProApp(this@MainActivity)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                isTtsReady = true
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(0.95f)
            } catch (e: Exception) {
                isTtsReady = false
            }
        }
    }

    fun playBeep() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_PIP, 100)
        } catch (t: Throwable) {}
    }

    fun playLongBeep() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300)
        } catch (t: Throwable) {}
    }

    fun playSuccessBeep() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 350)
        } catch (t: Throwable) {}
    }

    fun speak(text: String, isArabic: Boolean) {
        if (isTtsReady && tts != null) {
            try {
                tts?.language = if (isArabic) Locale("ar") else Locale.US
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WorkoutTTS")
            } catch (t: Throwable) {}
        }
    }

    fun triggerVibration(durationMs: Long) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(durationMs, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (t: Throwable) {}
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (t: Throwable) {}
        try {
            toneGen?.release()
        } catch (t: Throwable) {}
        super.onDestroy()
    }
}

@Composable
fun GymProApp(activity: MainActivity) {
    var screenState by rememberSaveable { mutableStateOf(AppScreen.SPLASH) }
    var isArabic by rememberSaveable { 
        mutableStateOf(java.util.Locale.getDefault().language == "ar") 
    }
    var isVoiceEnabled by rememberSaveable { mutableStateOf(true) }
    var isVibEnabled by rememberSaveable { mutableStateOf(true) }

    // Workout configuration settings
    var totalSets by rememberSaveable { mutableIntStateOf(3) }
    var workoutTimeSeconds by rememberSaveable { mutableIntStateOf(45) }
    var restTimeSeconds by rememberSaveable { mutableIntStateOf(15) }
    var isTimerMode by rememberSaveable { mutableStateOf(true) } // true for Timer, false for Counter

    // Runtime state tracking variables
    var activeSet by rememberSaveable { mutableIntStateOf(1) }
    var currentWorkoutState by rememberSaveable { mutableStateOf(WorkoutState.PREPHASE) }
    var runningTimeLeft by rememberSaveable { mutableIntStateOf(45) }
    var runningRestLeft by rememberSaveable { mutableIntStateOf(15) }
    var repsCount by rememberSaveable { mutableIntStateOf(0) }
    var totalRepsLogged by rememberSaveable { mutableIntStateOf(0) }
    var isPaused by rememberSaveable { mutableStateOf(false) }

    // About app popup triggers
    var showAboutDialog by remember { mutableStateOf(false) }

    // Mirror Arabic layout direction dynamically
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalBlack),
            contentAlignment = Alignment.TopCenter
        ) {
            // Cinematic Background Glowing Orbs
            AnimatedBackgroundSpheres(workoutState = currentWorkoutState, screenState = screenState)

            // Responsive App Container
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
            ) {
                // Switch layout screens
                when (screenState) {
                AppScreen.SPLASH -> {
                    SplashScreen {
                        activity.triggerVibration(100)
                        activity.speak(Loc.getString("app_title", isArabic) + ". " + Loc.getString("app_subtitle", isArabic), isArabic)
                        screenState = AppScreen.DASHBOARD
                    }
                }

                AppScreen.DASHBOARD -> {
                    val viewModel: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    MainDashboardScreen(
                        isArabic = isArabic,
                        onLanguageToggle = { isArabic = !isArabic },
                        onImageAnalyzeClick = { screenState = AppScreen.IMAGE_ANALYSIS },
                        onGlassShowcaseClick = { screenState = AppScreen.IOS_GLASS_SHOWCASE },
                        timerContent = {
                            ConfigurationScreen(
                                isArabic = isArabic,
                                isVoiceEnabled = isVoiceEnabled,
                                isVibEnabled = isVibEnabled,
                                totalSets = totalSets,
                                workoutTimeSeconds = workoutTimeSeconds,
                                restTimeSeconds = restTimeSeconds,
                                isTimerMode = isTimerMode,
                                onVoiceToggle = { isVoiceEnabled = !isVoiceEnabled },
                                onVibToggle = { isVibEnabled = !isVibEnabled },
                                onSetsChange = { totalSets = it },
                                onWorkTimeChange = { workoutTimeSeconds = it },
                                onRestTimeChange = { restTimeSeconds = it },
                                onModeChange = { isTimerMode = it },
                                onAboutClick = {},
                                onImageAnalyzeClick = { screenState = AppScreen.IMAGE_ANALYSIS },
                                onVoiceCoachClick = {},
                                onStartWorkout = {
                                    activeSet = 1
                                    repsCount = 0
                                    totalRepsLogged = 0
                                    runningTimeLeft = workoutTimeSeconds
                                    runningRestLeft = restTimeSeconds
                                    currentWorkoutState = WorkoutState.PREPHASE
                                    isPaused = false
                                    screenState = AppScreen.READY_COUNTDOWN
                                }
                            )
                        },
                        workoutContent = {
                            WorkoutTrackerScreen(
                                isArabic = isArabic,
                                viewModel = viewModel,
                                activity = activity
                            )
                        },
                        mealsContent = {
                            MealPlannerScreen(
                                isArabic = isArabic,
                                viewModel = viewModel,
                                onSmartChefClick = { screenState = AppScreen.SMART_CHEF }
                            )
                        }
                    )
                }

                AppScreen.READY_COUNTDOWN -> {
                    ReadyCountdownScreen(
                        isArabic = isArabic,
                        activity = activity,
                        isVoiceEnabled = isVoiceEnabled,
                        onFinished = {
                            screenState = AppScreen.ACTIVE
                            currentWorkoutState = WorkoutState.TRAINING
                            if (isVoiceEnabled) {
                                activity.speak(
                                    Loc.getString("set_label", isArabic) + " 1. " + Loc.getString("start", isArabic),
                                    isArabic
                                )
                            }
                        }
                    )
                }

                AppScreen.ACTIVE -> {
                    ActiveWorkoutScreen(
                        isArabic = isArabic,
                        activity = activity,
                        totalSets = totalSets,
                        activeSet = activeSet,
                        workoutTimeSec = workoutTimeSeconds,
                        restTimeSec = restTimeSeconds,
                        isTimerMode = isTimerMode,
                        isVoiceEnabled = isVoiceEnabled,
                        isVibEnabled = isVibEnabled,
                        isPaused = isPaused,
                        runningTimeLeft = runningTimeLeft,
                        runningRestLeft = runningRestLeft,
                        repsCount = repsCount,
                        workoutState = currentWorkoutState,
                        onTimeTick = { runningTimeLeft = it },
                        onRestTick = { runningRestLeft = it },
                        onRepInc = {
                            repsCount++
                            totalRepsLogged++
                            if (isVibEnabled) activity.triggerVibration(60)
                            activity.playBeep()
                        },
                        onRepDec = {
                            if (repsCount > 0) {
                                repsCount--
                                totalRepsLogged--
                                if (isVibEnabled) activity.triggerVibration(40)
                            }
                        },
                        onStateShift = { nextState -> currentWorkoutState = nextState },
                        onSetIndexInc = { activeSet++ },
                        onPauseToggle = {
                            isPaused = !isPaused
                            if (isVoiceEnabled) {
                                val t = if (isPaused) Loc.getString("pause", isArabic) else Loc.getString("resume", isArabic)
                                activity.speak(t, isArabic)
                            }
                        },
                        onSkip = {
                            if (currentWorkoutState == WorkoutState.TRAINING) {
                                if (activeSet < totalSets) {
                                    currentWorkoutState = WorkoutState.RESTING
                                    runningRestLeft = restTimeSeconds
                                    if (isVoiceEnabled) activity.speak(Loc.getString("rest_time", isArabic), isArabic)
                                } else {
                                    activity.playSuccessBeep()
                                    if (isVoiceEnabled) activity.speak(Loc.getString("completed_msg", isArabic), isArabic)
                                    screenState = AppScreen.COMPLETED
                                }
                            } else if (currentWorkoutState == WorkoutState.RESTING) {
                                activeSet++
                                repsCount = 0
                                currentWorkoutState = WorkoutState.TRAINING
                                runningTimeLeft = workoutTimeSeconds
                                if (isVoiceEnabled) {
                                    activity.speak(
                                        Loc.getString("set_label", isArabic) + " $activeSet. " + Loc.getString("start", isArabic),
                                        isArabic
                                    )
                                }
                            }
                        },
                        onQuit = {
                            screenState = AppScreen.DASHBOARD
                        },
                        onFinishWorkout = {
                            activity.playSuccessBeep()
                            if (isVoiceEnabled) activity.speak(Loc.getString("completed_msg", isArabic), isArabic)
                            screenState = AppScreen.COMPLETED
                        }
                    )
                }

                AppScreen.COMPLETED -> {
                    CompletedScreen(
                        isArabic = isArabic,
                        totalSets = totalSets,
                        isTimerMode = isTimerMode,
                        totalReps = totalRepsLogged,
                        durationSec = totalSets * workoutTimeSeconds,
                        onBack = {
                            screenState = AppScreen.DASHBOARD
                        }
                    )
                }

                AppScreen.IMAGE_ANALYSIS -> {
                    val viewModel: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    ImageAnalysisScreen(
                        isArabic = isArabic,
                        onBack = { screenState = AppScreen.DASHBOARD },
                        activity = activity,
                        viewModel = viewModel
                    )
                }

                AppScreen.SMART_CHEF -> {
                    val viewModel: SmartChefViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    SmartChefScreen(
                        isArabic = isArabic,
                        onBack = { screenState = AppScreen.DASHBOARD },
                        viewModel = viewModel
                    )
                }

                AppScreen.IOS_GLASS_SHOWCASE -> {
                    IosGlassEngineScreen(
                        isArabic = isArabic,
                        onBack = { screenState = AppScreen.DASHBOARD }
                    )
                }
            } // Close when (screenState)
            } // Close the Responsive App Container

            // Glassmorphic About Dialog
            if (showAboutDialog) {
                AboutGymProDialog(
                    isArabic = isArabic,
                    onDismiss = { showAboutDialog = false }
                )
            }
        }
    }
}

// Background glows relative to states
@Composable
fun AnimatedBackgroundSpheres(workoutState: WorkoutState, screenState: AppScreen) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spheres_scale"
    )

    // Vibrant background theme adjustment based on operational states
    val coreGlowColor = when (screenState) {
        AppScreen.SPLASH -> SportyRed
        AppScreen.COMPLETED -> ChampionshipGold
        AppScreen.DASHBOARD -> SportyRed
        else -> {
            when (workoutState) {
                WorkoutState.TRAINING -> SportyRed
                WorkoutState.RESTING -> AccentTeal
                WorkoutState.PAUSED -> Color.DarkGray
                else -> SportyRed
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Upper left primary blob
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-70).dp)
                .size(380.dp)
                .alpha(0.18f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(coreGlowColor, Color.Transparent)
                    )
                )
        )

        // Bottom right backup glowing ambient aura
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-130).dp, y = 140.dp)
                .size(440.dp * scaleAnim)
                .alpha(0.14f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(coreGlowColor, Color.Transparent)
                    )
                )
        )
    }
}

// SPLASH SCREEN
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Multi-stage cinematic enter animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = LinearOutSlowInEasing)
        )
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_transparent),
                contentDescription = "Gym Pro Logo",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "GYM PRO",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PREMIUM ATHLETIC ENGINE",
                color = SportyRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(alpha.value)
            )

            Spacer(modifier = Modifier.height(100.dp))

            // Premium loading bar indicator
            LinearProgressIndicator(
                color = SportyRed,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

// APP CONFIGURATION PAGE (MAIN SCREEN CONFIG STATE)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigurationScreen(
    isArabic: Boolean,
    isVoiceEnabled: Boolean,
    isVibEnabled: Boolean,
    totalSets: Int,
    workoutTimeSeconds: Int,
    restTimeSeconds: Int,
    isTimerMode: Boolean,
    onVoiceToggle: () -> Unit,
    onVibToggle: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onWorkTimeChange: (Int) -> Unit,
    onRestTimeChange: (Int) -> Unit,
    onModeChange: (Boolean) -> Unit,
    onAboutClick: () -> Unit,
    onImageAnalyzeClick: () -> Unit,
    onVoiceCoachClick: () -> Unit,
    onStartWorkout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberCharcoal)
    ) {
        // 1. Ambient yellow glow top background effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SportyRed.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        // 2. Main scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 120.dp), // Removed top padding from Modifier
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Spacer to allow content to start below fixed header but scroll behind it
            Spacer(modifier = Modifier.height(96.dp))
            
            // Header was here, now deleted and replaced by top box entry

            // ----------------- GLOBAL VOICE COACH & VIBRATION SINGLE CONTAINER -----------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .iosSquircleCard(
                        cornerSizeRatio = 0.24f,
                        baseOpacity = 0.20f,
                        saturationScale = 1.80f
                    )
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Voice Toggle column-cell
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVoiceToggle() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVoiceEnabled) PhosphorIcons.VolumeUp else PhosphorIcons.VolumeOff,
                        contentDescription = null,
                        tint = if (isVoiceEnabled) SportyRed else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.getString("voice_coach", isArabic),
                        color = if (isVoiceEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                // Vibration Toggle column-cell
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVibToggle() },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Vibration,
                        contentDescription = null,
                        tint = if (isVibEnabled) SportyRed else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Loc.getString("vibration", isArabic),
                        color = if (isVibEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ----------------- SETS CONTROL ROW & SUB PRESETS -----------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.getString("sets", isArabic).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .recessedGlassInput(cornerSizeRatio = 0.45f, baseOpacity = 0.06f)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (totalSets > 1) onSetsChange(totalSets - 1) },
                            enabled = totalSets > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Remove,
                                contentDescription = "Decrease Sets",
                                tint = if (totalSets > 1) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = totalSets.toString(),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        IconButton(
                            onClick = { if (totalSets < 30) onSetsChange(totalSets + 1) },
                            enabled = totalSets < 30,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Add,
                                contentDescription = "Increase Sets",
                                tint = if (totalSets < 30) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Horizontal Preset Buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 8, 10, 12).forEach { p ->
                        val presetLabel = if (isArabic) "$p مجاميع" else "$p sets"
                        PresetButtonPill(label = presetLabel, active = totalSets == p) {
                            onSetsChange(p)
                        }
                    }
                }
            }

            // ----------------- WORKOUT TIME CONTROL ROW & SUB PRESETS -----------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val workMin = workoutTimeSeconds / 60
                val workSec = workoutTimeSeconds % 60
                val workDisplay = if (workoutTimeSeconds >= 60) {
                    if (workSec == 0) "${workMin}m" else "${workMin}m ${workSec}s"
                } else {
                    "${workoutTimeSeconds}s"
                }
                val workDisplayAr = if (workoutTimeSeconds >= 60) {
                    if (workSec == 0) "$workMin د" else "$workMin د $workSec ث"
                } else {
                    "$workoutTimeSeconds ث"
                }
                val finalWorkDisplay = if (isArabic) workDisplayAr else workDisplay

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.getString("workout_time", isArabic).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .recessedGlassInput(cornerSizeRatio = 0.45f, baseOpacity = 0.06f)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (workoutTimeSeconds > 5) onWorkTimeChange(workoutTimeSeconds - 5) },
                            enabled = workoutTimeSeconds > 5,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Remove,
                                contentDescription = "Decrease Workout Time",
                                tint = if (workoutTimeSeconds > 5) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = finalWorkDisplay,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )

                        IconButton(
                            onClick = { if (workoutTimeSeconds < 1800) onWorkTimeChange(workoutTimeSeconds + 5) },
                            enabled = workoutTimeSeconds < 1800,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Add,
                                contentDescription = "Increase Workout Time",
                                tint = if (workoutTimeSeconds < 1800) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Horizontal Preset Buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 30, 45, 60, 120).forEach { p ->
                        val presetLabel = if (p >= 60) {
                            if (isArabic) "${p / 60} د" else "${p / 60}m"
                        } else {
                            if (isArabic) "$p ث" else "${p}s"
                        }
                        PresetButtonPill(label = presetLabel, active = workoutTimeSeconds == p) {
                            onWorkTimeChange(p)
                        }
                    }
                }
            }

            // ----------------- REST TIME CONTROL ROW & SUB PRESETS -----------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val restMin = restTimeSeconds / 60
                val restSec = restTimeSeconds % 60
                val restDisplay = if (restTimeSeconds == 0) {
                    if (isArabic) "بدون راحة" else "No Rest"
                } else if (restTimeSeconds >= 60) {
                    if (restSec == 0) "${restMin}m" else "${restMin}m ${restSec}s"
                } else {
                    "${restTimeSeconds}s"
                }
                val restDisplayAr = if (restTimeSeconds == 0) {
                    "بدون راحة"
                } else if (restTimeSeconds >= 60) {
                    if (restSec == 0) "$restMin د" else "$restMin د $restSec ث"
                } else {
                    "$restTimeSeconds ث"
                }
                val finalRestDisplay = if (isArabic) restDisplayAr else restDisplay

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.getString("rest_time", isArabic).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier
                            .recessedGlassInput(cornerSizeRatio = 0.45f, baseOpacity = 0.06f)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (restTimeSeconds > 0) onRestTimeChange(maxOf(0, restTimeSeconds - 5)) },
                            enabled = restTimeSeconds > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Remove,
                                contentDescription = "Decrease Rest Time",
                                tint = if (restTimeSeconds > 0) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = finalRestDisplay,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )

                        IconButton(
                            onClick = { if (restTimeSeconds < 600) onRestTimeChange(restTimeSeconds + 5) },
                            enabled = restTimeSeconds < 600,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Add,
                                contentDescription = "Increase Rest Time",
                                tint = if (restTimeSeconds < 600) Color.White else Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Horizontal Preset Buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 10, 15, 30, 45, 60).forEach { p ->
                        val presetLabel = if (p == 0) {
                            if (isArabic) "بدون راحة" else "No Rest"
                        } else if (p >= 60) {
                            if (isArabic) "${p / 60} د" else "${p / 60}m"
                        } else {
                            if (isArabic) "$p ث" else "${p}s"
                        }
                        PresetButtonPill(label = presetLabel, active = restTimeSeconds == p) {
                            onRestTimeChange(p)
                        }
                    }
                }
            }

            // ----------------- COUNTING MODE SELECTOR -----------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Loc.getString("counting_type", isArabic).uppercase(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .recessedGlassInput(cornerSizeRatio = 0.45f, baseOpacity = 0.08f)
                        .padding(4.dp)
                ) {
                    // Timer Tab option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SquircleShape(0.40f))
                            .background(if (isTimerMode) SportyRed else Color.Transparent)
                            .clickable { onModeChange(true) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.getString("timer_mode", isArabic),
                            color = if (isTimerMode) CyberCharcoal else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Manual Counter Tab option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SquircleShape(0.40f))
                            .background(if (!isTimerMode) SportyRed else Color.Transparent)
                            .clickable { onModeChange(false) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Loc.getString("counter_mode", isArabic),
                            color = if (!isTimerMode) CyberCharcoal else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ----------------- POPULAR PRESET GRID -----------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = Loc.getString("presets", isArabic).uppercase(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: HIIT Workout
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .iosSquircleCard(
                                cornerSizeRatio = 0.28f,
                                baseOpacity = 0.18f,
                                saturationScale = 1.80f
                            )
                            .clickable {
                                onSetsChange(3)
                                onWorkTimeChange(45)
                                onRestTimeChange(15)
                                onModeChange(true)
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = Loc.getString("hiit", isArabic),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "3 مجاميع • 45 ث • 15 ث" else "3 Sets • 45s • 15s",
                                color = SportyRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Card 2: Tabata Protocol
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .iosSquircleCard(
                                cornerSizeRatio = 0.28f,
                                baseOpacity = 0.18f,
                                saturationScale = 1.80f
                            )
                            .clickable {
                                onSetsChange(8)
                                onWorkTimeChange(20)
                                onRestTimeChange(10)
                                onModeChange(true)
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = Loc.getString("tabata", isArabic),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "8 مجاميع • 20 ث • 10 ث" else "8 Sets • 20s • 10s",
                                color = SportyRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ----------------- START WORKOUT ACTION BUTTON -----------------
            Button(
                onClick = onStartWorkout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SportyRed,
                    contentColor = CyberCharcoal
                ),
                shape = SquircleShape(0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(60.dp)
                    .testTag("start_workout_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Dumbbell,
                        contentDescription = null,
                        tint = CyberCharcoal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Loc.getString("start", isArabic).uppercase(),
                        color = CyberCharcoal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Custom Glass Segment Counter/Ajuster with gorgeous display text
@Composable
fun GlassValueAdjuster(
    value: Int,
    minValue: Int,
    maxValue: Int,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    unitLabel: String,
    isArabic: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlassWhite, RoundedCornerShape(20.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Decrement button
        IconButton(
            onClick = { if (value - step >= minValue) onValueChange(value - step) },
            enabled = value > minValue,
            modifier = Modifier
                .size(46.dp)
                .background(GlassWhite, CircleShape)
                .border(1.dp, GlassBorder, CircleShape)
        ) {
            Icon(
                imageVector = PhosphorIcons.Remove,
                contentDescription = "Decrease",
                tint = if (value > minValue) Color.White else Color.White.copy(0.3f)
            )
        }

        // Center displayed values
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (unitLabel.isEmpty()) value.toString() else unitLabel,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Increment button
        IconButton(
            onClick = { if (value + step <= maxValue) onValueChange(value + step) },
            enabled = value < maxValue,
            modifier = Modifier
                .size(46.dp)
                .background(GlassWhite, CircleShape)
                .border(1.dp, GlassBorder, CircleShape)
        ) {
            Icon(
                imageVector = PhosphorIcons.Add,
                contentDescription = "Increase",
                tint = if (value < maxValue) Color.White else Color.White.copy(0.3f)
            )
        }
    }
}

@Composable
fun PresetButtonPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) SportyRed.copy(0.15f) else Color.Transparent)
            .border(
                1.dp,
                if (active) SportyRed else Color.White.copy(alpha = 0.15f),
                CircleShape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (active) SportyRed else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ShortcutPresetCard(
    title: String,
    specs: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = specs,
                color = SportyRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ACTIVE PREPARATION START TIMER (3-2-1 READY SCREEN)
@Composable
fun ReadyCountdownScreen(
    isArabic: Boolean,
    activity: MainActivity,
    isVoiceEnabled: Boolean,
    onFinished: () -> Unit
) {
    var countSeconds by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        if (isVoiceEnabled) {
            activity.speak(Loc.getString("prepare", isArabic), isArabic)
        }
        while (countSeconds > 0) {
            activity.triggerVibration(70)
            activity.playBeep()
            delay(1000)
            countSeconds--
        }
        activity.playLongBeep()
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = Loc.getString("prepare", isArabic),
                color = SportyRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Massive anim countdown representation
            AnimatedContent(
                targetState = countSeconds,
                transitionSpec = {
                    scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            scaleOut(animationSpec = tween(300))
                },
                label = "prep_num"
            ) { count ->
                Text(
                    text = count.toString(),
                    color = SportyRed, // SportyRed is Neon Yellow #CCFF00
                    fontSize = 130.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ACTIVE WORKOUT TIMER SCREEN
@Composable
fun ActiveWorkoutScreen(
    isArabic: Boolean,
    activity: MainActivity,
    totalSets: Int,
    activeSet: Int,
    workoutTimeSec: Int,
    restTimeSec: Int,
    isTimerMode: Boolean,
    isVoiceEnabled: Boolean,
    isVibEnabled: Boolean,
    isPaused: Boolean,
    runningTimeLeft: Int,
    runningRestLeft: Int,
    repsCount: Int,
    workoutState: WorkoutState,
    onTimeTick: (Int) -> Unit,
    onRestTick: (Int) -> Unit,
    onRepInc: () -> Unit,
    onRepDec: () -> Unit,
    onStateShift: (WorkoutState) -> Unit,
    onSetIndexInc: () -> Unit,
    onPauseToggle: () -> Unit,
    onSkip: () -> Unit,
    onQuit: () -> Unit,
    onFinishWorkout: () -> Unit
) {
    val context = LocalContext.current

    // Background and highlight styling according to our target state
    val statusColor = when (workoutState) {
        WorkoutState.TRAINING -> SportyRed
        WorkoutState.RESTING -> AccentTeal
        WorkoutState.PAUSED -> Color.DarkGray
        else -> SportyRed
    }

    // Active tick countdown delay handling
    LaunchedEffect(isPaused, workoutState, runningTimeLeft, runningRestLeft) {
        if (!isPaused) {
            if (workoutState == WorkoutState.TRAINING && isTimerMode) {
                if (runningTimeLeft > 0) {
                    delay(1000)
                    val nextTick = runningTimeLeft - 1
                    onTimeTick(nextTick)

                    // Audio coaching ticks in the final 3 seconds
                    if (nextTick in 1..3) {
                        activity.playBeep()
                        if (isVibEnabled) activity.triggerVibration(60)
                    }
                } else {
                    // Current workout set timer completed
                    activity.triggerVibration(250)
                    if (activeSet < totalSets) {
                        onStateShift(WorkoutState.RESTING)
                        onRestTick(restTimeSec)
                        if (isVoiceEnabled) {
                            activity.speak(
                                Loc.getString("rest_time", isArabic),
                                isArabic
                            )
                        }
                    } else {
                        // Flawless final workout completion
                        onFinishWorkout()
                    }
                }
            } else if (workoutState == WorkoutState.RESTING) {
                if (runningRestLeft > 0) {
                    delay(1000)
                    val nextRest = runningRestLeft - 1
                    onRestTick(nextRest)

                    // countdown rest beeps
                    if (nextRest in 1..3) {
                        activity.playBeep()
                        if (isVibEnabled) activity.triggerVibration(60)
                    }
                } else {
                    // Rest time completed. Advance set
                    activity.triggerVibration(250)
                    activity.playLongBeep()
                    onSetIndexInc()
                    onStateShift(WorkoutState.TRAINING)
                    onTimeTick(workoutTimeSec)
                    if (isVoiceEnabled) {
                        activity.speak(
                            Loc.getString("set_label", isArabic) + " ${activeSet + 1}. " + Loc.getString("start", isArabic),
                            isArabic
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ----------------- TOP CAPSULAR HEADER (GYM PRO) -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xE116161A))
                .border(1.dp, Color(0x66CCFF00), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onQuit,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = PhosphorIcons.ArrowBack,
                    contentDescription = "Quit",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_transparent),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GYM PRO",
                    color = NeonYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            // User profile placeholder (cyber-neon design)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorIcons.Trophy,
                    contentDescription = "User Activity",
                    tint = NeonYellow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ----------------- CONTEXT CARD (WORKOUT VS TIME) -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: Workout labels
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Dumbbell,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isArabic) "التمرين" else "WORKOUT",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (workoutState == WorkoutState.TRAINING) {
                            if (isArabic) "جولة تمرين" else "Active Set"
                        } else {
                            if (isArabic) "فترة الراحة" else "Rest Cycle"
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Vertical separating line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            // Right Column: Target/Elapsed Time labels
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Timer,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isArabic) "زمن الجولة" else "SET TIME",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    val targetSec = if (workoutState == WorkoutState.TRAINING) workoutTimeSec else restTimeSec
                    val tm = targetSec / 60
                    val ts = targetSec % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", tm, ts),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ----------------- MASSIVE CIRCULAR TICK TIMER -----------------
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val currentLeft = if (workoutState == WorkoutState.TRAINING) runningTimeLeft else runningRestLeft
            val maxSeconds = if (workoutState == WorkoutState.TRAINING) workoutTimeSec else restTimeSec
            val percent = if (maxSeconds > 0) currentLeft.toFloat() / maxSeconds.toFloat() else 1f

            val smoothSweep by animateFloatAsState(
                targetValue = percent,
                animationSpec = tween(500, easing = LinearEasing),
                label = "progress_wheel"
            )

            // Neon ambient backlight background
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(statusColor.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Canvas drawing background track, static clock ticks, and sweeping dynamic neon arc
            Canvas(modifier = Modifier.size(220.dp)) {
                val radius = size.minDimension / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                val strokeWidthPx = 8.dp.toPx()
                val activeRadius = maxOf(0f, (size.minDimension - strokeWidthPx) / 2f)
                val topLeftOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)
                val arcSize = Size(maxOf(0.1f, size.width - strokeWidthPx), maxOf(0.1f, size.height - strokeWidthPx))

                // 1. Draw extremely faint reference track perfectly aligned
                drawCircle(
                    color = Color.White.copy(alpha = 0.02f),
                    radius = activeRadius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidthPx)
                )

                // 2. Draw circular static clock face ticks (Chronograph markers background)
                val tickCount = 60
                for (i in 0 until tickCount) {
                    val angle = i * (360f / tickCount)
                    val angleRad = Math.toRadians(angle.toDouble() - 90.0) // Start drawing on top (12 o'clock)

                    // Highlight every 5th tick to represent major hour/minute indicators
                    val isMajorTick = i % 5 == 0
                    val tickColor = if (isMajorTick) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                    val tickLength = if (isMajorTick) 10.dp.toPx() else 6.dp.toPx()
                    val tickWidth = if (isMajorTick) 1.5.dp.toPx() else 1.dp.toPx()

                    val innerR = activeRadius - 10.dp.toPx() - tickLength
                    val outerR = activeRadius - 10.dp.toPx()

                    val startX = centerOffset.x + innerR * Math.cos(angleRad).toFloat()
                    val startY = centerOffset.y + innerR * Math.sin(angleRad).toFloat()
                    val endX = centerOffset.x + outerR * Math.cos(angleRad).toFloat()
                    val endY = centerOffset.y + outerR * Math.sin(angleRad).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth
                    )
                }

                // 3. Draw single solid sweeping neon active progress arc (The only scrolling timer indicator)
                val sweep = if (workoutState == WorkoutState.TRAINING && !isTimerMode) 360f else smoothSweep * 360f
                drawArc(
                    color = statusColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            // Numeric Timer Center Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (workoutState == WorkoutState.TRAINING) {
                        if (isArabic) "التمرين الحالي" else "ACTIVE WORKSET"
                    } else {
                        if (isArabic) "مؤقت الراحة" else "REST TIMER"
                    },
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val rawSeconds = if (workoutState == WorkoutState.TRAINING) runningTimeLeft else runningRestLeft
                val m = rawSeconds / 60
                val s = rawSeconds % 60
                val timerTextValue = if (workoutState == WorkoutState.TRAINING && !isTimerMode) {
                    repsCount.toString()
                } else {
                    String.format(Locale.US, "%02d:%02d", m, s)
                }

                Text(
                    text = timerTextValue,
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (workoutState == WorkoutState.TRAINING && !isTimerMode) {
                        if (isArabic) "التكرارات المسجلة" else "LOGGED REPS"
                    } else {
                        if (isArabic) "المتبقي" else "REMAINING"
                    },
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Inline Play/Pause control inside circular timer (Shown only if there is a countdown)
                if (isTimerMode || workoutState == WorkoutState.RESTING) {
                    Spacer(modifier = Modifier.height(14.dp))
                    IconButton(
                        onClick = onPauseToggle,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF131313).copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, statusColor.copy(alpha = 0.3f), CircleShape)
                            .testTag("pause_resume_btn")
                    ) {
                        Icon(
                            imageVector = if (isPaused) PhosphorIcons.Play else PhosphorIcons.Pause,
                            contentDescription = "Play/Pause",
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ----------------- CURRENT PROGRESS STATE DETAILS -----------------
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isArabic) "المجموعة $activeSet من $totalSets" else "SET $activeSet OF $totalSets",
                color = NeonYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (workoutState == WorkoutState.TRAINING) {
                    if (isArabic) "جولة التمرين الحالية" else "Active Training Set"
                } else {
                    if (isArabic) "فترة الراحة والاسترجاع" else "Rest & Recovery Cycle"
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }

        // ----------------- HIGH-END GLASS STATS CARD -----------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SETS
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = PhosphorIcons.Dumbbell,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "المجاميع" else "SETS",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = activeSet.toString(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $totalSets",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            if (!isTimerMode) {
                // REPS (Shown only in manual rep counter mode)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Trophy,
                        contentDescription = null,
                        tint = NeonYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) "التكرارات" else "REPS",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = repsCount.toString(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }

            // REST TIME
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = PhosphorIcons.Timer,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "فترة الراحة" else "REST TIME",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "00:%02d", restTimeSec),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ----------------- EXTRA REPS TACTILE SWITCHERS FOR COUNTER MODE -----------------
        AnimatedVisibility(
            visible = !isTimerMode && workoutState == WorkoutState.TRAINING,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                IconButton(
                    onClick = onRepDec,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Remove,
                        contentDescription = "decrease rep",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onRepInc,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .width(140.dp)
                ) {
                    Text(
                        text = if (isArabic) "+ سجل تكرار" else "+ TAP REP",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onRepInc,
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Add,
                        contentDescription = "increase rep",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        } // End scrollable Content column

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------- PRIMARY ACTION SWITCH BUTTON -----------------
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("skip_btn")
        ) {
            Text(
                text = if (workoutState == WorkoutState.RESTING) {
                    if (isArabic) "تخطي فترة الراحة" else "SKIP REST"
                } else {
                    if (isArabic) "تخطي هذه الجولة" else "SKIP WORKSET"
                },
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

// WORKOUT SESSION COMPLETED SUMMARY SCREEN
@Composable
fun CompletedScreen(
    isArabic: Boolean,
    totalSets: Int,
    isTimerMode: Boolean,
    totalReps: Int,
    durationSec: Int,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .background(GlassWhite, RoundedCornerShape(26.dp))
                .border(2.dp, ChampionshipGold.copy(0.4f), RoundedCornerShape(26.dp))
                .padding(26.dp)
        ) {
            // Trophy glowing logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(ChampionshipGold.copy(0.12f), CircleShape)
                    .border(2.dp, ChampionshipGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorIcons.Trophy,
                    contentDescription = "Trophy Gold",
                    tint = ChampionshipGold,
                    modifier = Modifier.size(45.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = Loc.getString("congrats", isArabic),
                color = ChampionshipGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Text(
                text = Loc.getString("completed_msg", isArabic),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Horizontal Divider
            HorizontalDivider(color = GlassBorder)

            Spacer(modifier = Modifier.height(18.dp))

            // Key data stats lists
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Loc.getString("total_sets_done", isArabic),
                    color = NeutralGray,
                    fontSize = 13.sp
                )
                Text(
                    text = "$totalSets",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isTimerMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Loc.getString("total_reps_done", isArabic),
                        color = NeutralGray,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$totalReps",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Loc.getString("duration", isArabic),
                    color = NeutralGray,
                    fontSize = 13.sp
                )
                val m = durationSec / 60
                val s = durationSec % 60
                Text(
                    text = "$m ${Loc.getString("minute_abb", isArabic)} : $s ${Loc.getString("second_abb", isArabic)}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Close button
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = Loc.getString("back_to_config", isArabic),
                    color = Color.Black,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// GLASSMORPHIC ABOUT & DEVELOPER DIALOG PANEL
@Composable
fun AboutGymProDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Launches direct link
    fun triggerDialWhatsApp(number: String) {
        try {
            val formatted = "https://api.whatsapp.com/send?phone=+20$number"
            val uri = Uri.parse(formatted)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun triggerLaunchInstagram() {
        try {
            val link = "https://www.instagram.com/1x___moh__?igsh=MWQweTdqbzFiendiaw=="
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .border(2.dp, GlassBorder, RoundedCornerShape(26.dp)),
        containerColor = DeepDarkGray.copy(0.96f),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header image/brand look
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(SportyRed.copy(0.12f), CircleShape)
                        .border(1.5.dp, SportyRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_transparent),
                        contentDescription = "Gym Pro Premium logo",
                        modifier = Modifier.size(45.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = Loc.getString("app_title", isArabic),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "v1.0 Premium Premium Edition",
                    color = SportyRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Short App description
                Text(
                    text = Loc.getString("about_desc", isArabic),
                    color = SoftWhite,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = GlassBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // ----------------- DEVELOPER DETAILS SECTION -----------------
                Text(
                    text = Loc.getString("dev_section", isArabic),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Developer's name layout
                Text(
                    text = Loc.getString("dev_name", isArabic),
                    color = SportyRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Senior iOS / Android Mobile UX Architect",
                    color = NeutralGray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- WhatsApp Primary Button ---
                Button(
                    onClick = { triggerDialWhatsApp("01140251843") },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = PhosphorIcons.Chat,
                                contentDescription = "whatsapp 1",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Loc.getString("whats_1", isArabic),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "01140251843",
                            color = NeutralGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- WhatsApp Secondary Button ---
                Button(
                    onClick = { triggerDialWhatsApp("01033254483") },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = PhosphorIcons.Chat,
                                contentDescription = "whatsapp 2",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Loc.getString("whats_2", isArabic),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "01033254483",
                            color = NeutralGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Instagram contact Account Link ---
                Button(
                    onClick = { triggerLaunchInstagram() },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassWhite),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = PhosphorIcons.Camera,
                                contentDescription = "instagram link",
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Loc.getString("instagram", isArabic),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "1x___moh__",
                            color = SportyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SportyRed, contentColor = CyberCharcoal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = Loc.getString("done", isArabic),
                        color = CyberCharcoal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

// --- Image Analysis Utilities ---

suspend fun Bitmap.toBase64(): String = withContext(Dispatchers.IO) {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

@Composable
fun ImageAnalysisScreen(
    isArabic: Boolean,
    onBack: () -> Unit,
    activity: MainActivity,
    viewModel: GymViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val macrosState by viewModel.macroProgress.collectAsState()

    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0: Auto Image Scan, 1: Quick Manual Search
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var customUsdaApiKey by rememberSaveable { mutableStateOf("") }

    // Tab 0: Code for image scan & matches
    var isAnalyzing by remember { mutableStateOf(false) }
    var bitmapState: Bitmap? by remember { mutableStateOf(null) }
    var scannedFoodItems by remember { mutableStateOf<List<DetectedFoodItem>>(emptyList()) }
    var scanError by remember { mutableStateOf("") }

    // Tab 1: Code for manual search
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<UsdaFoodItem>>(emptyList()) }
    var searchError by remember { mutableStateOf("") }

    // Selected item configuration (for manual search card)
    var selectedSearchItem by remember { mutableStateOf<UsdaFoodItem?>(null) }
    var searchItemGrams by remember { mutableStateOf(100) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        scannedFoodItems = emptyList()
        scanError = ""
    }

    LaunchedEffect(imageUri) {
        if(imageUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = imageUri!!
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    activity.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                    
                    val maxDim = 800
                    var srcWidth = options.outWidth
                    var srcHeight = options.outHeight
                    var inSampleSize = 1
                    while (srcWidth / 2 >= maxDim || srcHeight / 2 >= maxDim) {
                        srcWidth /= 2
                        srcHeight /= 2
                        inSampleSize *= 2
                    }
                    
                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                    }
                    val decodedBitmap = activity.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                    
                    if (decodedBitmap != null) {
                        val scaledBitmap = if (decodedBitmap.width > maxDim || decodedBitmap.height > maxDim) {
                            val scale = Math.min(maxDim.toFloat() / decodedBitmap.width, maxDim.toFloat() / decodedBitmap.height)
                            Bitmap.createScaledBitmap(decodedBitmap, (decodedBitmap.width * scale).toInt(), (decodedBitmap.height * scale).toInt(), true)
                        } else decodedBitmap
                        
                        val softwareBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        withContext(Dispatchers.Main) {
                            bitmapState = softwareBitmap
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            bitmapState = null
                        }
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        bitmapState = null
                    }
                }
            }
        } else {
            bitmapState = null
        }
    }

    // Helper to extract USDA API key
    fun getUsdaKey(): String {
        return customUsdaApiKey.trim().ifEmpty { "DEMO_KEY" }
    }

    // Helper functions for logging to Room
    fun logFoodToMacros(desc: String, calsPer100: Double, proteinsPer100: Double, carbsPer100: Double, fatsPer100: Double, portionGrams: Int) {
        val multiplier = portionGrams / 100.0
        val addCals = (calsPer100 * multiplier).toInt()
        val addProt = (proteinsPer100 * multiplier).toInt()
        val addCarb = (carbsPer100 * multiplier).toInt()
        val addFat = (fatsPer100 * multiplier).toInt()

        val progress = macrosState
        val updated = progress.copy(
            calories = progress.calories + addCals,
            protein = progress.protein + addProt,
            carbs = progress.carbs + addCarb,
            fats = progress.fats + addFat
        )
        viewModel.updateMacros(updated)
        activity.triggerVibration(90)
        activity.playBeep()

        val itemSpeech = if (isArabic) {
            "تم تسجيل $portionGrams جراماً من $desc بنجاح مضافاً إليها $addCals سعرة حرارية للماكروز اليومية"
        } else {
            "Successfully logged $portionGrams grams of $desc, adding $addCals calories to your daily target."
        }
        activity.speak(itemSpeech, isArabic)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 1: ELEGANT TITLE HEADER CONTROLLER ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(GlassWhite, CircleShape)
                    .border(1.dp, GlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = PhosphorIcons.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isArabic) "فاحص ومحلل الأغذية الذكي" else "USDA Intelligent Food Tracker",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isArabic) "مطابقة حقيقية وموثوقة عبر USDA Food Data Central" else "Verified matching with USDA Food Data Central",
                    color = DimGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SECTION 2: CAPSULE TAB CONTROL BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassWhite, RoundedCornerShape(20.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // Tab 0: Auto Image Scanner Pill Selector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (activeTab == 0) NeonYellow else Color.Transparent)
                    .clickable {
                        activeTab = 0
                        activity.triggerVibration(45)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = PhosphorIcons.Camera,
                        contentDescription = null,
                        tint = if (activeTab == 0) CyberCharcoal else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تحليل صورة وجبة" else "SCAN MEAL PHOTO",
                        color = if (activeTab == 0) CyberCharcoal else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }

            // Tab 1: Manual Search Pill Selector
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (activeTab == 1) NeonYellow else Color.Transparent)
                    .clickable {
                        activeTab = 1
                        activity.triggerVibration(45)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = PhosphorIcons.ForkKnife,
                        contentDescription = null,
                        tint = if (activeTab == 1) CyberCharcoal else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "بحث يدوي سريع" else "MANUAL SEARCH",
                        color = if (activeTab == 1) CyberCharcoal else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- SECTION 3: API KEY CONFIGURATION FOLDER ---
        var showKeyConfig by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = RockGray.copy(0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showKeyConfig = !showKeyConfig },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(PhosphorIcons.Info, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "إعدادات الربط بقاعدة USDA" else "USDA API Connection Settings",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (showKeyConfig) "▲" else "▼",
                        color = DimGray,
                        fontSize = 10.sp
                    )
                }

                if (showKeyConfig) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isArabic) 
                            "افتراضياً نستخدم DEMO_KEY الخاص بالبحث العام ليكون مجاني ولا ينتهي. يمكنك كتابة مفتاحك الخاص للحصول على سرعة وصول أعلى." 
                            else "By default, the open DEMO_KEY is injected. Enter a custom USDA FDC key below for dedicated high-frequency access.",
                        color = DimGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUsdaApiKey,
                        onValueChange = { customUsdaApiKey = it },
                        label = { Text(if (isArabic) "مفتاح USDA API Key" else "Custom USDA API Key") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonYellow,
                            unfocusedBorderColor = GlassBorder,
                            focusedLabelColor = NeonYellow,
                            unfocusedLabelColor = DimGray
                        ),
                        singleLine = true,
                        placeholder = { Text("DEMO_KEY", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TAB 0 CONTENT: ACTIVE CAMERA / ALBUM ANALYZER ---
        if (activeTab == 0) {
            Text(
                text = if (isArabic) "فحص فوري وتحليل المكونات بالذكاء الاصطناعي:" else "INSTANT IMAGE ANALYZER COMPONENT:",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Upload selection card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            galleryLauncher.launch("image/*")
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = RockGray.copy(0.40f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Upload,
                        contentDescription = "Upload Photo",
                        tint = NeonYellow,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isArabic) "اضغط لاختيار صورة الوجبة" else "Tap to choose a meal image",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isArabic) "يدعم ملفات JPG, PNG من المعرض أو الكاميرا" else "Supports gallery pics & active shots",
                        color = DimGray,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Preview Block
            if (bitmapState != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmapState!!.asImageBitmap(),
                        contentDescription = "Selected food item",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f))))
                    )
                    Text(
                        text = if (isArabic) "صورة الوجبة جاهزة للفحص والاستخراج" else "Meal snapshot successfully loaded",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Trigger button
                Button(
                    onClick = {
                        if (bitmapState != null && !isAnalyzing) {
                            isAnalyzing = true
                            scannedFoodItems = emptyList()
                            scanError = ""
                            activity.triggerVibration(50)
                            
                            coroutineScope.launch {
                                try {
                                    val detectedJsonString = viewModel.aiAssistant.analyzeFoodImage(bitmapState!!, isArabic)
                                    
                                    if (detectedJsonString.isEmpty() || detectedJsonString.contains("Error")) {
                                        scanError = if (isArabic) "تعذر استخراج الأطعمة من الصورة، يرجى المحاولة بصورة أوضح." else "Could not identify foods. Try a clearer image."
                                        isAnalyzing = false
                                        return@launch
                                    }
                                    
                                    val detectedNames = cleanJsonArray(detectedJsonString)
                                    if (detectedNames.isEmpty()) {
                                        scanError = if (isArabic) "لم نتمكن من التعرف على أي وجبات لنتائج USDA." else "No food items extracted for USDA search."
                                        isAnalyzing = false
                                        return@launch
                                    }
                                    
                                    // Make parallel Retrofit queries to USDA API for each item!
                                    val usdaKey = getUsdaKey()
                                    val matchedList = detectedNames.map { name ->
                                        async(Dispatchers.IO) {
                                            try {
                                                val usdaResponse = UsdaRetrofitClient.service.searchFood(
                                                    apiKey = usdaKey,
                                                    query = name,
                                                    pageSize = 3
                                                )
                                                val foods = usdaResponse.foods ?: emptyList()
                                                DetectedFoodItem(
                                                    englishName = name,
                                                    usdaMatches = foods,
                                                    selectedIndex = 0,
                                                    portionGrams = 100,
                                                    isLogged = false
                                                )
                                            } catch (t: Throwable) {
                                                t.printStackTrace()
                                                DetectedFoodItem(
                                                    englishName = name,
                                                    usdaMatches = emptyList(),
                                                    selectedIndex = 0,
                                                    portionGrams = 100,
                                                    isLogged = false
                                                )
                                            }
                                        }
                                    }.awaitAll()
                                    
                                    scannedFoodItems = matchedList
                                    if (matchedList.isEmpty() || matchedList.all { it.usdaMatches.isEmpty() }) {
                                        scanError = if (isArabic) "اكتمل تحديد الأطعمة لكن تعذر مطابقتها في قاعدة بيانات الغذاء USDA." else "Ingredients identified but no matches found in USDA FDC database."
                                    } else {
                                        activity.playSuccessBeep()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    scanError = "Error: ${e.message}"
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = CyberCharcoal, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "جاري المسح العضلي والمطابقة..." else "SCROLLING USDA DATABASES...",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(PhosphorIcons.Check, contentDescription = null, tint = CyberCharcoal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "بدء فحص ومطابقة USDA للمكونات" else "EXTRACT & MATCH VIA USDA FDC",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error display
            if (scanError.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B181A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Red.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(text = scanError, color = Color(0xFFFF8A8A), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Scanned results list cards
            if (scannedFoodItems.isNotEmpty()) {
                Text(
                    text = if (isArabic) "نتائج استخراج الصورة ومطابقة الأغذية المعتمدة:" else "DETECTED FOOD ENTRIES & CALIBRATIONS:",
                    color = NeonYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                scannedFoodItems.forEachIndexed { itemIndex, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = RockGray.copy(0.7f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Row A: Top layout with Detected Label
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(NeonYellow, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.englishName.uppercase(),
                                        color = PureWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                if (item.isLogged) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(PhosphorIcons.Check, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isArabic) "مسجلة" else "LOGGED", color = NeonYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (item.usdaMatches.isEmpty()) {
                                Text(
                                    text = if (isArabic) 
                                        "لم نجد مطابقة دقيقة لهذا العنصر في USDA. يمكنك البحث يدوياً في التبويب الآخر." 
                                        else "No precise matches in USDA database for this item. Try manual search.",
                                    color = DimGray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            } else {
                                val selectedFood = item.usdaMatches[item.selectedIndex]
                                
                                // Show selected USDA entry name
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "USDA FDC Match:",
                                        color = NeonYellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedFood.description,
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "FDC ID: ${selectedFood.fdcId} • Category: ${selectedFood.dataType ?: "Unknown"}",
                                        color = DimGray,
                                        fontSize = 9.sp
                                    )
                                }

                                // Alternative matches indicator
                                if (item.usdaMatches.size > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isArabic) "مطابقات بديلة:" else "Alternative Match Options:",
                                            color = DimGray,
                                            fontSize = 9.sp
                                        )
                                        item.usdaMatches.forEachIndexed { matchIdx, altMatch ->
                                            if (matchIdx != item.selectedIndex) {
                                                Box(
                                                    modifier = Modifier
                                                        .border(0.5.dp, GlassBorder, RoundedCornerShape(4.dp))
                                                        .background(if (item.selectedIndex == matchIdx) NeonYellow else Color.Transparent)
                                                        .clickable {
                                                            val updatedList = scannedFoodItems.toMutableList()
                                                            updatedList[itemIndex] = item.copy(selectedIndex = matchIdx)
                                                            scannedFoodItems = updatedList
                                                            activity.triggerVibration(25)
                                                        }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "#${matchIdx + 1}",
                                                        color = if (item.selectedIndex == matchIdx) CyberCharcoal else PureWhite,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Weight slider or counter
                                Text(
                                    text = if (isArabic) "تعديل وزن الحصة المستهلكة بالجرامات:" else "CUSTOM PORTION WEIGHT IN GRAMS:",
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val nextGrams = maxOf(10, item.portionGrams - 20)
                                                val updatedList = scannedFoodItems.toMutableList()
                                                updatedList[itemIndex] = item.copy(portionGrams = nextGrams)
                                                scannedFoodItems = updatedList
                                                activity.triggerVibration(20)
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color.White.copy(0.06f), CircleShape)
                                        ) {
                                            Icon(PhosphorIcons.Remove, contentDescription = null, tint = PureWhite, modifier = Modifier.size(10.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${item.portionGrams}g",
                                            color = NeonYellow,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        IconButton(
                                            onClick = {
                                                val nextGrams = minOf(1500, item.portionGrams + 20)
                                                val updatedList = scannedFoodItems.toMutableList()
                                                updatedList[itemIndex] = item.copy(portionGrams = nextGrams)
                                                scannedFoodItems = updatedList
                                                activity.triggerVibration(20)
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(Color.White.copy(0.06f), CircleShape)
                                        ) {
                                            Icon(PhosphorIcons.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(10.dp))
                                        }
                                    }

                                    // Preconfigured pill selectors
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(50, 100, 150, 200).forEach { weight ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (item.portionGrams == weight) NeonYellow else RockGray)
                                                    .clickable {
                                                        val updatedList = scannedFoodItems.toMutableList()
                                                        updatedList[itemIndex] = item.copy(portionGrams = weight)
                                                        scannedFoodItems = updatedList
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${weight}g",
                                                    color = if (item.portionGrams == weight) CyberCharcoal else PureWhite,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Macro calculation values (Portion scaled)
                                val scale = item.portionGrams / 100.0
                                val activeCals = selectedFood.getCalories() * scale
                                val activeProt = selectedFood.getProtein() * scale
                                val activeCarbs = selectedFood.getCarbs() * scale
                                val activeFats = selectedFood.getFats() * scale

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(0.03f), RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${activeCals.toInt()} kcal", color = NeonYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isArabic) "السعرات" else "Cals", color = DimGray, fontSize = 8.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${String.format("%.1f", activeProt)}g", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isArabic) "بروتين" else "P", color = DimGray, fontSize = 8.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${String.format("%.1f", activeCarbs)}g", color = Color(0xFFFF9100), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isArabic) "كارب" else "C", color = DimGray, fontSize = 8.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${String.format("%.1f", activeFats)}g", color = Color(0xFFE040FB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isArabic) "دهون" else "F", color = DimGray, fontSize = 8.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Add button
                                Button(
                                    onClick = {
                                        logFoodToMacros(
                                            desc = selectedFood.description,
                                            calsPer100 = selectedFood.getCalories(),
                                            proteinsPer100 = selectedFood.getProtein(),
                                            carbsPer100 = selectedFood.getCarbs(),
                                            fatsPer100 = selectedFood.getFats(),
                                            portionGrams = item.portionGrams
                                        )
                                        val updatedList = scannedFoodItems.toMutableList()
                                        updatedList[itemIndex] = item.copy(isLogged = true)
                                        scannedFoodItems = updatedList
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.isLogged) Color.DarkGray else NeonYellow,
                                        contentColor = CyberCharcoal
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (item.isLogged) { if (isArabic) "تم الإضافة للماكروز ✓" else "ADDED TO DAILY TRACKER ✓" }
                                            else { if (isArabic) "إضافة الوجبة للماكروز اليومية" else "ADD PORTION TO MACROS" },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = if (item.isLogged) PureWhite else CyberCharcoal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- TAB 1 CONTENT: QUICK MANUAL USDA SEARCH BAR ---
        if (activeTab == 1) {
            Text(
                text = if (isArabic) "البحث المباشر الدقيق في قاعدة USDA بالإنجليزية:" else "VERIFIED ENGLISH USDA DIRECT QUERY SYSTEM:",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Search input field Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apple, chicken, tuna...", color = Color.Gray, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonYellow,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = NeonYellow,
                        unfocusedLabelColor = DimGray
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .background(GlassWhite, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (searchQuery.trim().isNotEmpty() && !isSearching) {
                            isSearching = true
                            searchResults = emptyList()
                            searchError = ""
                            selectedSearchItem = null
                            activity.triggerVibration(40)
                            
                            coroutineScope.launch {
                                try {
                                    val usdaKey = getUsdaKey()
                                    val response = UsdaRetrofitClient.service.searchFood(
                                        apiKey = usdaKey,
                                        query = searchQuery,
                                        pageSize = 15
                                    )
                                    val foods = response.foods ?: emptyList()
                                    searchResults = foods
                                    if (foods.isEmpty()) {
                                        searchError = if (isArabic) "لم نجد نتائج مطابقة لبحثك في قاعدة الأغذية." else "No matching foods found."
                                    } else {
                                        activity.playBeep()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    searchError = "Error: ${e.message}"
                                } finally {
                                    isSearching = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(color = CyberCharcoal, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isArabic) "بحث" else "SEARCH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Translation advisory banner
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(NeonYellow, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) 
                        "اكتب الكلمات باللغة الإنجليزية للوصول المباشر لقاعدة الأغذية الأمريكية (مثال: eggs, milk, potato)" 
                        else "For highly precise lookup, write search queries in English.",
                    color = DimGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Error / Success states
            if (searchError.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2B181A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Red.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(text = searchError, color = Color(0xFFFF8A8A), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Results details calibration Card (if item selected)
            if (selectedSearchItem != null) {
                val activeFood = selectedSearchItem!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = RockGray.copy(0.9f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CALIBRATE PORTION VALUE",
                                color = NeonYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = { selectedSearchItem = null }, modifier = Modifier.size(18.dp)) {
                                Icon(PhosphorIcons.Remove, contentDescription = "Close", tint = DimGray, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = activeFood.description,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FDC Identifier: ${activeFood.fdcId} • Serving standard 100g base nutritional reference",
                            color = DimGray,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selected weight sliders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        searchItemGrams = maxOf(10, searchItemGrams - 20)
                                        activity.triggerVibration(20)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(0.06f), CircleShape)
                                ) {
                                    Icon(PhosphorIcons.Remove, contentDescription = null, tint = PureWhite, modifier = Modifier.size(10.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${searchItemGrams}g",
                                    color = NeonYellow,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        searchItemGrams = minOf(1500, searchItemGrams + 20)
                                        activity.triggerVibration(20)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(0.06f), CircleShape)
                                ) {
                                    Icon(PhosphorIcons.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(10.dp))
                                }
                            }

                            // Quick preset weight buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(50, 100, 150, 200).forEach { grams ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (searchItemGrams == grams) NeonYellow else RockGray)
                                            .clickable { searchItemGrams = grams }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${grams}g",
                                            color = if (searchItemGrams == grams) CyberCharcoal else PureWhite,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Calculations layout
                        val multiplier = searchItemGrams / 100.0
                        val calVal = activeFood.getCalories() * multiplier
                        val protVal = activeFood.getProtein() * multiplier
                        val carbVal = activeFood.getCarbs() * multiplier
                        val fatVal = activeFood.getFats() * multiplier

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(0.03f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${calVal.toInt()} kcal", color = NeonYellow, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Text(if (isArabic) "السعرات" else "ENERGY", color = DimGray, fontSize = 9.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${String.format("%.1f", protVal)}g", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (isArabic) "بروتين" else "PROTEIN", color = DimGray, fontSize = 9.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${String.format("%.1f", carbVal)}g", color = Color(0xFFFF9100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (isArabic) "كربوهيدرات" else "CARBS", color = DimGray, fontSize = 9.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${String.format("%.1f", fatVal)}g", color = Color(0xFFE040FB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(if (isArabic) "دهون هرمونية" else "FATS", color = DimGray, fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Direct log commit button
                        Button(
                            onClick = {
                                logFoodToMacros(
                                    desc = activeFood.description,
                                    calsPer100 = activeFood.getCalories(),
                                    proteinsPer100 = activeFood.getProtein(),
                                    carbsPer100 = activeFood.getCarbs(),
                                    fatsPer100 = activeFood.getFats(),
                                    portionGrams = searchItemGrams
                                )
                                selectedSearchItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isArabic) "تأكيد وتسجيل الحصة اليومية للماكروز" else "CONFIRM & CO-LOG PORTION",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // List of search outcomes
            if (searchResults.isNotEmpty()) {
                Text(
                    text = if (isArabic) "سجل المطابقات المستخرجة من USDA:" else "CORRESPONDING SEARCH OUTCOMES:",
                    color = NeonYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                searchResults.forEach { foodItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                if (selectedSearchItem?.fdcId == foodItem.fdcId) NeonYellow.copy(0.4f) else GlassBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedSearchItem = foodItem
                                searchItemGrams = 100
                                activity.triggerVibration(30)
                            },
                        colors = CardDefaults.cardColors(containerColor = RockGray.copy(0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = foodItem.description,
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "FDC ID: ${foodItem.fdcId} • Category: ${foodItem.dataType ?: "Survey"}",
                                    color = DimGray,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${foodItem.getCalories().toInt()} kcal",
                                    color = NeonYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "P: ${foodItem.getProtein().toInt()}g",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SECTION 4: REAL-TIME LIVED PROGRESS HUD SUMMARY ---
        Text(
            text = if (isArabic) "موجز الماكروز الكلية المسجلة لليوم:" else "LIVE COMPACT ATHLETIC NUTRITION INDEX:",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        val currentCals = macrosState.calories
        val currentTargets = macrosState.getTargets()
        val maxCalories = currentTargets.calories

        if (macrosState.weight <= 0f) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, SportyRed.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "ملفك الرياضي غير مهيأ بعد ⚠️" else "Athletic Profile: Unconfigured ⚠️",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isArabic) {
                            "لتفعيل مؤشرات السعرات والماكروز لخطط التمرين، اضغط بالأسفل لتهيئة ملف الميكروز بالذكاء الاصطناعي."
                        } else {
                            "To unlock live athletic indicators and tailored sports formulas, tap below to compute and activate your sports metrics!"
                        },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                    Button(
                        onClick = { 
                            viewModel.requestTabChange(DashboardTab.SETTINGS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SportyRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isArabic) "اضبط ملف الرياضي والماكروز الآن ⚙️" else "SETUP SMART MACROS NOW ⚙️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(2.dp, GlassBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular graphic cals index
                    val ratio = (currentCals.toFloat() / maxCalories.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(0.05f),
                                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = NeonYellow,
                                    startAngle = -90f,
                                    sweepAngle = 360f * ratio,
                                    useCenter = false,
                                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currentCals",
                                color = NeonYellow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/$maxCalories",
                                color = DimGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "سُعرة" else "KCAL",
                                color = PureWhite,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Numerical micro metrics
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "إجمالي القيم المسجلة حالياً:" else "CURRENT DAILY AMINO PROFILE:",
                            color = PureWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Protein", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(text = "${macrosState.protein}g / ${currentTargets.protein}g", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "Carbs", color = Color(0xFFFF9100), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(text = "${macrosState.carbs}g / ${currentTargets.carbs}g", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "Fats", color = Color(0xFFE040FB), fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Text(text = "${macrosState.fats}g / ${currentTargets.fats}g", color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// Model classes / parser inside the scope
data class DetectedFoodItem(
    val englishName: String,
    val usdaMatches: List<UsdaFoodItem>,
    val selectedIndex: Int = 0,
    val portionGrams: Int = 100,
    val isLogged: Boolean = false
)

fun cleanJsonArray(rawText: String): List<String> {
    try {
        var clean = rawText.trim()
        if (clean.contains("```")) {
            val startIndex = clean.indexOf("```json")
            if (startIndex != -1) {
                val endIndex = clean.indexOf("```", startIndex + 7)
                if (endIndex != -1) {
                    clean = clean.substring(startIndex + 7, endIndex).trim()
                }
            } else {
                val genericStart = clean.indexOf("```")
                val genericEnd = clean.indexOf("```", genericStart + 3)
                if (genericStart != -1 && genericEnd != -1) {
                    clean = clean.substring(genericStart + 3, genericEnd).trim()
                }
            }
        }
        
        clean = clean.trim()
        if (clean.startsWith("[") && clean.endsWith("]")) {
            return Json.decodeFromString<List<String>>(clean)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    val result = mutableListOf<String>()
    val cleanedText = rawText.replace("[", "").replace("]", "").replace("\"", "").replace("'", "")
    val parts = cleanedText.split(",")
    for (part in parts) {
        val trimmed = part.trim()
        if (trimmed.isNotEmpty() && trimmed.length > 2 && !trimmed.contains("{") && !trimmed.contains("}")) {
            result.add(trimmed)
        }
    }
    return result
}
