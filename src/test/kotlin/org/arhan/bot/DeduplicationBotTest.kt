package org.arhan.bot

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.PhotoSize
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.File
import org.telegram.telegrambots.meta.api.methods.GetFile
import java.io.Serializable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeduplicationBotTest {
    private lateinit var bot: TestableDeduplicationBot
    private lateinit var update: Update
    private lateinit var message: Message
    private lateinit var user: User

    private fun createUpdateMock(messageText: String = "test message"): Update {
        val mockUser = mockk<User> {
            every { id } returns 123L
            every { userName } returns "testUser"
        }

        val mockMessage = mockk<Message> {
            every { text } returns messageText
            every { from } returns mockUser
            every { chatId } returns 456L
            every { messageId } returns 789
            every { hasText() } returns true
            every { hasPhoto() } returns false
        }

        return mockk {
            every { hasMessage() } returns true
            every { getMessage() } returns mockMessage
            every { message } returns mockMessage
        }
    }

    private fun createPhotoUpdateMock(fileId: String = "test_file_id"): Update {
        val mockUser = mockk<User> {
            every { id } returns 123L
            every { userName } returns "testUser"
        }

        val mockPhoto = mockk<PhotoSize> {
            every { this@mockk.fileId } returns fileId
        }

        val mockMessage = mockk<Message> {
            every { from } returns mockUser
            every { chatId } returns 456L
            every { messageId } returns 789
            every { text } returns null
            every { caption } returns null
            every { hasText() } returns false
            every { hasPhoto() } returns true
            every { hasAnimation() } returns false
            every { photo } returns listOf(mockPhoto)
        }

        return mockk {
            every { hasMessage() } returns true
            every { getMessage() } returns mockMessage
            every { message } returns mockMessage
        }
    }

    // Create a testable version of DeduplicationBot that doesn't make real API calls
    private class TestableDeduplicationBot : DeduplicationBot(maxHistorySize = 2, botUsername = "test_bot", botToken = "test_token") {
        var lastDuplicateMessageChatId: String? = null
        var lastDeletedMessageId: Int? = null
        var lastDeletedChatId: String? = null
        private val imageContents = mutableMapOf<String, ByteArray>()

        override fun sendDuplicateMessage(chatId: String): Message {
            lastDuplicateMessageChatId = chatId

            return Message().apply {
                text = "Duplicate message"
                messageId = 789 // Match the expected message ID in tests
            }
        }

        override fun deleteMessage(chatId: String, messageId: Int) {
            lastDeletedChatId = chatId
            lastDeletedMessageId = messageId
        }

        override fun getImageBytes(photo: org.telegram.telegrambots.meta.api.objects.PhotoSize): ByteArray {
            return imageContents.getOrPut(photo.fileId) { 
                "test image content for ${photo.fileId}".toByteArray() 
            }
        }
    }

    private fun createPhotoUpdateMock(): Update {
        val mockUser = mockk<User> {
            every { id } returns 123L
            every { userName } returns "testUser"
        }

        val mockPhoto = mockk<PhotoSize> {
            every { fileId } returns "test_file_id"
        }

        val mockMessage = mockk<Message> {
            every { from } returns mockUser
            every { chatId } returns 456L
            every { hasText() } returns false
            every { hasPhoto() } returns true
            every { photo } returns listOf(mockPhoto)
        }

        return mockk {
            every { hasMessage() } returns true
            every { getMessage() } returns mockMessage
            every { message } returns mockMessage
        }
    }

    @BeforeEach
    fun setup() {
        bot = TestableDeduplicationBot()
        update = createUpdateMock("test message")
        message = update.message
        user = message.from
    }

    @Test
    fun `test duplicate message detection and deletion`() {
        // First message should be accepted
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Mock message ID for the duplicate message
        every { message.messageId } returns 789

        // Second identical message should be detected as duplicate and deleted
        bot.onUpdateReceived(update)
        assertEquals("456", bot.lastDuplicateMessageChatId)
        assertEquals(789, bot.lastDeletedMessageId)
        assertEquals("456", bot.lastDeletedChatId)
    }

    @Test
    fun `test case insensitive duplicate detection`() {
        // First message with different case
        update = createUpdateMock("Test Message")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)

        // Second message with different case should be detected as duplicate and deleted
        update = createUpdateMock("test message")
        bot.onUpdateReceived(update)
        assertEquals("456", bot.lastDuplicateMessageChatId)
        assertEquals(789, bot.lastDeletedMessageId)
        assertEquals("456", bot.lastDeletedChatId)
    }

    @Test
    fun `test message history limit`() {
        // Send three different messages
        for (i in 1..3) {
            update = createUpdateMock("message $i")
            bot.onUpdateReceived(update)
            assertEquals(null, bot.lastDuplicateMessageChatId)
            assertEquals(null, bot.lastDeletedMessageId)
        }

        // The first message should have been removed due to size limit (2)
        update = createUpdateMock("message 1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId, 
            "Message 1 should not be detected as duplicate after being removed from history")
        assertEquals(null, bot.lastDeletedMessageId,
            "Message 1 should not be deleted after being removed from history")
    }

    @Test
    fun `test empty message handling`() {
        val emptyMessage = mockk<Message> {
            every { text } returns null
            every { hasText() } returns false
            every { hasPhoto() } returns false
            every { hasAnimation() } returns false
            every { messageId } returns 789
            every { from } returns mockk {
                every { id } returns 123L
                every { userName } returns "testUser"
            }
            every { chatId } returns 456L
        }

        update = mockk {
            every { hasMessage() } returns true
            every { getMessage() } returns emptyMessage
            every { message } returns emptyMessage
        }
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId, "Empty messages should be ignored")
        assertEquals(null, bot.lastDeletedMessageId, "Empty messages should not be deleted")
        assertEquals(null, bot.lastDeletedChatId, "Empty messages should not trigger chat actions")
    }

    @Test
    fun `test whitespace and case normalization`() {
        // First message with extra spaces
        update = createUpdateMock("  Test   Message  ")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)

        // Second message with different case and spacing
        update = createUpdateMock("test message")
        bot.onUpdateReceived(update)
        assertEquals("456", bot.lastDuplicateMessageChatId, 
            "Normalized messages should be detected as duplicates")
        assertEquals(789, bot.lastDeletedMessageId,
            "Duplicate normalized message should be deleted")
        assertEquals("456", bot.lastDeletedChatId)
    }

    @Test
    fun `test duplicate image detection`() {
        // First image should be accepted
        update = createPhotoUpdateMock("test_image_1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Same image should be detected as duplicate and deleted
        update = createPhotoUpdateMock("test_image_1")
        bot.onUpdateReceived(update)
        assertEquals("456", bot.lastDuplicateMessageChatId,
            "Duplicate image should be detected")
        assertEquals(789, bot.lastDeletedMessageId,
            "Duplicate image should be deleted")
        assertEquals("456", bot.lastDeletedChatId)
    }

    @Test
    fun `test different images from same user`() {
        // First image should be accepted
        update = createPhotoUpdateMock("test_image_1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Different image from same user should be accepted
        update = createPhotoUpdateMock("test_image_2")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId,
            "Different images from same user should not be detected as duplicates")
        assertEquals(null, bot.lastDeletedMessageId,
            "Different images should not be deleted")
        assertEquals(null, bot.lastDeletedChatId,
            "Different images should not trigger chat actions")
    }

    private fun createAnimationUpdateMock(fileId: String = "test_animation_id"): Update {
        val mockUser = mockk<User> {
            every { id } returns 123L
            every { userName } returns "testUser"
        }

        val mockAnimation = mockk<org.telegram.telegrambots.meta.api.objects.games.Animation> {
            every { this@mockk.fileId } returns fileId
        }

        val mockMessage = mockk<Message> {
            every { from } returns mockUser
            every { chatId } returns 456L
            every { messageId } returns 789
            every { text } returns null
            every { hasText() } returns false
            every { hasPhoto() } returns false
            every { hasAnimation() } returns true
            every { animation } returns mockAnimation
        }

        return mockk {
            every { hasMessage() } returns true
            every { getMessage() } returns mockMessage
            every { message } returns mockMessage
        }
    }

    @Test
    fun `test duplicate animation detection`() {
        // First animation should be accepted
        update = createAnimationUpdateMock("test_animation_1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Same animation should be detected as duplicate and deleted
        update = createAnimationUpdateMock("test_animation_1")
        bot.onUpdateReceived(update)
        assertEquals("456", bot.lastDuplicateMessageChatId,
            "Duplicate animation should be detected")
        assertEquals(789, bot.lastDeletedMessageId,
            "Duplicate animation should be deleted")
        assertEquals("456", bot.lastDeletedChatId)
    }

    @Test
    fun `test different animations from same user`() {
        // First animation should be accepted
        update = createAnimationUpdateMock("test_animation_1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Different animation from same user should be accepted
        update = createAnimationUpdateMock("test_animation_2")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId,
            "Different animations from same user should not be detected as duplicates")
        assertEquals(null, bot.lastDeletedMessageId,
            "Different animations should not be deleted")
        assertEquals(null, bot.lastDeletedChatId,
            "Different animations should not trigger chat actions")
    }

    @Test
    fun `test same animation from different users`() {
        // First animation from user1 should be accepted
        update = createAnimationUpdateMock("test_animation_1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Same animation from user2 should be accepted (different user ID in hash)
        val user2Update = mockk<Update> {
            val user2 = mockk<User> {
                every { id } returns 789L
                every { userName } returns "testUser2"
            }

            val animation = mockk<org.telegram.telegrambots.meta.api.objects.games.Animation> {
                every { fileId } returns "test_animation_1"
            }

            val message = mockk<Message> {
                every { from } returns user2
                every { chatId } returns 456L
                every { messageId } returns 999
                every { text } returns null
                every { hasText() } returns false
                every { hasPhoto() } returns false
                every { hasAnimation() } returns true
                every { this@mockk.animation } returns animation
            }

            every { hasMessage() } returns true
            every { getMessage() } returns message
            every { this@mockk.message } returns message
        }

        bot.onUpdateReceived(user2Update)
        assertEquals(null, bot.lastDuplicateMessageChatId,
            "Same animation from different users should not be detected as duplicates")
        assertEquals(null, bot.lastDeletedMessageId,
            "Same animation from different users should not be deleted")
        assertEquals(null, bot.lastDeletedChatId,
            "Same animation from different users should not trigger chat actions")
    }

    @Test
    fun `test same image from different users`() {
        // First image from user1 should be accepted
        update = createPhotoUpdateMock("test_image_1")
        bot.onUpdateReceived(update)
        assertEquals(null, bot.lastDuplicateMessageChatId)
        assertEquals(null, bot.lastDeletedMessageId)
        assertEquals(null, bot.lastDeletedChatId)

        // Same image from user2 should be accepted (different user ID in hash)
        val user2Update = mockk<Update> {
            val user2 = mockk<User> {
                every { id } returns 789L
                every { userName } returns "testUser2"
            }

            val photo = mockk<PhotoSize> {
                every { fileId } returns "test_image_1"
            }

            val message = mockk<Message> {
                every { from } returns user2
                every { chatId } returns 456L
                every { messageId } returns 999
                every { text } returns null
                every { caption } returns null
                every { hasText() } returns false
                every { hasPhoto() } returns true
                every { hasAnimation() } returns false
                every { this@mockk.photo } returns listOf(photo)
            }

            every { hasMessage() } returns true
            every { getMessage() } returns message
            every { this@mockk.message } returns message
        }

        bot.onUpdateReceived(user2Update)
        assertEquals(null, bot.lastDuplicateMessageChatId,
            "Same image from different users should not be detected as duplicates")
        assertEquals(null, bot.lastDeletedMessageId,
            "Same image from different users should not be deleted")
        assertEquals(null, bot.lastDeletedChatId,
            "Same image from different users should not trigger chat actions")
    }
}
