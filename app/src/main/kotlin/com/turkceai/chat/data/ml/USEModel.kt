package com.turkceai.chat.data.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Universal Sentence Encoder Multilingual Model
 * Google'ın çok dilli cümle embedding modeli
 * Türkçe dahil 100+ dil destekler
 */
class USEModel(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    
    // Model dosya adı (TFLite formatında)
    private val MODEL_FILE = "use_multilingual_v2.tflite"
    
    init {
        try {
            loadModel()
        } catch (e: Exception) {
            Log.e("USEModel", "Model yüklenemedi: ${e.message}")
            isModelLoaded = false
        }
    }
    
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            if (modelBuffer != null) {
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                isModelLoaded = true
                Log.i("USEModel", "Model başarıyla yüklendi!")
            }
        } catch (e: Exception) {
            Log.e("USEModel", "Model yükleme hatası", e)
            isModelLoaded = false
        }
    }
    
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd("models/$MODEL_FILE")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e("USEModel", "Model dosyası bulunamadı: $MODEL_FILE", e)
            null
        }
    }
    
    /**
     * Cümle embedding vektörü oluştur
     * 512 boyutlu vektör döner
     */
    fun getEmbedding(text: String): FloatArray? {
        if (!isModelLoaded || interpreter == null) {
            return null
        }
        
        return try {
            // Model input: [1] shape'li string array
            val input = arrayOf(text)
            
            // Model output: [1][512] shape'li float array
            val output = Array(1) { FloatArray(512) }
            
            interpreter?.run(input, output)
            
            output[0]
        } catch (e: Exception) {
            Log.e("USEModel", "Embedding oluşturma hatası", e)
            null
        }
    }
    
    /**
     * İki metin arasındaki benzerlik skoru (0-1 arası)
     * Cosine similarity kullanır
     */
    fun getSimilarity(text1: String, text2: String): Float {
        val embedding1 = getEmbedding(text1) ?: return 0f
        val embedding2 = getEmbedding(text2) ?: return 0f
        
        return cosineSimilarity(embedding1, embedding2)
    }
    
    /**
     * En iyi eşleşen yanıtı bul
     */
    fun findBestResponse(input: String, responses: Map<String, List<String>>): String? {
        var bestMatch: String? = null
        var highestScore = 0.5f // Eşik değer
        
        for ((pattern, responseList) in responses) {
            val similarity = getSimilarity(input, pattern)
            if (similarity > highestScore) {
                highestScore = similarity
                // Rastgele bir yanıt seç
                bestMatch = responseList.random()
            }
        }
        
        Log.d("USEModel", "En iyi eşleşme: $bestMatch (skor: $highestScore)")
        return bestMatch
    }
    
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        
        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2))
        } else {
            0f
        }
    }
    
    fun isLoaded(): Boolean = isModelLoaded
    
    fun close() {
        interpreter?.close()
        isModelLoaded = false
    }
}
