package com.turkceai.chat.data.repository

import com.turkceai.chat.data.model.ChatMessage
import com.turkceai.chat.data.model.ChatRequest
import com.turkceai.chat.data.model.ChatResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

interface ChatRepository {
    suspend fun sendMessage(messages: List<ChatMessage>): Result<String>
}

class ChatRepositoryImpl(
    private val apiKey: String = ""
) : ChatRepository {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("HTTP", message)
                }
            }
        }
    }

    override suspend fun sendMessage(messages: List<ChatMessage>): Result<String> {
        return try {
            val request = ChatRequest(messages = messages)
            
            // Hugging Face Inference API kullanımı
            val apiUrl = "https://api-inference.huggingface.co/models/deepseek-ai/deepseek-llm-7b-chat"
            
            val headers = mutableMapOf<String, String>()
            if (apiKey.isNotBlank()) {
                headers["Authorization"] = "Bearer $apiKey"
            }

            val response = client.post(apiUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    headers.forEach { (key, value) ->
                        append(key, value)
                    }
                }
                setBody(request)
            }

            when (response.status.value) {
                200 -> {
                    val chatResponse: ChatResponse = response.body()
                    val content = chatResponse.choices?.firstOrNull()?.message?.content
                        ?: "Üzgünüm, bir yanıt oluşturamadım."
                    Result.success(content)
                }
                503 -> {
                    // Model loading
                    Result.failure(Exception("Model yükleniyor, lütfen birkaç saniye bekleyin ve tekrar deneyin."))
                }
                else -> {
                    val errorBody = response.body<String>()
                    Result.failure(Exception("API Hatası: ${response.status.value} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}
