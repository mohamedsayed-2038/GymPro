package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.launch

enum class DashboardTab {
    TIMER,
    WORKOUT,
    MEALS,
    SETTINGS
}

@Composable
fun MainDashboardScreen(
    isArabic: Boolean,
    onLanguageToggle: () -> Unit,
    onImageAnalyzeClick: () -> Unit,
    onGlassShowcaseClick: () -> Unit,
    timerContent: @Composable () -> Unit,
    workoutContent: @Composable () -> Unit,
    mealsContent: @Composable () -> Unit
) {
    val viewModel: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val requestedTab by viewModel.targetTab.collectAsState()
    var currentTab by remember { mutableStateOf(DashboardTab.TIMER) }

    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            currentTab = it
            viewModel.clearTabChangeRequest()
        }
    }

    val hazeState = remember { HazeState() }
    
    // Voice Coach Overlay State
    var showVoiceOverlay by remember { mutableStateOf(false) }
    
    // Bind global service states to trigger continuous animations
    val isServiceActive by VoiceCoachService.isServiceActive.collectAsState()
    val isLiveConnected by VoiceCoachService.isLiveConnected.collectAsState()
    val isSpeaking by VoiceCoachService.isSpeaking.collectAsState()
    val amplitudes by VoiceCoachService.amplitudes.collectAsState()
    val wakeWordSpotted by VoiceCoachService.wakeWordSpotted.collectAsState()

    val context = LocalContext.current

    val permissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[android.Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        if (recordGranted && notificationsGranted) {
            VoiceCoachService.startService(context)
        } else {
            android.widget.Toast.makeText(
                context,
                if (isArabic) "يرجى منح الأذونات المطلوبة لتشغيل المدرب الصوتي" else "Please grant the required permissions to run the voice coach",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16161A)),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()) {
            
            // Core Tab Content Pane
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
            ) {
                when (currentTab) {
                    DashboardTab.TIMER -> timerContent()
                    DashboardTab.WORKOUT -> workoutContent()
                    DashboardTab.MEALS -> mealsContent()
                    DashboardTab.SETTINGS -> SettingsScreen(
                        isArabic = isArabic,
                        onLanguageToggle = onLanguageToggle
                    )
                }
            }

            // High-contrast clean App Header (Removed Settings and Voice Coach triggering)
            AppHeader(
                isArabic = isArabic,
                onImageAnalyzeClick = onImageAnalyzeClick,
                onGlassShowcaseClick = onGlassShowcaseClick,
                hazeState = hazeState
            )

            // Dynamic bottom nav bar consisting of 5 items
            FloatingBottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                isArabic = isArabic,
                isServiceActive = isServiceActive,
                onClickVoice = {
                    if (isServiceActive) {
                        VoiceCoachService.stopService(context)
                    } else {
                        val hasRecord = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        val hasNotifications = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (hasRecord && hasNotifications) {
                            VoiceCoachService.startService(context)
                        } else {
                            val list = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                list.add(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionsLauncher.launch(list.toTypedArray())
                        }
                    }
                },
                onSwipeUpVoice = {
                    showVoiceOverlay = true
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState
            )

            // Cinematic Glassmorphic Bottom Drawer Panel (Slide-up Overlay)
            AnimatedVisibility(
                visible = showVoiceOverlay,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = EaseOutQuart)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400, easing = EaseInCubic)
                ) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .zIndex(100f)
            ) {
                VoiceCoachBottomSheet(
                    isArabic = isArabic,
                    isServiceActive = isServiceActive,
                    isLiveConnected = isLiveConnected,
                    isSpeaking = isSpeaking,
                    amplitudes = amplitudes,
                    wakeWordSpotted = wakeWordSpotted,
                    onStopService = {
                        VoiceCoachService.stopService(context)
                    },
                    onDismiss = {
                        showVoiceOverlay = false
                    }
                )
            }
        }
    }
}

@Composable
fun AppHeader(
    isArabic: Boolean,
    onImageAnalyzeClick: () -> Unit,
    onGlassShowcaseClick: () -> Unit,
    hazeState: HazeState
) {
    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(56.dp)
            .hazeChild(
                state = hazeState,
                shape = SquircleShape(0.45f),
                style = HazeStyle(blurRadius = 24.dp, tint = Color(0xFF1E1E1E).copy(alpha = 0.45f))
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), SquircleShape(0.45f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Identity Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo_transparent),
                contentDescription = "Logo",
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = Loc.getString("app_title", isArabic).uppercase(),
                color = SportyRed,
                fontSize = 18.sp, 
                fontWeight = FontWeight.Black,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                letterSpacing = 0.5.sp
            )
        }

        // Action Buttons Row (Glass Showcase and Camera AI Scanner)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Glass physical simulation engine showcase trigger
            IconButton(
                onClick = onGlassShowcaseClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Layers,
                    contentDescription = "iOS Physical Glass Lab",
                    tint = Color(0xFFCCFF00), // High contrast neon yellow
                    modifier = Modifier.size(22.dp)
                )
            }

            // Camera AI Scan Trigger ONLY
            IconButton(
                onClick = onImageAnalyzeClick,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Camera,
                    contentDescription = "AI Scanner",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingBottomNav(
    currentTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    isArabic: Boolean,
    isServiceActive: Boolean,
    onClickVoice: () -> Unit,
    onSwipeUpVoice: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState
) {
    val glassBgColor = Color(0x7A121212) 
    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0x33FFFFFF), // Highlight
            Color(0x13FFFFFF),
            Color(0x35CCFF00)  // Yellow Neon Accent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Backdrop elevate plate
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .hazeChild(
                    state = hazeState,
                    shape = SquircleShape(0.48f),
                    style = HazeStyle(blurRadius = 30.dp, tint = glassBgColor)
                )
                .border(1.dp, glassBorderBrush, SquircleShape(0.48f))
        )

        // Balanced 5-element navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: TIMER
            NavItem(
                icon = PhosphorIcons.Timer,
                label = if (isArabic) "المؤقت" else "Timer",
                selected = currentTab == DashboardTab.TIMER,
                onClick = { onTabSelected(DashboardTab.TIMER) }
            )

            // Tab 2: WORKOUT
            NavItem(
                icon = PhosphorIcons.Dumbbell,
                label = if (isArabic) "التمارين" else "Workout",
                selected = currentTab == DashboardTab.WORKOUT,
                onClick = { onTabSelected(DashboardTab.WORKOUT) }
            )

            // Center: Circular Cinematic Voice Coach Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = (-14).dp),
                contentAlignment = Alignment.Center
            ) {
                // Fluent Soundwaves animation rings
                if (isServiceActive) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
                    val wave1Scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "wave1"
                    )
                    val wave1Alpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "wave1a"
                    )

                    val wave2Scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, delayMillis = 450, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "wave2"
                    )
                    val wave2Alpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, delayMillis = 450, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "wave2a"
                    )

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                scaleX = wave1Scale
                                scaleY = wave1Scale
                                alpha = wave1Alpha
                            }
                            .background(SportyRed.copy(alpha = 0.5f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer {
                                scaleX = wave2Scale
                                scaleY = wave2Scale
                                alpha = wave2Alpha
                            }
                            .background(SportyRed.copy(alpha = 0.4f), CircleShape)
                    )
                }

                // Inner core solid filled button with fluid glow outline
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(
                            elevation = if (isServiceActive) 16.dp else 4.dp,
                            shape = CircleShape,
                            ambientColor = SportyRed,
                            spotColor = SportyRed
                        )
                        .background(SportyRed, CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.y < -12f) {
                                        onSwipeUpVoice()
                                    }
                                }
                            )
                        }
                        .clickable { onClickVoice() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Microphone,
                        contentDescription = "المدرب الصوتي",
                        tint = RockGray, // Dark fill icon on solid solid yellow backdrop
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Tab 3: MEALS
            NavItem(
                icon = PhosphorIcons.ForkKnife,
                label = if (isArabic) "الوجبات" else "Meals",
                selected = currentTab == DashboardTab.MEALS,
                onClick = { onTabSelected(DashboardTab.MEALS) }
            )

            // Tab 4: SETTINGS (Replaces old header about profile buttons)
            NavItem(
                icon = PhosphorIcons.Info,
                label = if (isArabic) "الإعدادات" else "Settings",
                selected = currentTab == DashboardTab.SETTINGS,
                onClick = { onTabSelected(DashboardTab.SETTINGS) }
            )
        }
    }
}

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = SportyRed
    val inactiveColor = Color(0x9EFFFFFF)
    val color = if (selected) activeColor else inactiveColor

    Column(
        modifier = Modifier
            .width(64.dp)
            .height(54.dp)
            .clip(SquircleShape(0.28f))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else Modifier.let { FontWeight.Normal }
        )
    }
}

/**
 * Highly cinematic Glassmorphic Swipe Up panel overlay showing raw wave amplitude audio visualizers
 * and live coaching status feedback instantly in perfect Arabic.
 */
@Composable
fun VoiceCoachBottomSheet(
    isArabic: Boolean,
    isServiceActive: Boolean,
    isLiveConnected: Boolean,
    isSpeaking: Boolean,
    amplitudes: List<Float>,
    wakeWordSpotted: Boolean,
    onStopService: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_waves")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 1600 else 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_velocity"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .iosSquircleCard(
                cornerSizeRatio = 0.28f,
                baseOpacity = 0.26f,
                saturationScale = 1.80f
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Drag indicator handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), SquircleShape(0.48f))
                    .clickable { onDismiss() }
            )

            Text(
                text = if (isArabic) "المدرب الصوتي المباشر" else "Live Athletic Coach",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            // Dynamic Live feedback status
            val statusText = when {
                wakeWordSpotted -> if (isArabic) "تم رصد الصوت! تفضل بالسؤال..." else "Voice spotted... Speak now!"
                isSpeaking -> if (isArabic) "المدرب يتحدث بملاحظات الأداء الآن..." else "Coach is outputting live voice feedback..."
                isLiveConnected -> if (isArabic) "متصل ومستعد للتوجيه!" else "Coach connected & listening..."
                isServiceActive -> if (isArabic) "جاري الربط مع قنوات البث..." else "Connecting core live streams..."
                else -> if (isArabic) "المدرب الصوتي غير نشط" else "Voice Coach Inactive"
            }

            val statusColor = when {
                wakeWordSpotted -> Color(0xFFFFEB3B) // Yellow peak
                isSpeaking -> Color(0xFF00E676) // Green talk
                isLiveConnected -> Color(0xFF00E5FF) // Ice cyan
                isServiceActive -> Color(0xFFFFC107) // Amber setup
                else -> Color.Gray
            }

            Text(
                text = statusText,
                color = statusColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            // Audio Waveform Visualization Canvas (Rendered directly)
            Box(
                modifier = Modifier
                    .fillModifier()
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midY = size.height / 2
                    val width = size.width

                    val path = Path()
                    path.moveTo(0f, midY)

                    val pointsCount = 120
                    val step = width / pointsCount

                    for (i in 0..pointsCount) {
                        val x = i * step
                        val progress = i.toFloat() / pointsCount
                        val edgeDampening = kotlin.math.sin(progress * Math.PI.toFloat())
                        var yOffset = 0f

                        if (isLiveConnected) {
                            val baseAmpIdx = (progress * (amplitudes.size - 1)).toInt()
                                .coerceIn(0, (amplitudes.size - 1).coerceAtLeast(0))
                            val amp = if (amplitudes.isNotEmpty()) amplitudes[baseAmpIdx] * 70f else 10f
                            yOffset = kotlin.math.sin(phase + i * 0.16f) * amp * edgeDampening
                        } else if (isServiceActive) {
                            // Subtle connecting state pulse
                            yOffset = kotlin.math.sin(phase * 1.5f + i * 0.12f) * 3f * edgeDampening
                        } else {
                            // Inactive is completely flat
                            yOffset = 0f
                        }

                        path.lineTo(x, midY + yOffset)
                    }

                    drawPath(
                        path = path,
                        color = when {
                            isLiveConnected -> Color(0xFF00E5FF)
                            isSpeaking -> Color(0xFF00E676)
                            else -> Color(0xFF4A4A5A)
                        },
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = RockGray),
                    shape = SquircleShape(0.28f),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isArabic) "إغلاق الواجهة" else "Close Dashboard",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Primary stop toggle button
                if (isServiceActive) {
                    Button(
                        onClick = {
                            onStopService()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D)),
                        shape = SquircleShape(0.28f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isArabic) "إيقاف المدرب" else "Stop Coach",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Inline helper for cleanly mapping weight parameters without issues
fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()
