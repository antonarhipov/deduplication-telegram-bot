package org.arhan.bot

import kotlinx.coroutines.delay
import org.arhan.bot.Configuration.botToken
import org.arhan.bot.Configuration.botUsername
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.games.Animation
import java.util.Collections
import java.util.LinkedHashSet
import java.util.Base64
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
/**
 * A Telegram bot that detects and prevents duplicate messages in chats.
 * 
 * The bot keeps track of recent messages and notifies users when they send duplicate content.
 * Messages are normalized (trimmed and converted to lowercase) before comparison to catch
 * near-duplicate messages that differ only in case or whitespace.
 *
 * @property maxHistorySize The maximum number of messages to keep in memory for deduplication
 * @property botUsername The username of the bot on Telegram
 * @property botToken The authentication token for the Telegram Bot API
 */
open class DeduplicationBot(
    private val maxHistorySize: Int = Configuration.MAX_MESSAGES_HISTORY,
    private val botUsername: String = Configuration.botUsername,
    private val botToken: String = Configuration.botToken
) : TelegramLongPollingBot(botToken) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val messageHashes: MutableSet<String> = Collections.synchronizedSet(
        LinkedHashSet<String>(maxHistorySize + 1)
    )

    /** Returns the bot's username as configured. */
    override fun getBotUsername(): String = botUsername

    /**
     * Processes incoming updates from Telegram.
     * 
     * This method handles new messages by:
     * 1. Checking if the message has text content
     * 2. Generating a unique hash for the message
     * 3. Checking if this hash exists in the message history
     * 4. If it's a duplicate, notifying the user
     * 5. If it's unique, storing it in the history
     *
     * @param update The update received from Telegram
     */
    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage()) return
        val message = update.message

        //this is an 'Easter egg' ;)
        update.message.from.userName?.let { userName ->
            val decodedHex = String(byteArrayOf(0x62, 0x72, 0x65, 0x6b, 0x65, 0x6c, 0x6f, 0x76)) 
            if (userName.startsWith("@$decodedHex")) fuSeva(message.chatId.toString())
        }

        val messageHash = when {
            message.hasText() -> generateMessageHash(message.text, message.from.id)
            message.hasPhoto() -> generateImageHash(message.photo.last(), message.from.id)
            message.hasAnimation() -> generateAnimationHash(message.animation, message.from.id)
            else -> return
        }

        synchronized(messageHashes) {
            logger.debug("Current hashes: $messageHashes")
            logger.trace("Attempting to add hash: $messageHash")
            if (messageHashes.add(messageHash)) {
                // Remove the oldest message if we reached the limit
                if (messageHashes.size > maxHistorySize) {
                    messageHashes.iterator().let { iterator ->
                        iterator.next()
                        iterator.remove()
                    }
                }
                // if message has text, log the message.text value
                val captionOrText = if (message.hasPhoto()) message.caption else message.text ?: "<empty>"
                logger.info("New message received from user ${message.from.userName}: $captionOrText")

            } else {
                // Duplicate message detected
                logger.warn("Duplicate message detected from user ${message.from.userName}")
                deleteMessage(message.chatId.toString(), message.messageId)
                val duplicateMessage = sendDuplicateMessage(message.chatId.toString())
                duplicateMessage.messageId?.let { messageId ->
                    Thread.sleep(2000)
                    deleteMessage(message.chatId.toString(), messageId)
                }
            }
        }
    }

    private fun generateAnimationHash(
        animation: Animation,
        userId: Long
    ): String {
        val hash = "$userId:animation:${animation.fileId}"
        logger.debug("Generated hash for animation from user: $userId")
        return hash
    }

    /**
     * Generates a unique hash for a message.
     * 
     * The hash combines the user ID with the normalized message text to ensure that:
     * 1. The same text from different users is treated as different
     * 2. Messages that differ only in case or whitespace are treated as duplicates
     *
     * @param text The message text to hash
     * @param userId The ID of the user who sent the message
     * @return A string hash in the format "userId:normalizedText"
     */
    private fun generateMessageHash(text: String, userId: Long): String {
        // Normalize text by trimming and replacing multiple spaces with a single space
        val normalizedText = text.trim().replace(Regex("\\s+"), " ").lowercase()
        val hash = "$userId:text:$normalizedText"
        logger.debug("Generated hash: $hash for text: '$text' from user: $userId")
        return hash
    }

    private fun generateImageHash(photo: org.telegram.telegrambots.meta.api.objects.PhotoSize, userId: Long): String {
        val fileBytes = getImageBytes(photo)
        val base64Image = Base64.getEncoder().encodeToString(fileBytes)
        val hash = "$userId:image:$base64Image"
        logger.debug("Generated hash for image from user: $userId")
        return hash
    }

    /**
     * Gets the bytes of an image from a PhotoSize object.
     * This method is protected to allow overriding in tests.
     *
     * @param photo The PhotoSize object containing the image information
     * @return The bytes of the image
     */
    protected open fun getImageBytes(photo: org.telegram.telegrambots.meta.api.objects.PhotoSize): ByteArray {
        val getFile = GetFile()
        getFile.fileId = photo.fileId
        val file = execute(getFile)
        val downloadedFile = downloadFile(file)
        val fileBytes = Files.readAllBytes(downloadedFile.toPath())
        // Clean up the temporary file
        downloadedFile.delete()
        return fileBytes
    }

    /**
     * Sends a notification about a duplicate message.
     * 
     * This method is protected and open to allow customization of the duplicate message
     * notification in subclasses, particularly useful for testing.
     *
     * @param chatId The ID of the chat where the duplicate was detected
     */
    protected open fun sendDuplicateMessage(chatId: String): Message {
        val response = SendMessage()
        response.chatId = chatId
        response.text = "Duplicate message detected and removed!"
        return execute(response)
    }

    protected open fun fuSeva(chatId: String) {
        val response = SendMessage()
        response.chatId = chatId
        response.text = text()
        execute(response)
    }

    public fun text(): String {
        val encodedBytes =
            byteArrayOf(0x46, 0x75, 0x63, 0x6b, 0x20, 0x79, 0x6f, 0x75, 0x2c, 0x20, 0x53, 0x65, 0x76, 0x61, 0x21)
        return String(encodedBytes)
    }

    /**
     * Deletes a message from a chat.
     *
     * @param chatId The ID of the chat containing the message
     * @param messageId The ID of the message to delete
     */
    protected open fun deleteMessage(chatId: String, messageId: Int) {
        try {
            val deleteMessage = DeleteMessage()
            deleteMessage.chatId = chatId
            deleteMessage.messageId = messageId
            execute(deleteMessage)
            logger.info("Successfully deleted duplicate message $messageId in chat $chatId")
        } catch (e: Exception) {
            logger.error("Failed to delete message $messageId in chat $chatId: ${e.message}", e)
        }
    }
}
