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
 * OpenAI API client — supports GPT-4o-mini (cheap) and GPT-4o (vision).
 */
class OpenAIClient(private val apiKey: String) : LLMClient {

    override val providerName = "OpenAI"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.openai.com/v1"
    private val textModel = "gpt-4o-mini"
    private val visionModel = "gpt-4o"

    override suspend fun chat(
        systemPrompt: String,
        userMessage: String,
        conversationHistory: List<ChatMessage>
    ): LLMResponse = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
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
                put("model", textModel)
                put("messages", messages)
                put("temperature", 0.1)
                put("max_tokens", 1024)
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }

            val response = executeRequest(body.toString())
            parseResponse(response)
        } catch (e: Exception) {
            LLMResponse(text = "", error = "OpenAI error: ${e.message}")
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
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", userMessage)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:$imageMimeType;base64,$imageBase64")
                                put("detail", "low")
                            })
                        })
                    })
                })
            }

            val body = JSONObject().apply {
                put("model", visionModel)
                put("messages", messages)
                put("temperature", 0.1)
                put("max_tokens", 1024)
            }

            val response = executeRequest(body.toString())
            parseResponse(response)
        } catch (e: Exception) {
            LLMResponse(text = "", error = "OpenAI Vision error: ${e.message}")
        }
    }

    private fun executeRequest(body: String): String {
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response")
    }

    private fun parseResponse(responseBody: String): LLMResponse {
        val json = JSONObject(responseBody)

        if (json.has("error")) {
            val error = json.getJSONObject("error")
            return LLMResponse(text = "", error = error.getString("message"))
        }

        val content = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        val usage = json.optJSONObject("usage")
        val inputTokens = usage?.optInt("prompt_tokens", 0) ?: 0
        val outputTokens = usage?.optInt("completion_tokens", 0) ?: 0

        return LLMResponse(
            text = content,
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = inputTokens + outputTokens
        )
    }
}
