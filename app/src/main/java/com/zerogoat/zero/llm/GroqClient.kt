package com.zerogoat.zero.llm

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Groq API client — ultra-fast inference (fastest LLM API).
 * Supports Llama, Mixtral, Gemma at blazing speed.
 */
class GroqClient(private val apiKey: String) : LLMClient {

    companion object {
        private const val TAG = "Groq"
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override val providerName = "Groq"

    override suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): LLMResponse {
        val messagesJson = JSONArray()
        if (systemPrompt.isNotEmpty()) {
            messagesJson.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        for (msg in conversationHistory) {
            messagesJson.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }
        messagesJson.put(JSONObject().put("role", "user").put("content", userMessage))

        val body = JSONObject().apply {
            put("model", DEFAULT_MODEL)
            put("messages", messagesJson)
            put("max_tokens", 1024)
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        response.close()

        return try {
            val json = JSONObject(responseBody)
            val content = json.getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content")
            val usage = json.optJSONObject("usage")
            LLMResponse(
                text = content,
                inputTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
                outputTokens = usage?.optInt("completion_tokens", 0) ?: 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            LLMResponse("", error = e.message)
        }
    }

    override suspend fun chatWithVision(
        systemPrompt: String,
        userMessage: String,
        imageBase64: String,
        imageMimeType: String
    ): LLMResponse {
        // Groq doesn't support vision natively — fall back to text
        return chat(systemPrompt, userMessage, emptyList())
    }

    override fun estimateTokens(text: String): Int = text.length / 4
}
