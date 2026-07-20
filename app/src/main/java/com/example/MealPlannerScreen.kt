package com.example

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class AthleticFoodItem(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)

@Composable
fun MealPlannerScreen(
    isArabic: Boolean,
    viewModel: GymViewModel,
    onSmartChefClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val macros by viewModel.macroProgress.collectAsState()

    val resolved = macros.getTargets()
    val targetCalories = resolved.calories
    val targetProtein = resolved.protein
    val targetCarbs = resolved.carbs
    val targetFats = resolved.fats

    val resolvedGym = macros.copy(isGymDay = true).getTargets()
    val resolvedRest = macros.copy(isGymDay = false).getTargets()

    // Local SharedPreferences parameters
    val prefs = remember { context.getSharedPreferences("gym_pro_prefs", Context.MODE_PRIVATE) }
    var waterCups by remember { mutableStateOf(prefs.getInt("water_cups_count", 0)) }

    // Coach advices / meals generator
    var showCustomFoodDialog by remember { mutableStateOf(false) }

    // Pro athlete fuel inputs
    val athleticFoods = remember {
        listOf(
            AthleticFoodItem("chicken", "صدور دجاج مشوية (١٥٠ج)", "Grilled Chicken (150g)", 240, 46, 0, 4),
            AthleticFoodItem("oats", "شوفان بالمكسرات (٨٠ج)", "Oats with Nuts (80g)", 340, 12, 54, 8),
            AthleticFoodItem("eggs", "بيض كامل مسلوق بالسبانخ", "Boiled Eggs with Spinach", 220, 18, 2, 14),
            AthleticFoodItem("rice", "أرز بسمتي مطبوخ بالبخار", "Steam Basmati Rice", 195, 4, 42, 1),
            AthleticFoodItem("greek_yogurt", "زبادي يوناني طبيعي عسل", "greek Yogurt with Honey", 140, 14, 12, 3),
            AthleticFoodItem("banana", "حبة موزة مع زبدة فول", "Banana with Peanut Butter", 210, 5, 29, 9),
            AthleticFoodItem("almonds", "مكسرات لوز مطحونة (٤٠ج)", "Raw Almonds (40g)", 230, 8, 8, 19),
            AthleticFoodItem("whey", "سكوب واي بروتين معزول", "Isolate Whey Scoop", 130, 26, 1, 1)
        )
    }

    val mainActivity = context as? MainActivity

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (macros.weight <= 0f) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .iosSquircleCard(
                            cornerSizeRatio = 0.28f,
                            baseOpacity = 0.16f,
                            saturationScale = 1.80f
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SportyRed.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Weight,
                                contentDescription = null,
                                tint = SportyRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "اضبط ملفك الرياضي والماكروز ⚡" else "SETUP ATHLETIC PROFILE & MACROS ⚡",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isArabic) {
                                    "خطتك فارغة حالياً والبيانات الافتراضية معطلة. يرجى تهيئة طولك ووزنك ونظامك المفضل من لوحة «الإعدادات ⚙️» ليتولى الذكاء الاصطناعي حساب وحقن سعراتك وبنائك العضلي الدقيق!"
                                } else {
                                    "Your calculated sports matrix is currently unconfigured. Enter your weight, height, and athletic goal to automatically formulate calorie and protein targets tailored by Gemini!"
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = { 
                                mainActivity?.triggerVibration(65)
                                viewModel.requestTabChange(DashboardTab.SETTINGS) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SportyRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (isArabic) "انتقال لتهيئة الملف والذكاء الاصطناعي الآن ⚙️" else "SETUP ATHLETIC PROFILE NOW ⚙️",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Column {
                    // --- 1. PREMIUM ATHLETIC GYM VS REST SWITCHER ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .recessedGlassInput(cornerSizeRatio = 0.45f, baseOpacity = 0.08f)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Training Day Pill Selector
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SquircleShape(0.40f))
                                .background(if (macros.isGymDay) NeonYellow else Color.Transparent)
                                .clickable { 
                                    viewModel.updateMacros(macros.copy(isGymDay = true))
                                    mainActivity?.triggerVibration(65)
                                    mainActivity?.speak(
                                        if (isArabic) "تم تفعيل نمط يوم التمرين بمستهدف ${resolvedGym.calories} سعرة" 
                                        else "Gym training day active. Target ${resolvedGym.calories} calories.", 
                                        isArabic
                                    )
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PhosphorIcons.Flame,
                                    contentDescription = null,
                                    tint = if (macros.isGymDay) CyberCharcoal else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "يوم تدريب (${resolvedGym.calories / 1000f}k)" else "TRAINING DAY (${resolvedGym.calories / 1000f}k)",
                                    color = if (macros.isGymDay) CyberCharcoal else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
            
                        // Passive Recovery Pill Selector
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SquircleShape(0.40f))
                                .background(if (!macros.isGymDay) NeonYellow else Color.Transparent)
                                .clickable { 
                                    viewModel.updateMacros(macros.copy(isGymDay = false))
                                    mainActivity?.triggerVibration(65)
                                    mainActivity?.speak(
                                        if (isArabic) "تم تفعيل نمط يوم الاستراحة بمستهدف ${resolvedRest.calories} سعرة" 
                                        else "Rest recovery day active. Target ${resolvedRest.calories} calories.", 
                                        isArabic
                                    )
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = PhosphorIcons.Leaf,
                                    contentDescription = null,
                                    tint = if (!macros.isGymDay) CyberCharcoal else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "يوم استراحة (${resolvedRest.calories / 1000f}k)" else "RECOVERY DAY (${resolvedRest.calories / 1000f}k)",
                                    color = if (!macros.isGymDay) CyberCharcoal else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
            
                    // --- 2. ASYMMETRICAL CYBERNETIC MACROS TRACKER INDEX ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .iosSquircleCard(
                                cornerSizeRatio = 0.28f,
                                baseOpacity = 0.16f,
                                saturationScale = 1.80f
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Split A: Dominant primary Calories HUD Circle
                            val calProgress = (macros.calories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .size(105.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.05f),
                                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = NeonYellow,
                                            startAngle = -90f,
                                            sweepAngle = 360f * calProgress,
                                            useCenter = false,
                                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${macros.calories}",
                                        color = NeonYellow,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                    Text(
                                        text = "/$targetCalories",
                                        color = DimGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isArabic) "سعرة" else "KCAL",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
            
                            Spacer(modifier = Modifier.width(20.dp))
            
                            // Split B: Linear high performance digital tubes for Protein, Carbs, Fats
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = (if (isArabic) "مؤشرات البناء والتدفق" else "METABOLIC PROFILE INDEX").uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
            
                                // Protein Row tube
                                MacroLinearTube(
                                    label = if (isArabic) "بروتين" else "Protein",
                                    currentVal = macros.protein,
                                    targetVal = targetProtein,
                                    unit = "g",
                                    color = Color(0xFF00E5FF)
                                )
            
                                // Carbs Row tube
                                MacroLinearTube(
                                    label = if (isArabic) "كربوهيدرات" else "Carbs",
                                    currentVal = macros.carbs,
                                    targetVal = targetCarbs,
                                    unit = "g",
                                    color = Color(0xFFFF9100)
                                )
            
                                // Fats Row tube
                                MacroLinearTube(
                                    label = if (isArabic) "دهون هرمونية" else "Fats",
                                    currentVal = macros.fats,
                                    targetVal = targetFats,
                                    unit = "g",
                                    color = Color(0xFFE040FB)
                                )
                            }
                        }
                    }
            
                    Spacer(modifier = Modifier.height(14.dp))
            
                    // --- 3. CONTROL BAR PANEL PACK (AI Chef, Custom Food add, Reset) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Intelligent Chef Option
                        Button(
                            onClick = onSmartChefClick,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Trophy, 
                                contentDescription = null, 
                                tint = CyberCharcoal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "شيف جيم برو 🍳" else "GymPro Chef 🍳",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
            
                        // Custom values picker
                        Button(
                            onClick = { showCustomFoodDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RockGray, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Add, 
                                contentDescription = null, 
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تسجيل وجبة" else "Log Manual",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
            
                        // Zero value resetting
                        IconButton(
                            onClick = { 
                                viewModel.updateMacros(macros.copy(calories = 0, protein = 0, carbs = 0, fats = 0))
                                mainActivity?.triggerVibration(150)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF2C1618), RoundedCornerShape(14.dp))
                                .border(0.5.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Remove,
                                contentDescription = "Reset Macros Indices",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 5. DETAILED FEEDING LIST CONTENT ---
        // A. High responsive interactive water hydration tracker
        item {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .iosSquircleCard(
                    cornerSizeRatio = 0.28f,
                    baseOpacity = 0.16f,
                    saturationScale = 1.80f
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "مستوى ترطيب الهايدرو" else "ANABOLIC COMPACT HYDRATION",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                  text = if (isArabic) "أتممت $waterCups من أصل ١٠ أكواب يومية" else "Secured $waterCups of 10 athlete units",
                                color = Color(0xFF00E5FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Interactive mini-status bar representation
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                for (i in 1..10) {
                                    val isFilled = i <= waterCups
                                    val barColor = if (isFilled) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f)
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(2f.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                        }

                        // Hydro actions clickers
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (waterCups > 0) {
                                        waterCups -= 1
                                        prefs.edit().putInt("water_cups_count", waterCups).apply()
                                        mainActivity?.triggerVibration(30)
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Remove, 
                                    contentDescription = "Decrease water volume", 
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (waterCups < 20) {
                                        waterCups += 1
                                        prefs.edit().putInt("water_cups_count", waterCups).apply()
                                        mainActivity?.triggerVibration(45)
                                        mainActivity?.playBeep()
                                        if (waterCups == 10) {
                                            mainActivity?.speak(if (isArabic) "رائع! أكتمل ترطيب وتأمين المياه لليوم بنجاح" else "Fantastic, perfect daily hydro targets met.", isArabic)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF00E5FF), CircleShape)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Add, 
                                    contentDescription = "Increase water volume", 
                                    tint = CyberCharcoal,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // B. Quick fuel cell tapping inputs preset
            item {
                Column {
                    Text(
                        text = if (isArabic) "سجل وجبات وميكروز رياضية سريعة:" else "TAP TO CO-LOG ATHLETIC FUEL PRESET:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(athleticFoods) { food ->
                            Box(
                                modifier = Modifier
                                    .clip(SquircleShape(0.28f))
                                    .background(RockGray)
                                    .border(1.dp, GlassBorder, SquircleShape(0.28f))
                                    .clickable {
                                        viewModel.updateMacros(
                                            macros.copy(
                                                calories = macros.calories + food.calories,
                                                protein = macros.protein + food.protein,
                                                carbs = macros.carbs + food.carbs,
                                                fats = macros.fats + food.fats
                                            )
                                        )
                                        mainActivity?.triggerVibration(45)
                                        mainActivity?.playBeep()
                                        mainActivity?.speak(
                                            if (isArabic) {
                                                "تم تسجيل " + food.nameAr + " بنجاح"
                                            } else {
                                                "Logged " + food.nameEn
                                            }, 
                                            isArabic
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) food.nameAr else food.nameEn,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "+${food.calories} kcal",
                                            color = NeonYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "P: +${food.protein}g",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. Static elite athletic suggestions
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "التوصيات والخطط الغذائية الاحترافية" else "ELITE ATHLETIC FEEDING DOSSIER",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (macros.isGymDay) {
                    CoachDossierSheet(
                        title = if (isArabic) "طاقة وقوة ما قبل التمرين" else "High Energy Pre-Workout",
                        desc = if (isArabic) "تناول مصادر الكربوهيدرات المعقدة (أرز بسمتي أو شوفان) مع مصدر بروتين عالي وسريع الهضم قبل حصتك التدريبية بـ ٩٠ دقيقة لتوجيه الطاقة المطلوبة وزيادة ضغط النيتروجين في العضلات." else "Load clean complex carbs (Basmati/Oats) plus highly bioavailable protein 90 mins prior to training sessions to secure muscular pump and constant hydration tiers.",
                        iconColor = NeonYellow
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CoachDossierSheet(
                        title = if (isArabic) "نافذة الاستشفاء والبناء الفوري" else "Post-Workout Hypertrophy Window",
                        desc = if (isArabic) "احرص على إمداد عضلاتك بالواي بروتين وكربوهيدرات سريعة لتعويض الجليكوجين العضلي المفقود ودعم ألياف البروتين لإصلاح الأنسجة الممزقة." else "Take direct rapid isolate amino acids with high glycemics within 45 mins post-workout to enhance instant macro delivery and muscular recovery.",
                        iconColor = NeonYellow
                    )
                } else {
                    CoachDossierSheet(
                        title = if (isArabic) "صيانة المستهدف في أوقات الراحة" else "Caloric Profile Rest Management",
                        desc = if (isArabic) "في أيام الاستشفاء السلبي، قلل من تناول الكربوهيدرات الكلية مقابل زيادة الدهون الصحية (أوميجا ٣ ولوز وأفوكادو) لوقاية المفاصل وتعزيز الغدد وإفراز الهرمونات البنائية الذاتية." else "Reduce total insulin-spiking carbs on non-lifting recovery cycles and load plant fats (almond, omega/avocados) to boost joints health and inner hormone profiles.",
                        iconColor = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CoachDossierSheet(
                        title = if (isArabic) "توزيع البروتينات الليلية المضادة للهدم" else "Anti-catabolic Nighttime Supper",
                        desc = if (isArabic) "تناول الكازين أو زبادي يوناني مع البيض لتأمين أحماض أمينية بطيئة الامتصاص طوال فترة النوم، لمنع التفكيك العضلي وتوجيه البناء الصافي." else "Secure slow-digesting proteins (Greek yogurt/Cottage casein) with egg white limits prior to sleep to stop overnight muscle tissue oxidation.",
                        iconColor = Color(0xFF00E5FF)
                    )
                }
            }
    }

    // --- DIALOG FOR FULLY CUSTOM FOOD PARAMETERS CREATOR ---
    if (showCustomFoodDialog) {
        var foodName by remember { mutableStateOf("") }
        var foodCalStr by remember { mutableStateOf("") }
        var foodProtStr by remember { mutableStateOf("") }
        var foodCarbStr by remember { mutableStateOf("") }
        var foodFatStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomFoodDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إدخل ميكروز وجبة مخصصة" else "MANUAL NUTRITION LOGGER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .recessedGlassInput(cornerSizeRatio = 0.28f, baseOpacity = 0.08f)
                    ) {
                        OutlinedTextField(
                            value = foodName,
                            onValueChange = { foodName = it },
                            label = { Text(if (isArabic) "اسم الوجبة بالكامل" else "Meal Description") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedLabelColor = NeonYellow,
                                unfocusedLabelColor = DimGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .recessedGlassInput(cornerSizeRatio = 0.28f, baseOpacity = 0.08f)
                        ) {
                            OutlinedTextField(
                                value = foodCalStr,
                                onValueChange = { foodCalStr = it },
                                label = { Text(if (isArabic) "السعرات" else "Kcal") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedLabelColor = NeonYellow,
                                    unfocusedLabelColor = DimGray
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .recessedGlassInput(cornerSizeRatio = 0.28f, baseOpacity = 0.08f)
                        ) {
                            OutlinedTextField(
                                value = foodProtStr,
                                onValueChange = { foodProtStr = it },
                                label = { Text(if (isArabic) "البروتين" else "P(g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedLabelColor = NeonYellow,
                                    unfocusedLabelColor = DimGray
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .recessedGlassInput(cornerSizeRatio = 0.28f, baseOpacity = 0.08f)
                        ) {
                            OutlinedTextField(
                                value = foodCarbStr,
                                onValueChange = { foodCarbStr = it },
                                label = { Text(if (isArabic) "الكارب" else "C(g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedLabelColor = NeonYellow,
                                    unfocusedLabelColor = DimGray
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .recessedGlassInput(cornerSizeRatio = 0.28f, baseOpacity = 0.08f)
                        ) {
                            OutlinedTextField(
                                value = foodFatStr,
                                onValueChange = { foodFatStr = it },
                                label = { Text(if (isArabic) "الدهون" else "F(g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedLabelColor = NeonYellow,
                                    unfocusedLabelColor = DimGray
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val calories = foodCalStr.toIntOrNull() ?: 0
                        val protein = foodProtStr.toIntOrNull() ?: 0
                        val carbs = foodCarbStr.toIntOrNull() ?: 0
                        val fats = foodFatStr.toIntOrNull() ?: 0
                        viewModel.updateMacros(
                            macros.copy(
                                calories = macros.calories + calories,
                                protein = macros.protein + protein,
                                carbs = macros.carbs + carbs,
                                fats = macros.fats + fats
                            )
                        )
                        showCustomFoodDialog = false
                        mainActivity?.triggerVibration(100)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = CyberCharcoal)
                ) {
                    Text(if (isArabic) "تسجيل وحساب" else "CO-LOG MACROS", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFoodDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "DISMISS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF16161A),
            shape = SquircleShape(0.28f)
        )
    }
}

@Composable
fun MacroLinearTube(
    label: String,
    currentVal: Int,
    targetVal: Int,
    unit: String,
    color: Color
) {
    val fillRatio = (currentVal.toFloat() / targetVal.toFloat()).coerceIn(0f, 1f)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label, 
                color = Color.White.copy(alpha = 0.85f), 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currentVal/$targetVal$unit", 
                color = color, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        // Digital progress outline tube
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(SquircleShape(0.48f))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillRatio)
                    .height(6.dp)
                    .clip(SquircleShape(0.48f))
                    .background(color)
            )
        }
    }
}

@Composable
fun CoachDossierSheet(
    title: String,
    desc: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iosSquircleCard(
                cornerSizeRatio = 0.28f,
                baseOpacity = 0.16f,
                saturationScale = 1.80f
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PhosphorIcons.ForkKnife, 
                contentDescription = null, 
                tint = iconColor, 
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = DimGray,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
