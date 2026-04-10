package com.turkceai.chat.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

@Serializable
data class ChatRequest(
    val model: String = "deepseek-ai/deepseek-llm-7b-chat",
    val messages: List<ChatMessage>
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>? = null,
    val error: String? = null
)

@Serializable
data class Choice(
    val message: ChatMessage,
    val finish_reason: String? = null
)
