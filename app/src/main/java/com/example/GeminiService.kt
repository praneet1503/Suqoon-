package com.example

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.Header
import android.util.Log

// Gemini Request/Response Models
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// Groq Request/Response Models
@JsonClass(generateAdapter = true)
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GroqMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GroqResponse(
    val choices: List<GroqChoice>?
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val message: GroqMessage?
)

interface GroqApiService {
    @POST("chat/completions")
    suspend fun generateChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): GroqResponse
}

object RetrofitClient {
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val groqService: GroqApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GROQ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GroqApiService::class.java)
    }
}

object GeminiService {
    private const val TAG = "GeminiService"
    private const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
    private const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"

    private fun isApiKeyConfigured(key: String, placeholder: String): Boolean {
        return key.isNotEmpty() && key != placeholder
    }

    suspend fun getDetoxRecommendations(
        userName: String,
        screenTime: Float,
        sleepLog: Float,
        screenTimeGoal: Float,
        mood: String?
    ): String = withContext(Dispatchers.IO) {
        val groqApiKey = try { BuildConfig.GROQ_API_KEY } catch (e: Exception) { "" }
        val geminiApiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        val isGroqEnabled = isApiKeyConfigured(groqApiKey, "MY_GROQ_API_KEY")
        val isGeminiEnabled = isApiKeyConfigured(geminiApiKey, "MY_GEMINI_API_KEY")

        val prompt = """
            Generate personalized digital detox recommendations and screen time/balance offline activity suggestions.
            Here is the user profile and daily stats:
            - Name: $userName
            - Today's Screen Time: $screenTime hours
            - Screen Time Limit/Goal: $screenTimeGoal hours
            - Sleep Logged last night: $sleepLog hours
            - Reported Current Mood: ${mood ?: "Not reported"}
            
            Based on these figures, please deliver 3 distinct, highly actionable, personalized detox tricks, and offline activity proposals. 
            Format the output beautifully with clear bullet points using emojis. Be friendly, empathetic, and encouraging. Focus on local/physical replacement activities. Avoid vague suggestions, make them highly specific to their state (e.g. if they slept low, suggest sleep hygiene tips; if screen time is high, suggest immediate eye strain reliefs and green-space strolls). Keep the response concise, under 200 words.
        """.trimIndent()

        if (isGroqEnabled) {
            Log.d(TAG, "Calling Groq API for detox recommendations...")
            val messages = listOf(
                GroqMessage(role = "system", content = "You are Usra AI, an empathetic digital wellness companion."),
                GroqMessage(role = "user", content = prompt)
            )
            val request = GroqRequest(
                model = DEFAULT_GROQ_MODEL,
                messages = messages,
                temperature = 0.7f
            )
            try {
                val response = RetrofitClient.groqService.generateChatCompletion(
                    authorization = "Bearer $groqApiKey",
                    request = request
                )
                val responseText = response.choices?.firstOrNull()?.message?.content
                if (!responseText.isNullOrBlank()) {
                    return@withContext responseText
                } else {
                    return@withContext "No recommendations returned from Groq. Please try again!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Groq API", e)
                return@withContext "Error connecting to Groq. Please verify your GROQ_API_KEY in the Secrets panel. Details: ${e.localizedMessage}"
            }
        } else if (isGeminiEnabled) {
            Log.d(TAG, "Calling Gemini API for detox recommendations...")
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "You are Usra AI, an empathetic digital wellness companion.")))
            )
            try {
                val response = RetrofitClient.geminiService.generateContent(
                    model = DEFAULT_GEMINI_MODEL,
                    apiKey = geminiApiKey,
                    request = request
                )
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    return@withContext responseText
                } else {
                    return@withContext "No recommendations found. Please adjust your logs and try again!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini API", e)
                return@withContext "Could not load AI suggestions right now. Make sure you have configured a valid GEMINI_API_KEY in your Secrets panel under Settings."
            }
        } else {
            return@withContext "API Configuration Error: Please configure either GROQ_API_KEY or GEMINI_API_KEY in your Secrets panel under Settings."
        }
    }

    suspend fun getFamilyRecommendations(
        userName: String,
        stressLevel: Int,
        currentTime: String
    ): String = withContext(Dispatchers.IO) {
        val groqApiKey = try { BuildConfig.GROQ_API_KEY } catch (e: Exception) { "" }
        val geminiApiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        val isGroqEnabled = isApiKeyConfigured(groqApiKey, "MY_GROQ_API_KEY")
        val isGeminiEnabled = isApiKeyConfigured(geminiApiKey, "MY_GEMINI_API_KEY")

        val prompt = """
            Generate personalized, warm, family-oriented screen-free activities to build connection and relieve stress.
            Here is the current state for family connection:
            - User Name: $userName
            - User's Stress Level: $stressLevel out of 10 (where 10 is extremely stressed)
            - Current Local Time: $currentTime
            
            Please deliver 3 distinct, physical, screen-free family or partner bonding activity recommendations tailored to this time of day and stress level.
            - If it is late at night, suggest tranquil/cozy indoor activities (e.g. ambient lighting conversations, safe stretches, quiet memory books, acoustic music sessions).
            - If stress levels are very high, suggest ultra-low effort, highly relaxing techniques that do not demand major physical stamina.
            - If it is daytime, propose light, stimulating physical replacement outdoor/indoor collaborative games, courtyard walks, or creative baking tasks.
            
            Format the response beautifully in Markdown with clear bullet points, encouraging emojis, and clear indicators showing why these choices suit their stress factor at this hour. Be highly specific. Keep the entire response under 220 words.
        """.trimIndent()

        if (isGroqEnabled) {
            Log.d(TAG, "Calling Groq API for family recommendations...")
            val messages = listOf(
                GroqMessage(role = "system", content = "You are Usra AI, an empathetic senior family relation and digital mindfulness coach."),
                GroqMessage(role = "user", content = prompt)
            )
            val request = GroqRequest(
                model = DEFAULT_GROQ_MODEL,
                messages = messages,
                temperature = 0.7f
            )
            try {
                val response = RetrofitClient.groqService.generateChatCompletion(
                    authorization = "Bearer $groqApiKey",
                    request = request
                )
                val responseText = response.choices?.firstOrNull()?.message?.content
                if (!responseText.isNullOrBlank()) {
                    return@withContext responseText
                } else {
                    return@withContext "No recommendations returned from Groq. Please try again!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Groq API", e)
                return@withContext "Error connecting to Groq. Please verify your GROQ_API_KEY in the Secrets panel. Details: ${e.localizedMessage}"
            }
        } else if (isGeminiEnabled) {
            Log.d(TAG, "Calling Gemini API for family recommendations...")
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "You are Usra AI, an empathetic senior family relation and digital mindfulness coach.")))
            )
            try {
                val response = RetrofitClient.geminiService.generateContent(
                    model = DEFAULT_GEMINI_MODEL,
                    apiKey = geminiApiKey,
                    request = request
                )
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    return@withContext responseText
                } else {
                    return@withContext "No recommendations found. Please try again!"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini API", e)
                return@withContext "Could not load AI family suggestions right now. Ensure you have configured a valid GEMINI_API_KEY in your Secrets panel under Settings."
            }
        } else {
            return@withContext "API Configuration Error: Please configure either GROQ_API_KEY or GEMINI_API_KEY in your Secrets panel under Settings."
        }
    }

    suspend fun getAIQuests(
        userName: String,
        screenTime: Float,
        sleepLog: Float,
        screenTimeGoal: Float,
        mood: String?,
        familyStress: Int
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val groqApiKey = try { BuildConfig.GROQ_API_KEY } catch (e: Exception) { "" }
        val geminiApiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }

        val isGroqEnabled = isApiKeyConfigured(groqApiKey, "MY_GROQ_API_KEY")
        val isGeminiEnabled = isApiKeyConfigured(geminiApiKey, "MY_GEMINI_API_KEY")

        val prompt = """
            You are Usra AI, an empathetic senior digital wellness and family relationship companion.
            Based on the following personal and household metrics, generate exactly two highly relevant, creative, screen-free "Reconnection Quests" for this user and their family.
            
            Metrics:
            - User Name: $userName
            - Today's Screen Time: $screenTime hours (Goal Limit: $screenTimeGoal)
            - Last Night's Sleep Logged: $sleepLog hours
            - Current Mood: ${mood ?: "Not reported"}
            - Family Stress Index: $familyStress out of 10
            - Household State: Mom (Working Parent, 4.0 hrs screen, mood: Okay), Dad (Work Mode, 5.0 hrs screen, mood: Happy)
            
            Please deliver custom, specific offline bonding quest recommendations.
            The response must follow this EXACT structure so our mobile app can parse the keys correctly:
            QUEST_1_TITLE: [Catchy quest title starting with an emoji, max 25 characters]
            QUEST_1_SUBTITLE: [Specific actionable screen-free instruction referencing a family member, max 50 characters]
            QUEST_2_TITLE: [Catchy quest title starting with an emoji, max 25 characters]
            QUEST_2_SUBTITLE: [Specific actionable screen-free instruction referencing a family member, max 50 characters]
            
            Do not include any other text, greetings, code fences, markdown asterisks, or surrounding layout. Just output exactly those four lines.
        """.trimIndent()

        var responseText = ""

        if (isGroqEnabled) {
            Log.d(TAG, "Calling Groq API for AI quests...")
            val messages = listOf(
                GroqMessage(role = "system", content = "You are Usra AI, a precise offline habit generator."),
                GroqMessage(role = "user", content = prompt)
            )
            val request = GroqRequest(
                model = DEFAULT_GROQ_MODEL,
                messages = messages,
                temperature = 0.7f
            )
            try {
                val response = RetrofitClient.groqService.generateChatCompletion(
                    authorization = "Bearer $groqApiKey",
                    request = request
                )
                responseText = response.choices?.firstOrNull()?.message?.content ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Groq API", e)
            }
        } else if (isGeminiEnabled) {
            Log.d(TAG, "Calling Gemini API for AI quests...")
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = Content(parts = listOf(Part(text = "You are Usra AI, a precise offline habit generator.")))
            )
            try {
                val response = RetrofitClient.geminiService.generateContent(
                    model = DEFAULT_GEMINI_MODEL,
                    apiKey = geminiApiKey,
                    request = request
                )
                responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini API", e)
            }
        }

        val result = mutableMapOf<String, String>()
        if (responseText.isNotBlank()) {
            val lines = responseText.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("QUEST_1_TITLE:") -> {
                        result["title1"] = trimmed.substringAfter("QUEST_1_TITLE:").trim()
                    }
                    trimmed.startsWith("QUEST_1_SUBTITLE:") -> {
                        result["subtitle1"] = trimmed.substringAfter("QUEST_1_SUBTITLE:").trim()
                    }
                    trimmed.startsWith("QUEST_2_TITLE:") -> {
                        result["title2"] = trimmed.substringAfter("QUEST_2_TITLE:").trim()
                    }
                    trimmed.startsWith("QUEST_2_SUBTITLE:") -> {
                        result["subtitle2"] = trimmed.substringAfter("QUEST_2_SUBTITLE:").trim()
                    }
                }
            }
        }

        // Apply fallback if any key is missing
        if (!result.containsKey("title1") || result["title1"]!!.isBlank()) {
            result["title1"] = "✨ AI Boardgame Battle"
        }
        if (!result.containsKey("subtitle1") || result["subtitle1"]!!.isBlank()) {
            result["subtitle1"] = "Enjoy offline gameplay with Dad to ease Work Stress."
        }
        if (!result.containsKey("title2") || result["title2"]!!.isBlank()) {
            result["title2"] = "✨ AI Dinner Prep Assistant"
        }
        if (!result.containsKey("subtitle2") || result["subtitle2"]!!.isBlank()) {
            result["subtitle2"] = "Help Mom with device-free cooking prep to unwind."
        }

        result
    }
}
