package com.zerogoat.zero.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Anthropic Claude API client — supports Claude 3.5 Sonnet and Haiku.
 */
class AnthropicClient(private val apiKey: String) : LLMClient {

    override val providerName = "Anthropic"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.anthropic.com/v1"
    private val model = "claude-3-5-haiku-latest"

    override suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): LLMResponse = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                for (msg in conversationHistory) {
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            val body = JSONObject().apply {
                put("model", model)
                put("system", systemPrompt)
                put("messages", messages)
                put("max_tokens", 1024)
                put("temperature", 0.1)
            }

            val response = executeRequest(body.toString())
            parseResponse(response)
        } catch (e: Exception) {
            LLMResponse(text = "", error = "Anthropic error: ${e.message}")
        }
    }

    override suspend fun chatWithVision(
        systemPrompt: String,
        userMessage: String,
        imageBase64: String,
        imageMimeType: String
    ): LLMResponse = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", imageMimeType)
                                put("data", imageBase64)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userMessage)
                        })
                    })
                })
            }

            val body = JSONObject().apply {
                put("model", model)
                put("system", systemPrompt)
                put("messages", messages)
                put("max_tokens", 1024)
                put("temperature", 0.1)
            }

            val response = executeRequest(body.toString())
            parseResponse(response)
        } catch (e: Exception) {
            LLMResponse(text = "", error = "Anthropic Vision error: ${e.message}")
        }
    }

    private fun executeRequest(body: String): String {
        val request = Request.Builder()
            .url("$baseUrl/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response")
    }

    private fun parseResponse(responseBody: String): LLMResponse {
        val json = JSONObject(responseBody)

        if (json.optString("type") == "error") {
            val error = json.getJSONObject("error")
            return LLMResponse(text = "", error = error.getString("message"))
        }

        val content = json.getJSONArray("content")
            .getJSONObject(0)
            .getString("text")

        val usage = json.optJSONObject("usage")
        val inputTokens = usage?.optInt("input_tokens", 0) ?: 0
        val outputTokens = usage?.optInt("output_tokens", 0) ?: 0

        return LLMResponse(
            text = content,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = inputTokens + outputTokens
        )
    }
}
