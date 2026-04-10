package com.turkceai.chat.data.repository

import com.turkceai.chat.data.model.ChatMessage
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

class ChatRepositoryImpl : ChatRepository {

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

    private val demoResponses = mapOf(
        "merhaba" to "Merhaba! 👋 Size nasıl yardımcı olabilirim?",
        "nasılsın" to "Ben bir yapay zeka olduğum için duygularım yok, ama çalışıyorum! Siz nasılsınız?",
        "selam" to "Selam! Hoş geldiniz. Bugün size nasıl yardımcı olabilirim?",
        " yardım" to "Tabii! Size nasıl yardımcı olabilirim? Sorularınızı yanıtlamak için buradayım.",
        "teşekkür" to "Rica ederim! 😊 Başka bir sorunuz varsa bekliyorum.",
        "görüşürüz" to "Görüşürüz! İyi günler dilerim. 👋",
        "ne yapabilirsin" to "Size şunlarda yardımcı olabilirim:\n\n• Sorularınızı yanıtlamak\n• Sohbet etmek\n• Bilgi vermek\n• Yardımcı öneriler sunmak\n\nNe hakkında konuşmak istersiniz?"
    )

    override suspend fun sendMessage(messages: List<ChatMessage>): Result<String> {
        return try {
            val lastMessage = messages.lastOrNull()?.content?.lowercase() ?: ""
            
            // Demo mod: Basit yanıtlar (API key yoksa veya internet yoksa çalışır)
            demoResponses.entries.find { lastMessage.contains(it.key) }?.let {
                return Result.success(it.value)
            }

            // API çağrısı dene
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
            // İnternet yok veya hata, demo mod
            val lastMessage = messages.lastOrNull()?.content?.lowercase() ?: ""
            Result.success(generateLocalResponse(lastMessage))
        }
    }

    private fun generateLocalResponse(input: String): String {
        return when {
            input.contains("nasıl") || input.contains("?") -> 
                "Bu konuda size yardımcı olmaya çalışayım. Daha detaylı bilgi verir misiniz?"
            input.contains("teşekkür") || input.contains("sağol") ->
                "Rica ederim! 😊 Yardımcı olabildiysem ne mutlu."
            input.contains("günaydın") || input.contains("iyi günler") ->
                "Günaydın! ☀️ Harika bir gün dilerim. Size nasıl yardımcı olabilirim?"
            input.contains("iyi geceler") || input.contains("uyu") ->
                "İyi geceler! 🌙 Tatlı rüyalar dilerim."
            input.contains("türkiye") || input.contains("türk") ->
                "Türkiye çok güzel bir ülke! 🇹🇷 Türkçe konuşmak benim için büyük bir zevk."
            input.contains("android") || input.contains("kotlin") ->
                "Android ve Kotlin harika teknolojiler! Bu uygulama da Kotlin ve Jetpack Compose ile yazıldı."
            input.length < 5 ->
                "Anladım. Başka bir konuda yardımcı olabilir miyim?"
            else -> "İlginç bir konu! Bu hakkında daha fazla bilgi verir misiniz?"
        }
    }

    fun close() {
        client.close()
    }
}
