package com.turkceai.chat.data.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Lokal AI Model - TensorFlow Lite ile çalışır
 * İnternet olmadan bile cihazda çalışır
 */
class LocalAIModel(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    
    // Basit Türkçe yanıt üreticisi - Template tabanlı
    private val responseTemplates = mapOf(
        "greeting" to listOf(
            "Merhaba! 👋 Size nasıl yardımcı olabilirim?",
            "Selam! Hoş geldiniz. Bugün nasıl hissediyorsunuz?",
            "Merhabalar! Sohbet etmek için buradayım.",
            "Selamlar! 🌟 Size nasıl destek olabilirim?"
        ),
        "how_are_you" to listOf(
            "Ben bir AI olduğum için duygularım yok, ama sistemlerim tamamen çalışır durumda! Siz nasılsınız?",
            "Teşekkürler, ben hep iyiyim! Sizin gününüz nasıl geçiyor?",
            "Benim için her zaman iyi! Size nasıl yardımcı olabilirim?"
        ),
        "thanks" to listOf(
            "Rica ederim! 😊 Yardımcı olabildiysem ne mutlu bana.",
            "Ne demek, her zaman! Başka bir sorunuz var mı?",
            "Gladly! İsterseniz başka konularda da sohbet edebiliriz."
        ),
        "goodbye" to listOf(
            "Görüşürüz! 👋 Kendinize iyi bakın, iyi günler!",
            "Hoşça kalın! Tekrar görüşmek üzere.",
            "Allahaısmarladık! Güzel bir gün dilerim. 🌟"
        ),
        "help" to listOf(
            """
            |Size şöyle yardımcı olabilirim:
            |
            |💬 Sohbet ve muhabbet
            |📚 Bilgi ve araştırma
            |💡 Fikir ve öneriler
            |📝 Yazı yazma desteği
            |❓ Sorularınızı yanıtlama
            |
            |Ne hakkında konuşmak istersiniz?
            """.trimMargin(),
            
            "Tabii, buyrun! Size nasıl yardımcı olabilirim? Sorularınızı dinliyorum."
        ),
        "weather" to listOf(
            "Hava durumu için internet bağlantısı gerekli. Ancak genel olarak dışarı çıkmadan önce hazırlıklı olun! 🌤️",
            "Maalesef lokal olarak hava verisine erişemiyorum. Ama dışarı çıkmadan önce pencere dışına bakmayı unutmayın! 😊"
        ),
        "time" to listOf(
            "Şu an saat bilgisini lokal olarak göremiyorum, ancak telefonunuzun saat uygulamasına bakabilirsiniz! ⏰",
            "Zaman her zaman akıp gidiyor! Şimdiki zamanı değerlendirin. 🕐"
        ),
        "identity" to listOf(
            "Ben TürkçeAI, sizin yapay zeka asistanınızım! 🇹🇷 Kotlin ve Jetpack Compose ile geliştirildim, TensorFlow Lite ile çalışıyorum."
        ),
        "default" to listOf(
            "İlginç bir konu! Bu hakkında daha fazla detay verir misiniz?",
            "Anladım. Sizinle bu konuyu daha derinlemesine konuşabiliriz.",
            "Bu konuda size nasıl yardımcı olabilirim?",
            "Merak uyandırıcı! Başka neler öğrenmek istersiniz?",
            "Size bu konuda destek olmaya çalışayım."
        )
    )
    
    // Konu analizi için kelime grupları
    private val keywordPatterns = mapOf(
        "greeting" to listOf("merhaba", "selam", "hey", "hoşgeldin", "günaydın", "iyi günler", "iyi akşamlar"),
        "how_are_you" to listOf("nasılsın", "naber", "nasıl gidiyor", "iyi misin", "keyfin nasıl"),
        "thanks" to listOf("teşekkür", "sağol", "eyvallah", "çok sağol", "yardımın için"),
        "goodbye" to listOf("görüşürüz", "hoşça kal", "bay bay", "allahai smarladık", "güle güle"),
        "help" to listOf("yardım", "help", "destek", "nasıl yaparım", "ne yapmalıyım"),
        "weather" to listOf("hava", "weather", "yağmur", "güneş", "sıcaklık", "soğuk"),
        "time" to listOf("saat", "zaman", "time", "tarih", "gün", "bugün ne"),
        "identity" to listOf("kimsin", "sen kimsin", "adın ne", "nedir bu", "what are you", "who are you")
    )
    
    init {
        initializeModel()
    }
    
    private fun initializeModel() {
        try {
            // Şu an için template tabanlı çalışıyor
            // İleride TFLite modeli eklenebilir
            isInitialized = true
            Log.d("LocalAIModel", "Model initialized successfully (template-based)")
        } catch (e: Exception) {
            Log.e("LocalAIModel", "Failed to initialize model", e)
            isInitialized = false
        }
    }
    
    /**
     * Girdi metnine göre akıllı yanıt üret
     */
    fun generateResponse(input: String, conversationHistory: List<String> = emptyList()): String {
        if (!isInitialized) {
            return "Model henüz hazır değil, lütfen bekleyin..."
        }
        
        val normalizedInput = input.lowercase().trim()
        
        // Konu tespiti
        val detectedTopic = detectTopic(normalizedInput)
        
        // Yanıt seç
        val responses = responseTemplates[detectedTopic] ?: responseTemplates["default"]!!
        
        // Rastgele ama tutarlı yanıt seç (history'e bakarak)
        val seed = (conversationHistory.size + normalizedInput.length) % responses.size
        var response = responses[seed]
        
        // Konuşma geçmişine göre kişiselleştir
        if (conversationHistory.isNotEmpty()) {
            val lastExchange = conversationHistory.takeLast(2).joinToString(" ")
            if (lastExchange.contains("?")) {
                response += "\n\nBaşka sorularınız var mı? 🤔"
            }
        }
        
        return response
    }
    
    /**
     * Girdi metninden konu tespiti yap
     */
    private fun detectTopic(input: String): String {
        for ((topic, keywords) in keywordPatterns) {
            for (keyword in keywords) {
                if (input.contains(keyword)) {
                    return topic
                }
            }
        }
        return "default"
    }
    
    /**
     * TensorFlow Lite modelini yükle (ileride kullanılabilir)
     */
    @Suppress("unused")
    private fun loadModelFile(modelName: String): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(modelName)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: IOException) {
            Log.e("LocalAIModel", "Error loading model: $modelName", e)
            null
        }
    }
    
    /**
     * Basit metin analizi - Duygu tespiti
     */
    fun analyzeSentiment(text: String): String {
        val positiveWords = listOf("iyi", "güzel", "harika", "muhteşem", "teşekkür", "sevindim", "mutlu")
        val negativeWords = listOf("kötü", "üzgün", "sinirli", "yoruldum", "sıkıldım", "problemli")
        
        val normalized = text.lowercase()
        val posCount = positiveWords.count { normalized.contains(it) }
        val negCount = negativeWords.count { normalized.contains(it) }
        
        return when {
            posCount > negCount -> "positive"
            negCount > posCount -> "negative"
            else -> "neutral"
        }
    }
    
    fun close() {
        interpreter?.close()
    }
}

/**
 * Lokal model durumu
 */
sealed class ModelStatus {
    object Loading : ModelStatus()
    object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}
