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
import android.util.Log

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

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiService {
    private const val TAG = "GeminiService"
    private const val DEFAULT_MODEL = "gemini-3.5-flash"

    suspend fun getDetoxRecommendations(
        userName: String,
        screenTime: Float,
        sleepLog: Float,
        screenTimeGoal: Float,
        mood: String?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return@withContext "API Configuration Error: Please configure a valid GEMINI_API_KEY in your Secrets panel under Settings."
        }

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

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are Suqoon AI, an empathetic digital wellness companion.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = DEFAULT_MODEL,
                apiKey = apiKey,
                request = request
            )
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!responseText.isNullOrBlank()) {
                responseText
            } else {
                "No recommendations found. Please adjust your logs and try again!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            "Could not load AI suggestions right now. Make sure you have configured a valid GEMINI_API_KEY in the Secrets panel, then try again! Error details: ${e.localizedMessage}"
        }
    }
}
