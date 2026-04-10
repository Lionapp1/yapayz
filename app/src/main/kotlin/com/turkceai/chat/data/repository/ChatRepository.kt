package com.turkceai.chat.data.repository

import android.content.Context
import com.turkceai.chat.data.model.ChatMessage
import com.turkceai.chat.data.ml.LocalAIModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface ChatRepository {
    suspend fun sendMessage(messages: List<ChatMessage>): Result<String>
}

// OpenRouter API Models
@Serializable
data class OpenRouterRequest(
    val model: String = "deepseek/deepseek-chat:free",
    val messages: List<OpenRouterMessage>
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>? = null,
    val error: OpenRouterError? = null
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

@Serializable
data class OpenRouterError(
    val message: String
)

class ChatRepositoryImpl(
    context: Context? = null
) : ChatRepository {

    // Lokal AI Model - İnternet olmadan çalışır!
    private val localModel: LocalAIModel? = context?.let { LocalAIModel(it) }

    // OpenRouter API - ücretsiz tier mevcut
    private val API_KEY = "sk-or-v1--demo-key" // Kullanıcı kendi keyini eklemeli
    private val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"

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
                    android.util.Log.d("TurkceAI", message)
                }
            }
        }
    }


    override suspend fun sendMessage(messages: List<ChatMessage>): Result<String> {
        return try {
            val lastMessage = messages.lastOrNull()?.content ?: ""
            val conversationTexts = messages.map { it.content }
            
            // Önce lokal modeli dene (her zaman çalışır!)
            localModel?.let { model ->
                val localResponse = model.generateResponse(lastMessage, conversationTexts)
                // Eğer API çalışmazsa lokal yanıt kullanılır
                return@try Result.success(localResponse)
            }

            // API çağrısı dene (opsiyonel)
            val apiMessages = messages.map { msg ->
                OpenRouterMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.content
                )
            }

            val request = OpenRouterRequest(messages = apiMessages)

            val response = client.post(BASE_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $API_KEY")
                header("HTTP-Referer", "https://turkceai.app")
                header("X-Title", "TurkceAI Chat")
                setBody(request)
            }

            when (response.status.value) {
                200 -> {
                    val apiResponse: OpenRouterResponse = response.body()
                    val content = apiResponse.choices?.firstOrNull()?.message?.content
                        ?: generateLocalResponse(lastMessage)
                    Result.success(content)
                }
                401 -> {
                    // API key geçersiz, demo moda devam et
                    Result.success(generateLocalResponse(lastMessage))
                }
                else -> {
                    // API hatası, demo moda devam et
                    Result.success(generateLocalResponse(lastMessage))
                }
            }
        } catch (e: Exception) {
            // İnternet yok veya hata, lokal model yanıt verir
            val lastMessage = messages.lastOrNull()?.content ?: ""
            val conversationTexts = messages.map { it.content }
            val fallbackResponse = localModel?.generateResponse(lastMessage, conversationTexts)
                ?: "Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin."
            Result.success(fallbackResponse)
        }
    }

    /**
     * Lokal model durumunu kontrol et
     */
    fun isLocalModelReady(): Boolean {
        return localModel != null
    }

    fun close() {
        client.close()
    }
}
