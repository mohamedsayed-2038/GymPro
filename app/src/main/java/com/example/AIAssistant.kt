package com.example

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIAssistant(private val context: android.content.Context) {
    private val generativeModel: GenerativeModel by lazy {
        var key = ""
        try { key = BuildConfig.GEMINI_API_KEY } catch (e: Exception) {}
        
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "null") {
            try {
                val properties = java.util.Properties()
                context.assets.open("secrets.properties").use { properties.load(it) }
                val assetKey = properties.getProperty("GEMINI_API_KEY")
                if (!assetKey.isNullOrEmpty()) key = assetKey
            } catch (e: Exception) {}
        }
        
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = key
        )
    }

    private fun getApiKey(): String {
        var key = ""
        try { key = BuildConfig.GEMINI_API_KEY } catch (e: Exception) {}
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "null") {
            try {
                val properties = java.util.Properties()
                context.assets.open("secrets.properties").use { properties.load(it) }
                val assetKey = properties.getProperty("GEMINI_API_KEY")
                if (!assetKey.isNullOrEmpty()) key = assetKey
            } catch (e: Exception) {}
        }
        return key
    }

    suspend fun getChefAdvice(calories: Int, protein: Int, isArabic: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                if (getApiKey().isEmpty() || getApiKey() == "MY_GEMINI_API_KEY") {
                    return@withContext if (isArabic) "خطأ: المفتاح السري (API Key) غير متوفر." else "Error: API Key is missing."
                }
                val prompt = "Under 15 words, suggest an athletic meal recipe for Egyptian athlete with $calories kcal and $protein g protein left. Use Egyptian Arabic and end with dynamic food emoji."
                val response: GenerateContentResponse = generativeModel.generateContent(prompt)
                response.text ?: "تناول بيضتين مع توست وشوفان بالموز لحصاد الميكروز! 🍳"
            } catch (e: Exception) {
                // Handle common and rate-limit errors gracefully
                handleAiError(e, isArabic)
            }
        }
    }

    suspend fun getCoachAdvice(muscleGroup: String, isArabic: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                if (getApiKey().isEmpty() || getApiKey() == "MY_GEMINI_API_KEY") {
                    return@withContext if (isArabic) "خطأ: المفتاح السري (API Key) غير متوفر." else "Error: API Key is missing."
                }
                val musclePrompt = if (isArabic) "عضلة: $muscleGroup" else "Muscle: $muscleGroup"
                val prompt = if (isArabic) {
                    "Provide a high-octane 15-word maximum gym tip and motivation for $musclePrompt in premium gym context. Write in aggressive, exciting Egyptian Arabic. End with a fire emoji."
                } else {
                    "Provide a high-octane 15-word maximum gym tip and motivation for $musclePrompt in premium gym context. Write in intense, professional, exciting English. End with a fire emoji."
                }
                val response: GenerateContentResponse = generativeModel.generateContent(prompt)
                response.text ?: (if (isArabic) "اضغط الحديد بعنف، المكسب قادم لا محالة! 🔥" else "Smashed iron hard, the gains are yours to keep! 🔥")
            } catch (e: Exception) {
                handleAiError(e, isArabic)
            }
        }
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap, isArabic: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                if (getApiKey().isEmpty() || getApiKey() == "MY_GEMINI_API_KEY") {
                    throw Exception("API_KEY_MISSING")
                }
                val systemPrompt = "Analyze this food image. Identify the major edible ingredients or food components. Return ONLY a valid JSON array of food names in English (suitable for USDA food database search). Do not explain anything. For example: [\"Grilled chicken breast\", \"White rice\", \"Broccoli\"]."
                val inputContent = content {
                    image(bitmap)
                    text(systemPrompt)
                }
                val response: GenerateContentResponse = generativeModel.generateContent(inputContent)
                response.text ?: ""
            } catch (e: Exception) {
                if (e.message == "API_KEY_MISSING") throw e
                throw Exception(handleAiError(e, isArabic))
            }
        }
    }

    suspend fun getVoiceCoachResponse(userSpeechText: String, isArabic: Boolean): String {
        return withContext(Dispatchers.IO) {
            try {
                if (getApiKey().isEmpty() || getApiKey() == "MY_GEMINI_API_KEY") {
                    return@withContext if (isArabic) "خطأ في المفتاح السري." else "Error: API Key missing."
                }
                
                val languageDir = if (isArabic) "in fiery Egyptian Arabic" else "in intense English"
                val systemPrompt = "Act as an extreme, highly intense, strictly professional and slightly aggressive virtual gym coach. A user just said: '$userSpeechText'. Respond $languageDir. Your response MUST be maximum 2 lines. Do NOT use any Markdown formatting (no asterisks, no bold, etc.) so it can be read smoothly via Text-To-Speech."
                
                val response: GenerateContentResponse = generativeModel.generateContent(systemPrompt)
                response.text ?: if (isArabic) "عاش! كمل تمرينك!" else "Keep pushing! Don't stop!"
            } catch (e: Exception) {
                handleAiError(e, isArabic).replace("*", "")
            }
        }
    }

    suspend fun calculatePersonalMacros(
        weight: Float,
        height: Float,
        age: Int,
        gender: String,
        activityLevel: String,
        fitnessGoal: String,
        dietType: String,
        injuries: String,
        isArabic: Boolean
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                if (getApiKey().isEmpty() || getApiKey() == "MY_GEMINI_API_KEY") {
                    throw Exception("API_KEY_MISSING")
                }
                
                val prompt = """
                You are 'GymPro Nutrition Core' (محرك تغذية جيم برو الذكي), a senior sports dietitian and personal trainer.
                Analyze the user's sports profile and calculate their exact, customized daily calorie and macronutrient targets for both:
                1. GYM DAYS (أيام التمرين والتحميل العضلي)
                2. REST DAYS (أيام الراحة والاستشفاء العضلي)
                
                USER DETAILS:
                - Weight: $weight Kg
                - Height: $height Cm
                - Age: $age Years old
                - Gender: $gender
                - Physical Activity level: $activityLevel
                - Ultimate Fitness Goal: $fitnessGoal
                - Diet Style Preference: $dietType
                - Current Injuries / Considerations: $injuries
                
                MATHEMATICAL STANDARDS:
                - Use Mifflin-St Jeor equation to calculate BMR (Basal Metabolic Rate).
                - For Bulking (تضخيم): Add surplus (+300 to +500 kcal).
                - For Cutting (تنشيف): Apply deficit (-300 to -500 kcal).
                - For Recomposition/Maintenance (محافظة): Keep balanced.
                - Calculate high athletic protein ratio: 1.8g to 2.2g of protein per kg of body weight depending on intensity.
                - Maintain healthy fats (around 20-30% of total calories).
                - Make Gym days higher in calories and carbs, Rest days slightly lower and focused on raw clean recovery.
                
                OUTPUT DIRECTIVE (CRITICAL):
                You MUST return a response that contains:
                1. A brief explanation in Egyptian Arabic/English (depending on user preference isArabic = $isArabic) describing why these specific targets were calculated for them, along with diet advice custom-tailored to their goal ($fitnessGoal), diet style ($dietType), and injury considerations ($injuries). Keep advice under 2-3 short, highly motivating sentences.
                2. At the very end, append a single standardized raw summary line on its own line:
                [TARGETS_SUMMARY]: GymCalories: <val> | RestCalories: <val> | GymProtein: <val> | RestProtein: <val> | GymCarbs: <val> | RestCarbs: <val> | GymFats: <val> | RestFats: <val>
                Where <val> are pure integer values (no 'kcal' or 'g' or Arabic symbols in the raw sum line). Example of valid summary:
                [TARGETS_SUMMARY]: GymCalories: 2850 | RestCalories: 2350 | GymProtein: 175 | RestProtein: 150 | GymCarbs: 310 | RestCarbs: 210 | GymFats: 75 | RestFats: 65
                """.trimIndent()
                
                val response: GenerateContentResponse = generativeModel.generateContent(prompt)
                response.text ?: ""
            } catch (e: Exception) {
                if (e.message == "API_KEY_MISSING") throw e
                throw Exception(handleAiError(e, isArabic))
            }
        }
    }

    private fun handleAiError(e: Exception, isArabic: Boolean): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("429") || msg.contains("quota") || msg.contains("rate limit") -> {
                if (isArabic) "عذراً، لقد تخطيت حدود الاستهلاك المجانية اللحظية. جرب بعد قليل! ⏳" 
                else "Rate limit exceeded. Please try again shortly. ⏳"
            }
            msg.contains("api key") || msg.contains("unauthorized") -> {
                if (isArabic) "مفتاح API غير صالح. تأكد من الإعدادات."
                else "Invalid API key."
            }
            else -> {
                if (isArabic) "المخدم مشغول، ركز في تمرينك وسنعاود الاتصال قريباً! 💪" 
                else "Server busy, focus on your lift and try later! 💪"
            }
        }
    }
}
