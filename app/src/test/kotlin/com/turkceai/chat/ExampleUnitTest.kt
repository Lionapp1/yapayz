package com.turkceai.chat

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun messageCreation_isCorrect() {
        val message = com.turkceai.chat.data.model.Message(
            content = "Test mesajı",
            isFromUser = true
        )
        assertEquals("Test mesajı", message.content)
        assertTrue(message.isFromUser)
        assertFalse(message.isError)
    }

    @Test
    fun chatMessageSerialization() {
        val chatMessage = com.turkceai.chat.data.model.ChatMessage(
            role = "user",
            content = "Merhaba"
        )
        assertEquals("user", chatMessage.role)
        assertEquals("Merhaba", chatMessage.content)
    }
}
