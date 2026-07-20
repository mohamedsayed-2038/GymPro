package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType

// Simple system utility to check active network connectivity
fun isDeviceOnline(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            true
        }
    } catch (e: Exception) {
        // Fallback to true so we don't block the user if check fails
        true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartChefScreen(
    isArabic: Boolean,
    onBack: () -> Unit,
    viewModel: SmartChefViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val chatMessages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val macroStats by viewModel.macroProgress.collectAsState()

    var inputIngredients by remember { mutableStateOf("") }
    
    // Check internet connectivity dynamically
    var isOnline by remember { mutableStateOf(isDeviceOnline(context)) }

    // Periodically sync network state
    LaunchedEffect(Unit) {
        while (true) {
            isOnline = isDeviceOnline(context)
            kotlinx.coroutines.delay(3000)
        }
    }

    // Auto-scroll to bottom of conversation whenever new messages are appended
    LaunchedEffect(chatMessages.size, isGenerating) {
        if (chatMessages.isNotEmpty()) {
            val offset = if (isGenerating) 1 else 0
            listState.animateScrollToItem(chatMessages.size - 1 + offset)
        }
    }

    val currentTargets = macroStats.getTargets()
    val targetCalories = currentTargets.calories
    val targetProtein = currentTargets.protein
    val remCal = maxOf(0, targetCalories - macroStats.calories)
    val remProt = maxOf(0, targetProtein - macroStats.protein)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121215)) // Deep luxury slate black
            .testTag("smart_chef_screen")
    ) {
        // Aesthetic Ambient Glow Sphere Background
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .size(350.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonYellow.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- 1. PREMIUM GLASSMORPHIC HEADER (COMPACT & FULLY ROUNDED WITH SOFT SHADOW) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .zIndex(1f)
                    .iosSquircleCard(
                        cornerSizeRatio = 0.45f,
                        baseOpacity = 0.22f,
                        saturationScale = 1.80f
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonYellow.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.ForkKnife,
                            contentDescription = null,
                            tint = NeonYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isArabic) "شيف برو 🍳" else "Chef Pro 🍳",
                            color = NeonYellow,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isArabic) "مستشار التغذية والوصفات الرياضية" else "AI Sports Chef & Nutritionist",
                            color = Color.LightGray.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Explicit exit / close button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                        .testTag("smart_chef_back_btn")
                ) {
                    Icon(
                        imageVector = PhosphorIcons.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- 2. LIVE METRIC HUD SYNC BAR (COLLAPSIBLE ACCORDION) ---
            var isHudExpanded by remember { mutableStateOf(true) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .zIndex(1f)
                    .iosSquircleCard(
                        cornerSizeRatio = 0.28f,
                        baseOpacity = 0.18f,
                        saturationScale = 1.80f
                    )
                    .animateContentSize()
                    .clickable { isHudExpanded = !isHudExpanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isOnline) Color(0xFF00E676) else Color(0xFFFF5252), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "مؤشرات التغذية الحية" else "Live Athlete Nutrition Metrics",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = if (isHudExpanded) PhosphorIcons.CaretUp else PhosphorIcons.CaretDown,
                        contentDescription = "Toggle HUD",
                        tint = NeonYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isHudExpanded,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "المتبقي للاحتياج اليومي:" else "Remaining deficits today:",
                                color = Color.LightGray.copy(alpha = 0.72f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "🔥 $remCal kcal",
                                    color = NeonYellow,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "💪 $remProt g Protein",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. CHAT CONVERSATION VIEWPORT ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isArabic) "أهلاً بك يا بطل! أنا شيف جيم برو 🍳" else "Welcome! I'm GymPro Chef 🍳",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isArabic) 
                                "اكتب لي المكونات المتوفرة عندك بالمنزل (مثل: عندي بيض، تشوفان، وحليب) أو اسألني عن أي تساؤل رياضي أو نصيحة تغذية! وسأقوم بمساعدتك فوراً وحساب السعرات والجرامات المناسبة لاحتياجك اليومي بدقة عالية."
                                else "Tell me what ingredients you have in your fridge or kitchen, or ask me any dynamic sports nutrition questions! I'll instantly design high-protein gourmet gym recipes aligned with your live macro deficits, or converse with you anytime.",
                            color = Color.LightGray.copy(alpha = 0.82f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val latestAiMessageId = remember(chatMessages) { chatMessages.lastOrNull { !it.isUser }?.id }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatMessages) { message ->
                            ChatBubble(
                                message = message,
                                isLatestAiMessage = message.id == latestAiMessageId,
                                onAddMeal = { finalMacros ->
                                    viewModel.updateMacrosDirectly(finalMacros.calories, finalMacros.protein, finalMacros.carbs, finalMacros.fats)
                                    val mainActivity = context as? MainActivity
                                    mainActivity?.triggerVibration(100)
                                    mainActivity?.playSuccessBeep()
                                    mainActivity?.speak(
                                        if (isArabic) "رائع! تم تسجيل السعرات والميكروز لهذه الوجبة بنجاح في مخطط يومك." 
                                        else "Done! Saved macro stats directly to your workout planner dashboard.",
                                        isArabic
                                    )
                                },
                                context = context
                            )
                        }
                        if (isGenerating) {
                            item {
                                ThinkingBubble()
                            }
                        }
                    }
                }
            }

            // --- 3. OFFLINE EMERGENCY BANNER & INPUT INTERACTOR ---
            AnimatedVisibility(
                visible = !isOnline,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(1f)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.8f),
                            spotColor = Color.Black
                        )
                        .background(Color(0xFF2D1619))
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (isArabic) 
                            "عذراً يا بطل، الشيف الذكي يحتاج لاتصال نشط بالإنترنت ليقيس مؤشراتك الحية ويصنع وصفاتك المخصصة. تفقد اتصالك وأعد المحاولة! 🌐" 
                            else "Sorry champ, the Smart Chef needs an active internet connection to evaluate your live physiological metrics and generate target recipes. Check your connection and try again! 🌐",
                        color = Color(0xFFFF8A8A),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Chat entry field (FULLY ROUNDED & SHADOWED WITH DYNAMIC EXPANSION)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp, top = 8.dp)
                    .zIndex(1f)
                    .recessedGlassInput(
                        cornerSizeRatio = 0.45f,
                        baseOpacity = 0.08f
                    )
                    .animateContentSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputIngredients,
                    onValueChange = { if (isOnline) inputIngredients = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "اكتب المكونات المتاحة..." else "Enter available ingredients...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chef_text_input"),
                    singleLine = false,
                    maxLines = 3,
                    enabled = isOnline && !isGenerating,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputIngredients.isNotBlank() && isOnline && !isGenerating) {
                                viewModel.sendMessage(inputIngredients, isArabic)
                                inputIngredients = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Modern Send button with solid Neon accent colors
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOnline && inputIngredients.isNotBlank() && !isGenerating) NeonYellow else Color.White.copy(
                                alpha = 0.05f
                            )
                        )
                        .clickable(
                            enabled = isOnline && inputIngredients.isNotBlank() && !isGenerating,
                            onClick = {
                                viewModel.sendMessage(inputIngredients, isArabic)
                                inputIngredients = ""
                                keyboardController?.hide()
                            }
                        )
                        .testTag("chef_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Flame,
                        contentDescription = "Send",
                        tint = if (isOnline && inputIngredients.isNotBlank() && !isGenerating) CyberCharcoal else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingBubble() {
    val thoughts = remember {
        listOf(
            "Analyzing body macros & static deficits...",
            "Inspecting athletic targets & active weight metrics...",
            "Filtering dynamic protein and energy counts...",
            "Calculating precise ingredients in grams...",
            "Formulating performance meal guide instructions..."
        )
    }
    var currentIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            currentIndex = (currentIndex + 1) % thoughts.size
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .animateContentSize(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(NeonYellow.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "chef_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "chef_rotate_anim"
            )
            Icon(
                imageVector = PhosphorIcons.Flame,
                contentDescription = null,
                tint = NeonYellow,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = thoughts[currentIndex],
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (i in 0..2) {
                    val delayMillis = i * 200
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot_trans")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delayMillis, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha_anim"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(NeonYellow.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isLatestAiMessage: Boolean,
    onAddMeal: (MacroValues) -> Unit,
    context: Context
) {
    if (message.isUser) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .iosSquircleCard(
                        cornerSizeRatio = 0.18f,
                        baseOpacity = 0.22f,
                        saturationScale = 1.80f
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                val isArabicUser = isArabicText(message.text)
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = if (isArabicUser) TextAlign.End else TextAlign.Start,
                    style = TextStyle(textDirection = if (isArabicUser) TextDirection.Rtl else TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(NeonYellow.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorIcons.ForkKnife,
                    contentDescription = null,
                    tint = NeonYellow,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarkdownText(text = message.text)

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var copied by remember { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()

                    // Sleek icon-only copy action button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (copied) Color(0xFF1E3A24) else Color.White.copy(alpha = 0.06f))
                            .clickable {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("GymPro Recipe", message.text)
                                    clipboard.setPrimaryClip(clip)
                                    copied = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(2000)
                                        copied = false
                                    }
                                } catch (e: Exception) { }
                            }
                            .testTag("recipe_copy_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (copied) PhosphorIcons.Check else PhosphorIcons.Copy,
                            contentDescription = "Copy Recipe",
                            tint = if (copied) Color(0xFF81C784) else Color.LightGray,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    val parsedMacros = remember(message.text) { parseMacrosFromText(message.text) }
                    val hasMacros = parsedMacros.calories > 0 || parsedMacros.protein > 0
                    var showAddDialog by remember { mutableStateOf(false) }

                    if (hasMacros) {
                        // Sleek icon-only add action button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NeonYellow.copy(alpha = 0.08f))
                                .clickable {
                                    showAddDialog = true
                                }
                                .testTag("recipe_add_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Trophy,
                                contentDescription = "Add to Planner",
                                tint = NeonYellow,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (showAddDialog) {
                        ConfirmMealDialog(
                            isArabic = isArabicText(message.text),
                            initialMacros = parsedMacros,
                            onConfirm = { finalMacros ->
                                onAddMeal(finalMacros)
                                showAddDialog = false
                            },
                            onDismiss = { showAddDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmMealDialog(
    isArabic: Boolean,
    initialMacros: MacroValues,
    onConfirm: (MacroValues) -> Unit,
    onDismiss: () -> Unit
) {
    var caloriesText by remember { mutableStateOf(initialMacros.calories.toString()) }
    var proteinText by remember { mutableStateOf(initialMacros.protein.toString()) }
    var carbsText by remember { mutableStateOf(initialMacros.carbs.toString()) }
    var fatsText by remember { mutableStateOf(initialMacros.fats.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "تأكيد تسجيل ميكروز هذه الوجبة" else "CONFIRM MEAL MACROS",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isArabic) {
                        "قمنا باستخراج السعرات والمغذيات تلقائياً؛ يمكنك مراجعتها وتعديلها قبل إضافتها لمجموعك اليومي المفرغ:"
                    } else {
                        "We extracted the following macros dynamically. You can review and adjust them before saving to your planner goals:"
                    },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 16.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .recessedGlassInput(cornerSizeRatio = 0.16f)
                    ) {
                        OutlinedTextField(
                            value = caloriesText,
                            onValueChange = { caloriesText = it },
                            placeholder = { Text(if (isArabic) "السعرات" else "Kcal", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .recessedGlassInput(cornerSizeRatio = 0.16f)
                    ) {
                        OutlinedTextField(
                            value = proteinText,
                            onValueChange = { proteinText = it },
                            placeholder = { Text(if (isArabic) "البروتين (ج)" else "P(g)", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .recessedGlassInput(cornerSizeRatio = 0.16f)
                    ) {
                        OutlinedTextField(
                            value = carbsText,
                            onValueChange = { carbsText = it },
                            placeholder = { Text(if (isArabic) "الكارب (ج)" else "C(g)", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .recessedGlassInput(cornerSizeRatio = 0.16f)
                    ) {
                        OutlinedTextField(
                            value = fatsText,
                            onValueChange = { fatsText = it },
                            placeholder = { Text(if (isArabic) "الدهون (ج)" else "F(g)", color = Color.Gray, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCal = caloriesText.toIntOrNull() ?: 0
                    val finalProt = proteinText.toIntOrNull() ?: 0
                    val finalCarb = carbsText.toIntOrNull() ?: 0
                    val finalFat = fatsText.toIntOrNull() ?: 0
                    onConfirm(MacroValues(finalCal, finalProt, finalCarb, finalFat))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal)
            ) {
                Text(if (isArabic) "تأكيد ومعايرة" else "Log Meal", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier.iosSquircleCard(
            cornerSizeRatio = 0.28f,
            baseOpacity = 0.24f,
            saturationScale = 1.80f
        )
    )
}

data class MacroValues(val calories: Int, val protein: Int, val carbs: Int, val fats: Int)

fun parseMacrosFromText(text: String): MacroValues {
    val normalizedText = text
        .replace("٠", "0").replace("١", "1").replace("٢", "2").replace("٣", "3").replace("٤", "4")
        .replace("٥", "5").replace("٦", "6").replace("٧", "7").replace("٨", "8").replace("٩", "9")

    val summaryPattern = java.util.regex.Pattern.compile(
        "\\[MACROS_SUMMARY\\]:\\s*Calories:\\s*(\\d+)[^|]*\\|\\s*Protein:\\s*(\\d+)[^|]*\\|\\s*Carbs:\\s*(\\d+)[^|]*\\|\\s*Fats:\\s*(\\d+)",
        java.util.regex.Pattern.CASE_INSENSITIVE
    )
    val summaryMatcher = summaryPattern.matcher(normalizedText)
    if (summaryMatcher.find()) {
        val cal = summaryMatcher.group(1)?.toIntOrNull() ?: 0
        val prot = summaryMatcher.group(2)?.toIntOrNull() ?: 0
        val carb = summaryMatcher.group(3)?.toIntOrNull() ?: 0
        val fat = summaryMatcher.group(4)?.toIntOrNull() ?: 0
        return MacroValues(cal, prot, carb, fat)
    }

    var calories = 0
    var protein = 0
    var carbs = 0
    var fats = 0

    val calPatterns = listOf(
        java.util.regex.Pattern.compile("Calories.*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("السعرات.*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*(?:kcal|calories|cal|سعرة|سعر)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(?:السعرات|سعرات).*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
    )
    for (pattern in calPatterns) {
        val matcher = pattern.matcher(normalizedText)
        if (matcher.find()) {
            calories = matcher.group(1)?.toIntOrNull() ?: 0
            if (calories > 0) break
        }
    }

    val protPatterns = listOf(
        java.util.regex.Pattern.compile("Protein.*?(\\d+)\\s*g", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("البروتين.*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*g\\s*Protein", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(?:البروتين|بروتين).*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*(?:جرام|ج)\\s*بروتين", java.util.regex.Pattern.CASE_INSENSITIVE)
    )
    for (pattern in protPatterns) {
        val matcher = pattern.matcher(normalizedText)
        if (matcher.find()) {
            protein = matcher.group(1)?.toIntOrNull() ?: 0
            if (protein > 0) break
        }
    }

    val carbsPatterns = listOf(
        java.util.regex.Pattern.compile("Carbs.*?(\\d+)\\s*g", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("Carbohydrates.*?(\\d+)\\s*g", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("الكربوهيدرات.*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*g\\s*Carbs", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(?:الكربوهيدرات|كربوهيدرات|كارب).*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*(?:جرام|ج)\\s*(?:كارب|كربوهيدرات)", java.util.regex.Pattern.CASE_INSENSITIVE)
    )
    for (pattern in carbsPatterns) {
        val matcher = pattern.matcher(normalizedText)
        if (matcher.find()) {
            carbs = matcher.group(1)?.toIntOrNull() ?: 0
            if (carbs > 0) break
        }
    }

    val fatsPatterns = listOf(
        java.util.regex.Pattern.compile("Fats.*?(\\d+)\\s*g", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("الدهون.*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*g\\s*Fats", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(?:الدهون|دهون).*?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE),
        java.util.regex.Pattern.compile("(\\d+)\\s*(?:جرام|ج)\\s*دهون", java.util.regex.Pattern.CASE_INSENSITIVE)
    )
    for (pattern in fatsPatterns) {
        val matcher = pattern.matcher(normalizedText)
        if (matcher.find()) {
            fats = matcher.group(1)?.toIntOrNull() ?: 0
            if (fats > 0) break
        }
    }

    return MacroValues(calories, protein, carbs, fats)
}

fun isArabicChar(char: Char): Boolean {
    val block = java.lang.Character.UnicodeBlock.of(char)
    return block == java.lang.Character.UnicodeBlock.ARABIC ||
           block == java.lang.Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
           block == java.lang.Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
           block == java.lang.Character.UnicodeBlock.ARABIC_SUPPLEMENT
}

fun isArabicText(text: String): Boolean {
    return text.any { isArabicChar(it) }
}

@Composable
fun parseMarkdownToAnnotatedString(text: String, primaryColor: Color): AnnotatedString {
    return remember(text) {
        val builder = AnnotatedString.Builder()
        var lastIdx = 0
        val pattern = java.util.regex.Pattern.compile("\\*\\*(.*?)\\*\\*")
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            
            if (start > lastIdx) {
                builder.append(text.substring(lastIdx, start))
            }
            
            builder.pushStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            builder.append(matcher.group(1) ?: "")
            builder.pop()
            
            lastIdx = end
        }
        if (lastIdx < text.length) {
            builder.append(text.substring(lastIdx))
        }
        builder.toAnnotatedString()
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) { text.split("\n") }
    val numberPattern = remember { java.util.regex.Pattern.compile("^\\d+\\.\\s+(.*)") }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        lines.forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("[MACROS")) {
                val isArabicLine = isArabicText(trimmedLine)
                val direct = if (isArabicLine) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides direct) {
                    when {
                        trimmedLine.startsWith("###") -> {
                            val headerText = trimmedLine.removePrefix("###").trim()
                            Text(
                                text = parseMarkdownToAnnotatedString(headerText, NeonYellow),
                                color = NeonYellow,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start,
                                style = TextStyle(textDirection = if (isArabicLine) TextDirection.Rtl else TextDirection.Ltr),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                lineHeight = 22.sp
                            )
                        }
                        trimmedLine.startsWith("##") -> {
                            val headerText = trimmedLine.removePrefix("##").trim()
                            Text(
                                text = parseMarkdownToAnnotatedString(headerText, NeonYellow),
                                color = NeonYellow,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start,
                                style = TextStyle(textDirection = if (isArabicLine) TextDirection.Rtl else TextDirection.Ltr),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                lineHeight = 24.sp
                            )
                        }
                        trimmedLine.startsWith("#") -> {
                            val headerText = trimmedLine.removePrefix("#").trim()
                            Text(
                                text = parseMarkdownToAnnotatedString(headerText, NeonYellow),
                                color = NeonYellow,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start,
                                style = TextStyle(textDirection = if (isArabicLine) TextDirection.Rtl else TextDirection.Ltr),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                lineHeight = 28.sp
                            )
                        }
                        trimmedLine.startsWith("-") || trimmedLine.startsWith("*") || trimmedLine.startsWith("•") -> {
                            val rawListText = trimmedLine.substring(1).trim()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(6.dp)
                                        .background(NeonYellow, CircleShape)
                                )
                                Text(
                                    text = parseMarkdownToAnnotatedString(rawListText, NeonYellow),
                                    color = Color(0xFFD2D2D9),
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Start,
                                    style = TextStyle(textDirection = if (isArabicLine) TextDirection.Rtl else TextDirection.Ltr),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        else -> {
                            val matcher = numberPattern.matcher(trimmedLine)
                            if (matcher.matches()) {
                                val listText = matcher.group(1) ?: ""
                                val numberText = trimmedLine.substring(0, trimmedLine.indexOf(".")) + "."
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = numberText,
                                        color = NeonYellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = parseMarkdownToAnnotatedString(listText, NeonYellow),
                                        color = Color(0xFFD2D2D9),
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.Start,
                                        style = TextStyle(textDirection = if (isArabicLine) TextDirection.Rtl else TextDirection.Ltr),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                // Standard paragraph text
                                Text(
                                    text = parseMarkdownToAnnotatedString(trimmedLine, NeonYellow),
                                    color = Color(0xFFE2E2E9),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Start,
                                    style = TextStyle(textDirection = if (isArabicLine) TextDirection.Rtl else TextDirection.Ltr),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
