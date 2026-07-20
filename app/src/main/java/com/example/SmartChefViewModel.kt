package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class SmartChefViewModel(application: Application) : AndroidViewModel(application) {
    private val gymDao = GymDatabase.getDatabase(application).gymDao()

    val macroProgress: StateFlow<MacroProgress> = gymDao.getMacros()
        .map { it ?: MacroProgress() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MacroProgress()
        )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val generativeModel by lazy {
        var key = ""
        try {
            key = BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) { }
        
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "null") {
            try {
                val properties = java.util.Properties()
                application.applicationContext.assets.open("secrets.properties").use { properties.load(it) }
                val assetKey = properties.getProperty("GEMINI_API_KEY")
                if (!assetKey.isNullOrEmpty()) {
                    key = assetKey
                }
            } catch (e: Exception) { }
        }

        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = key
        )
    }

    fun sendMessage(text: String, isArabic: Boolean) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(text = text, isUser = true)
        _chatMessages.update { it + userMsg }

        _isGenerating.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                var apiKey = ""
                try { apiKey = BuildConfig.GEMINI_API_KEY } catch (e: Exception) { }
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "null") {
                    try {
                        val properties = java.util.Properties()
                        getApplication<Application>().applicationContext.assets.open("secrets.properties").use { properties.load(it) }
                        val assetKey = properties.getProperty("GEMINI_API_KEY")
                        if (!assetKey.isNullOrEmpty()) apiKey = assetKey
                    } catch (e: Exception) { }
                }

                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    val missingKeyError = if (isArabic) "خطأ: مفتاح API للذكاء الاصطناعي (GEMINI_API_KEY) غير متوفر." else "Error: Gemini API key is missing."
                    _error.value = missingKeyError
                    _chatMessages.update { it + ChatMessage(text = missingKeyError, isUser = false) }
                    return@launch
                }

                // Gather Live Fitness Stats
                val currentMacros = macroProgress.value
                val currentTargets = currentMacros.getTargets()
                val targetCalories = currentTargets.calories
                val targetProtein = currentTargets.protein
                val targetCarbs = currentTargets.carbs
                val targetFats = currentTargets.fats

                val remainingCalories = maxOf(0, targetCalories - currentMacros.calories)
                val remainingProtein = maxOf(0, targetProtein - currentMacros.protein)
                val remainingCarbs = maxOf(0, targetCarbs - currentMacros.carbs)
                val remainingFats = maxOf(0, targetFats - currentMacros.fats)

                // Build Structured Conversation Flow with System Promopt Wrapper
                val systemPrompt = """
                You are 'GymPro Chef' (شيف جيم برو), an elite sports dietitian, professional fitness chef, and motivating physical coach.
                Your task is to be an interactive, highly professional, warm, and conversational chatbot for the user.
                
                COMMUNICATION STYLE & PERSONALITY:
                1. Warm & High-Energy: Always be encouraging, supportive, and polite. Act like a helpful personal coach/nutrition specialist.
                2. Natural Dialog: If the user greets you (e.g., "السلام عليكم", "مرحبًا", "هلا", "هاي", "Hello", "Hi"), do not ignore or reject it! Greet them back warmly, ask about their physical training today, and enthusiastically offer to build customized nutrition templates or answer their sports diet questions.
                3. Expert Trainer: Answer any informational fitness questions (e.g. "which oils are best?", "how to increase protein?") with high-accuracy nutrition/diet science.
                4. Tone of Dialog: If they write to you in Arabic, reply in dynamic, friendly Arabic (encouraged in fit Egyptian athletic dialect). If in English, reply in highly motivating and professional fitness-coach English.
                
                NUTRITIONAL RECIPE GENERATION PROTOCOL:
                If the user provides a list of ingredients (e.g. eggs, yogurt, oats), combine them with their live remaining macro targets below to construct an exact, professional athletic recipe:
                - Target Weights: Clearly state ingredient weights in grams (e.g. "150g egg whites", "50g oats").
                - Budget Friendly: If essential ingredients are missing to satisfy their dynamic deficits, you can add basic, standard Egyptian/general kitchen staples (like olive oil, water, basic spices, simple vegetables) and state their exact weights.
                - Clear Breakdown: Formulate a concrete breakdown containing: Recipe Title, Ingredients List, Step-by-Step Cooking Steps, and exact Macros (Calories, Protein, Carbs, Fats) computed specifically for this generated meal.
                
                FORMATTING DIRECTIVE (CRITICAL):
                Use structural Markdown formatting heavily so our custom visual parser renders it beautifully:
                - Use `# <Header>` for major titles.
                - Use `## <Header>` or `### <Header>` for section headers (e.g., ingredients, steps, macros).
                - Use bullet points (`- ` or `* ` or `• `) to describe steps and lists.
                - Use bold formatting (`**text**`) for emphasis, metrics, and ingredient names.
                - Do NOT repeat raw stars or dashes without correct format structures.
                - CRITICAL MACRO SUMMARY: If and only if you are outputting a recipe / custom sports meal, you MUST append a standardized, single macro summary line on its own separate line at the very end of your response:
                  `[MACROS_SUMMARY]: Calories: <val> kcal | Protein: <val> g | Carbs: <val> g | Fats: <val> g`
                  (Where <val> are pure western digits, e.g. Calories: 320 kcal | Protein: 42 g | Carbs: 12 g | Fats: 4 g)
                
                LIVE DYNAMIC FIT DEFICITS (FOR CALCULATING INGREDIENTS RECIPES):
                - Input text / available ingredients: $text
                - Calories needed today: $remainingCalories kcal
                - Protein needed today: $remainingProtein g
                - Carbohydrates needed today: $remainingCarbs g
                - Fats needed today: $remainingFats g
                """.trimIndent()

                // Construct full prompt containing conversation history up to this point
                val promptBuilder = java.lang.StringBuilder()
                promptBuilder.append(systemPrompt)
                promptBuilder.append("\n\n--- Conversation History ---\n")

                _chatMessages.value.dropLast(1).forEach { msg ->
                    if (msg.isUser) {
                        promptBuilder.append("User: ${msg.text}\n")
                    } else {
                        promptBuilder.append("GymPro Chef: ${msg.text}\n")
                    }
                }
                promptBuilder.append("User: $text\n")
                promptBuilder.append("GymPro Chef:")

                val response = generativeModel.generateContent(promptBuilder.toString())
                val replyText = response.text ?: (if (isArabic) "عذراً يا بطل، لم أستطع صياغة الوصفة الآن. حاول مرة أخرى!" else "Sorry champ, I couldn't formulate the recipe right now. Try again!")

                _chatMessages.update { it + ChatMessage(text = replyText, isUser = false) }
            } catch (e: Exception) {
                val errorMsg = handleAiError(e, isArabic)
                _error.value = errorMsg
                _chatMessages.update { it + ChatMessage(text = errorMsg, isUser = false) }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun handleAiError(e: Exception, isArabic: Boolean): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("429") || msg.contains("quota") || msg.contains("rate limit") -> {
                if (isArabic) "عذراً يا بطل، لقد تخطيت حدود الاستهلاك اللحظية المجانية للذكاء الاصطناعي. جرب بعد قليل! ⏳" 
                else "Rate limit exceeded. Please try again shortly. ⏳"
            }
            msg.contains("api key") || msg.contains("unauthorized") -> {
                if (isArabic) "مفتاح الـ API غير صالح. يرجى إعداده من لوحة الإعدادات."
                else "Invalid API key."
            }
            else -> {
                if (isArabic) "عذراً، المخدم مشغول بوزن السعرات حالياً، ركز بتمارينك وسنعاود الاتصال قريباً! 💪" 
                else "Server busy, focus on your lift and try later! 💪"
            }
        }
    }

    fun updateMacrosDirectly(newCalories: Int, newProtein: Int, newCarbs: Int, newFats: Int) {
        viewModelScope.launch {
            val current = macroProgress.value
            gymDao.updateMacros(
                current.copy(
                    calories = current.calories + newCalories,
                    protein = current.protein + newProtein,
                    carbs = current.carbs + newCarbs,
                    fats = current.fats + newFats
                )
            )
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        _error.value = null
    }
}
