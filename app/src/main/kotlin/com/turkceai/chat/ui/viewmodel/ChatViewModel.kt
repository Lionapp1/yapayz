package com.turkceai.chat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turkceai.chat.data.model.ChatMessage
import com.turkceai.chat.data.model.Message
import com.turkceai.chat.data.repository.ChatRepository
import com.turkceai.chat.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val chatRepository: ChatRepository = ChatRepositoryImpl(application.applicationContext)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        // Kullanıcı mesajını ekle
        val userMessage = Message(content = content, isFromUser = true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Sohbet geçmişini API formatına dönüştür
            val chatHistory = _messages.value.map { msg ->
                ChatMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.content
                )
            }

            val result = chatRepository.sendMessage(chatHistory)

            result.onSuccess { response ->
                val aiMessage = Message(content = response, isFromUser = false)
                _messages.value = _messages.value + aiMessage
            }.onFailure { exception ->
                _error.value = exception.message ?: "Bir hata oluştu"
                val errorMessage = Message(
                    content = exception.message ?: "Bir hata oluştu",
                    isFromUser = false,
                    isError = true
                )
                _messages.value = _messages.value + errorMessage
            }

            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (chatRepository is ChatRepositoryImpl) {
            chatRepository.close()
        }
    }
}
