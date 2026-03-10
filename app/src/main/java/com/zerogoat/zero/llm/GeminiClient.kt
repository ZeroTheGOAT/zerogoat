package com.zerogoat.zero.llm

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google Gemini API client — optimized for Gemini 2.0 Flash (cheapest, fastest).
 */
class GeminiClient(private val apiKey: String) : LLMClient {

    override val providerName = "Gemini"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val model = "gemini-2.0-flash"

    override suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): LLMResponse = withContext(Dispatchers.IO) {
        try {
            val body = buildTextRequestBody(systemPrompt, userMessage, conversationHistory)
            val response = executeRequest(body)
            parseResponse(response)
        } catch (e: Exception) {
            LLMResponse(text = "", error = "Gemini API error: ${e.message}")
        }
    }

    override suspend fun chatWithVision(
        systemPrompt: String,
        userMessage: String,
        imageBase64: String,
        imageMimeType: String
    ): LLMResponse = withContext(Dispatchers.IO) {
        try {
            val body = buildVisionRequestBody(systemPrompt, userMessage, imageBase64, imageMimeType)
            val response = executeRequest(body)
            parseResponse(response)
        } catch (e: Exception) {
            LLMResponse(text = "", error = "Gemini Vision error: ${e.message}")
        }
    }

    private fun buildTextRequestBody(
        systemPrompt: String,
        userMessage: String,
        history: List<ChatMessage>
    ): String {
        val json = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })

            val contents = JSONArray()
            for (msg in history) {
                contents.put(JSONObject().apply {
                    put("role", if (msg.role == "assistant") "model" else "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    })
                })
            }
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userMessage) })
                })
            })
            put("contents", contents)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 1024)
                put("responseMimeType", "application/json")
            })
        }
        return json.toString()
    }

    private fun buildVisionRequestBody(
        systemPrompt: String,
        userMessage: String,
        imageBase64: String,
        mimeType: String
    ): String {
        val json = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userMessage) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", mimeType)
                                put("data", imageBase64)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 1024)
            })
        }
        return json.toString()
    }

    private fun executeRequest(body: String): String {
        val url = "$baseUrl/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response")
    }

    private fun parseResponse(responseBody: String): LLMResponse {
        val json = JSONObject(responseBody)

        // Check for errors
        if (json.has("error")) {
            val error = json.getJSONObject("error")
            return LLMResponse(text = "", error = error.getString("message"))
        }

        val candidates = json.getJSONArray("candidates")
        if (candidates.length() == 0) {
            return LLMResponse(text = "", error = "No candidates in response")
        }

        val content = candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        // Parse token usage
        val usage = json.optJSONObject("usageMetadata")
        val inputTokens = usage?.optInt("promptTokenCount", 0) ?: 0
        val outputTokens = usage?.optInt("candidatesTokenCount", 0) ?: 0

        return LLMResponse(
            text = content,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = inputTokens + outputTokens
        )
    }
}
