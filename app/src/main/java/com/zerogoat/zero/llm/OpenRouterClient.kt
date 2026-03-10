package com.zerogoat.zero.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * OpenRouter API client — universal gateway to 200+ LLM models.
 * Single API key gives access to all providers.
 * https://openrouter.ai/docs
 */
class OpenRouterClient(
    private val apiKey: String,
    private val modelId: String = ModelRegistry.defaultModel.id
) : LLMClient {

    companion object {
        private const val TAG = "OpenRouter"
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override val providerName = "OpenRouter"

    override suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): LLMResponse {
        val body = buildRequestBody(systemPrompt, userMessage, conversationHistory, stream = false)

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://zerogoat.app")
            .addHeader("X-Title", "ZeroGoat")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        response.close()

        return parseResponse(responseBody)
    }

    override suspend fun chatWithVision(
        systemPrompt: String,
        userMessage: String,
        imageBase64: String,
        imageMimeType: String
    ): LLMResponse {
        val messagesJson = JSONArray()

        if (systemPrompt.isNotEmpty()) {
            messagesJson.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }

        // Add user message with image
        val content = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", userMessage))
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().put("url", "data:$imageMimeType;base64,$imageBase64"))
            })
        }
        messagesJson.put(JSONObject().put("role", "user").put("content", content))

        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", messagesJson)
            put("max_tokens", 1024)
            put("temperature", 0.2)
        }

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://zerogoat.app")
            .addHeader("X-Title", "ZeroGoat")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        response.close()

        return parseResponse(responseBody)
    }

    /** Stream tokens in real-time via SSE */
    override fun chatStream(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): Flow<String> = flow {
        val body = buildRequestBody(systemPrompt, userMessage, conversationHistory, stream = true)

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://zerogoat.app")
            .addHeader("X-Title", "ZeroGoat")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return@flow))

        reader.useLines { lines ->
            for (line in lines) {
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break

                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices") ?: continue
                        val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                        val content = delta.optString("content", "")
                        if (content.isNotEmpty()) {
                            emit(content)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Stream parse error: ${e.message}")
                    }
                }
            }
        }
        response.close()
    }.flowOn(Dispatchers.IO)

    override fun estimateTokens(text: String): Int {
        return text.length / 4  // Rough estimate
    }

    private fun buildRequestBody(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        stream: Boolean
    ): JSONObject {
        val messagesJson = JSONArray()

        if (systemPrompt.isNotEmpty()) {
            messagesJson.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }

        for (msg in conversationHistory) {
            messagesJson.put(JSONObject().put("role", msg.role).put("content", msg.content))
        }
        
        // Add current message
        messagesJson.put(JSONObject().put("role", "user").put("content", userMessage))

        return JSONObject().apply {
            put("model", modelId)
            put("messages", messagesJson)
            put("max_tokens", 1024)
            put("temperature", 0.2)
            put("stream", stream)
            // Request JSON mode for structured output
            if (!stream) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
        }
    }

    private fun parseResponse(body: String): LLMResponse {
        return try {
            val json = JSONObject(body)
            
            // Check for OpenRouter API errors (e.g., quota exceeded, invalid key)
            if (json.has("error")) {
                val errorObj = json.getJSONObject("error")
                val msg = errorObj.optString("message", "Unknown OpenRouter Error")
                return LLMResponse("", error = msg)
            }
            
            val choices = json.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")

            val usage = json.optJSONObject("usage")
            val inputTokens = usage?.optInt("prompt_tokens", 0) ?: 0
            val outputTokens = usage?.optInt("completion_tokens", 0) ?: 0

            LLMResponse(content, inputTokens, outputTokens)
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}, body: ${body.take(200)}")
            LLMResponse("", error = "Parse error: ${e.message}")
        }
    }
}
