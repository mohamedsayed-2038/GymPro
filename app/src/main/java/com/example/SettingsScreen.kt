package com.example

import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    isArabic: Boolean,
    onLanguageToggle: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GymViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val athleteState by viewModel.macroProgress.collectAsState()

    var username by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var fitnessGoal by remember { mutableStateOf("Bulking") }
    var injuryInput by remember { mutableStateOf("") }
    var dietType by remember { mutableStateOf("Standard") }
    
    // Loaded status to prevent overriding while editing
    var isInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(athleteState) {
        if (!isInitialized && athleteState.weight > 0) {
            username = athleteState.username
            weightInput = athleteState.weight.toString()
            heightInput = athleteState.height.toString()
            ageInput = athleteState.age.toString()
            gender = athleteState.gender.ifEmpty { "Male" }
            activityLevel = athleteState.activityLevel.ifEmpty { "Moderate" }
            fitnessGoal = athleteState.fitnessGoal.ifEmpty { "Bulking" }
            injuryInput = athleteState.injuries
            dietType = athleteState.dietType.ifEmpty { "Standard" }
            isInitialized = true
        }
    }

    var isUpdating by remember { mutableStateOf(false) }
    var aiLogs by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    fun openWhatsApp(number: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=+20$number"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun openInstagram() {
        try {
            val url = "https://www.instagram.com/1x___moh__?igsh=MWQweTdqbzFiendiaw=="
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    // Centered professional gradients using the official NeonYellow accent (known as SportyRed in theme)
    val brandGradient = Brush.linearGradient(
        colors = listOf(SportyRed, SportyRed.copy(alpha = 0.8f))
    )
    val surfaceGradient = Brush.verticalGradient(
        colors = listOf(RockGray, Color(0xFF151515))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberCharcoal)
    ) {
        // High-end ambient neon glow behind content
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(400.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SportyRed.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(300f, -50f),
                        radius = 700f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 88.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. Premium Brand Identity Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceGradient, RoundedCornerShape(20.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Modern stylized logo container
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(CyberCharcoal, CircleShape)
                            .border(1.dp, SportyRed.copy(alpha = 0.4f), CircleShape)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_transparent),
                            contentDescription = "GymPro Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "GYMPRO",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Minimal custom chip
                    Box(
                        modifier = Modifier
                            .background(SportyRed.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, SportyRed.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الإصدار الاحترافي v1.0" else "PREMIUM EDITION v1.0",
                            color = SportyRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Completely polished, clean, non-exaggerated description
                    Text(
                        text = if (isArabic) {
                            "جيم برو هو رفيقك الرياضي المتقدم المصمم لتنظيم ومتابعة التمارين بدقة، وإدارة مؤقتات الفواصل الزمنية المخصصة، وتخطيط الوجبات الغذائية، مع دعم التحليل المهني للصور للارتقاء بأدائك البدني اليومي."
                        } else {
                            "GymPro is an advanced athletic companion designed to organize and track training session durations, manage custom interval timers, design structured meals, and leverage smart visual analysis to elevate your physical performance."
                        },
                        color = DimGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 1.5 Personal Athletic Profile & AI Coach Calculator Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "الملف الشخصي الرياضي والذكاء الاصطناعي" else "ATHLETIC BODY PROFILE & AI METRICS",
                    color = SportyRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceGradient, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Username
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(if (isArabic) "الاسم الرياضي" else "Athletic Nickname", color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SportyRed,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor = SportyRed,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Weight & Height
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = { Text(if (isArabic) "الوزن (كجم)" else "Weight (kg)", color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SportyRed,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedLabelColor = SportyRed
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = { heightInput = it },
                                label = { Text(if (isArabic) "الطول (سم)" else "Height (cm)", color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SportyRed,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedLabelColor = SportyRed
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Age & Gender
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ageInput,
                                onValueChange = { ageInput = it },
                                label = { Text(if (isArabic) "العمر" else "Age (years)", color = Color.White.copy(alpha = 0.6f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SportyRed,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedLabelColor = SportyRed
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            // Gender Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "الجنس" else "Gender",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(CyberCharcoal, RoundedCornerShape(8.dp))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (gender == "Male") SportyRed.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { gender = "Male" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isArabic) "ذكر" else "Male",
                                            color = if (gender == "Male") SportyRed else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (gender == "Female") SportyRed.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { gender = "Female" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isArabic) "أنثى" else "Female",
                                            color = if (gender == "Female") SportyRed else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Fitness Goal Row
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isArabic) "الهدف الرياضي" else "Fitness Goal",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(CyberCharcoal, RoundedCornerShape(8.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val goals = listOf(
                                    "Bulking" to (if (isArabic) "تضخيم" else "Bulking"),
                                    "Cutting" to (if (isArabic) "تنشيف" else "Cutting"),
                                    "Recomp" to (if (isArabic) "محافظة" else "Recomp")
                                )
                                goals.forEach { (key, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (fitnessGoal == key) SportyRed.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { fitnessGoal = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (fitnessGoal == key) SportyRed else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Activity Level Row
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isArabic) "مستوى النشاط البدني" else "Activity Level",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(CyberCharcoal, RoundedCornerShape(8.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val activites = listOf(
                                    "Sedentary" to (if (isArabic) "خامل" else "Sedentary"),
                                    "Moderate" to (if (isArabic) "متوسط" else "Moderate"),
                                    "HighlyActive" to (if (isArabic) "فائق النشاط" else "Elite Athlete")
                                )
                                activites.forEach { (key, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (activityLevel == key) SportyRed.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { activityLevel = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (activityLevel == key) SportyRed else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Diet Preference Choice
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (isArabic) "النظام الغذائي المفضل" else "Diet Style Preference",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(CyberCharcoal, RoundedCornerShape(8.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val diets = listOf(
                                    "Standard" to (if (isArabic) "عادي" else "Standard"),
                                    "Keto" to (if (isArabic) "كيتو" else "Keto"),
                                    "Vegetarian" to (if (isArabic) "نباتي" else "Vegetarian")
                                )
                                diets.forEach { (key, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (dietType == key) SportyRed.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { dietType = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (dietType == key) SportyRed else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Injuries notes input
                        OutlinedTextField(
                            value = injuryInput,
                            onValueChange = { injuryInput = it },
                            label = { Text(if (isArabic) "هل تعاني من أي إصابات أو تفضيلات؟" else "Any Injuries/Medical Limits?", color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SportyRed,
                                unfocusedBorderColor = GlassBorder,
                                focusedLabelColor = SportyRed
                            ),
                            placeholder = { Text(if (isArabic) "ألم الركبة، الظهر، الكاحل..." else "Knee pain, lower back discomfort...", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Status / error / feedback messages
                        statusMessage?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isErrorStatus) Color.Red.copy(alpha = 0.1f) else SportyRed.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isErrorStatus) Color.Red.copy(alpha = 0.3f) else SportyRed.copy(alpha = 0.25f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = if (isErrorStatus) Color.Red else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Submit Button with active states and loader
                        val coroutineScope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                val wNum = weightInput.replace(",", ".").toFloatOrNull() ?: 0f
                                val hNum = heightInput.replace(",", ".").toFloatOrNull() ?: 0f
                                val aNum = ageInput.toIntOrNull() ?: 0

                                if (wNum <= 30f || hNum <= 80f || aNum <= 8) {
                                    statusMessage = if (isArabic) "يرجى إدخال بيانات وزن وطول وعمر منطقية وصحيحة!" else "Please enter realistic weight, height, and age numbers!"
                                    isErrorStatus = true
                                    return@Button
                                }

                                isUpdating = true
                                isErrorStatus = false
                                statusMessage = if (isArabic) "جاري إيقاظ الكابتن الذكي وحساب السعرات... ⚡" else "Booting AI sports dietitian to formulate targets... ⚡"

                                coroutineScope.launch {
                                    try {
                                        val assistant = viewModel.aiAssistant
                                        // Request Gemini calculation
                                        val aiAdvice = assistant.calculatePersonalMacros(
                                            weight = wNum,
                                            height = hNum,
                                            age = aNum,
                                            gender = gender,
                                            activityLevel = activityLevel,
                                            fitnessGoal = fitnessGoal,
                                            dietType = dietType,
                                            injuries = injuryInput,
                                            isArabic = isArabic
                                        )

                                        val summaryLine = aiAdvice.lines().firstOrNull { it.contains("[TARGETS_SUMMARY]") }
                                        var gymCalories = if (fitnessGoal == "Cutting") 2200 else if (fitnessGoal == "Bulking") 3000 else 2500
                                        var restCalories = gymCalories - 400
                                        var gymProtein = (wNum * 2.0f).toInt()
                                        var restProtein = gymProtein
                                        var gymCarbs = 300
                                        var restCarbs = 200
                                        var gymFats = 70
                                        var restFats = 60

                                        if (summaryLine != null) {
                                            try {
                                                val clean = summaryLine.substringAfter("[TARGETS_SUMMARY]:").trim()
                                                val mParts = clean.split("|").associate { 
                                                    val parts = it.split(":")
                                                    parts[0].trim().lowercase() to parts[1].trim().toInt()
                                                }
                                                gymCalories = mParts["gymcalories"] ?: gymCalories
                                                restCalories = mParts["restcalories"] ?: restCalories
                                                gymProtein = mParts["gymprotein"] ?: gymProtein
                                                restProtein = mParts["restprotein"] ?: restProtein
                                                gymCarbs = mParts["gymcarbs"] ?: gymCarbs
                                                restCarbs = mParts["restcarbs"] ?: restCarbs
                                                gymFats = mParts["gymfats"] ?: gymFats
                                                restFats = mParts["restfats"] ?: restFats
                                            } catch (ex: Exception) { }
                                        }

                                        val updatedProgress = athleteState.copy(
                                            username = username,
                                            weight = wNum,
                                            height = hNum,
                                            age = aNum,
                                            gender = gender,
                                            activityLevel = activityLevel,
                                            fitnessGoal = fitnessGoal,
                                            dietType = dietType,
                                            injuries = injuryInput,
                                            
                                            targetCaloriesGym = gymCalories,
                                            targetCaloriesRest = restCalories,
                                            targetProteinGym = gymProtein,
                                            targetProteinRest = restProtein,
                                            targetCarbsGym = gymCarbs,
                                            targetCarbsRest = restCarbs,
                                            targetFatsGym = gymFats,
                                            targetFatsRest = restFats
                                        )
                                        viewModel.updateMacros(updatedProgress)

                                        aiLogs = aiAdvice.split("[TARGETS_SUMMARY]").first().trim()
                                        statusMessage = if (isArabic) "تم التحديث بالكامل! تم تعيين مستهدفاتك المخصصة بنجاح 🎉" else "Success! Your custom target benchmarks are now active! 🎉"
                                    } catch (e: Exception) {
                                        isErrorStatus = true
                                        statusMessage = if (e.message?.contains("API_KEY_MISSING") == true) {
                                            if (isArabic) "عذراً يا بطل، مفتاح API مفقود! يرجى تهيئة مفتاح GEMINI_API_KEY في النظام أولاً." else "Error: Gemini API key is missing in your setup."
                                        } else {
                                            e.message ?: "Analysis failed."
                                        }
                                    } finally {
                                        isUpdating = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SportyRed),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isUpdating
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (isArabic) "حساب المستهدفات بالذكاء الاصطناعي ⚡" else "CALCULATE TARGET METRICS ⚡",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // AI Coach advice log output if present
                        if (aiLogs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CyberCharcoal, RoundedCornerShape(12.dp))
                                    .border(1.dp, SportyRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = PhosphorIcons.Flame,
                                            contentDescription = null,
                                            tint = SportyRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isArabic) "توجيهات الكابتن التغذوية والرياضية:" else "AI Fitness Coach Counselor Advice:",
                                            color = SportyRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = aiLogs,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Localization Configuration Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "خيارات التطبيق" else "APP CONFIGURATION",
                    color = SportyRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(surfaceGradient, RoundedCornerShape(14.dp))
                        .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { onLanguageToggle() }
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyberCharcoal, CircleShape)
                                .border(1.dp, SportyRed.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Info,
                                contentDescription = null,
                                tint = SportyRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isArabic) "لغة الواجهة" else "Interface Language",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(CyberCharcoal, RoundedCornerShape(8.dp))
                            .border(1.dp, GlassBorder.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SportyRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "العربية" else "English",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Technical Specs Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "الميزات والأنظمة النشطة" else "ACTIVE CORE SYSTEMS",
                    color = SportyRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                )

                val features = listOf(
                    Triple(PhosphorIcons.Check, if (isArabic) "منظومة تحليل الطعام البصري الذكي" else "AI Meals Computer Vision Module", "Active / نشط"),
                    Triple(PhosphorIcons.Flame, if (isArabic) "مؤقت الفترات والتدريب فائق الدقة" else "High-Precision Workout Timer Core", "Active / نشط"),
                    Triple(PhosphorIcons.Microphone, if (isArabic) "المرشد والمدرب الصوتي التفاعلي" else "Bi-directional Voice Coach Engine", "Optimal / ممتاز"),
                    Triple(PhosphorIcons.Weight, if (isArabic) "تصميم داكن عالي التباين والأناقة" else "Premium Black Contrast Design Flow", "Optimal / ممتاز")
                )

                features.forEach { (icon, title, stateText) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceGradient, RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(CyberCharcoal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = SportyRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = stateText,
                            color = SportyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Developer Portfolio Segment
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "معلومات المطور والمهندس" else "DEVELOPER PORTFOLIO",
                    color = SportyRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceGradient, RoundedCornerShape(20.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Minimalist Initial badge
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CyberCharcoal, CircleShape)
                                .border(1.dp, SportyRed.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MS",
                                color = SportyRed,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isArabic) "محمد سيد سالم" else "Mohamed Sayed Salem",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lead Android & iOS UX/UI Architect",
                                color = DimGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Professional Sleek Row for Primary WhatsApp (Matches yellow theme)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCharcoal)
                                .border(1.dp, SportyRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { openWhatsApp("01140251843") }
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PhosphorIcons.Chat,
                                    contentDescription = null,
                                    tint = SportyRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isArabic) "الواتساب الأساسي للاتصال" else "Primary WhatsApp Support",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "01140251843",
                                color = SportyRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Professional Row for Secondary WhatsApp (Sleek and matching)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCharcoal)
                                .border(1.dp, SportyRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { openWhatsApp("01033254483") }
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PhosphorIcons.Chat,
                                    contentDescription = null,
                                    tint = SportyRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isArabic) "الواتساب الاحتياطي للاتصال" else "Alternative WhatsApp Support",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "01033254483",
                                color = SportyRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Professional Row for Instagram Profile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCharcoal)
                                .border(1.dp, SportyRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .clickable { openInstagram() }
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PhosphorIcons.Camera,
                                    contentDescription = null,
                                    tint = SportyRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isArabic) "حساب الإنستجرام الرسمي" else "Official Instagram Handle",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "@1x___moh__",
                                color = SportyRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
