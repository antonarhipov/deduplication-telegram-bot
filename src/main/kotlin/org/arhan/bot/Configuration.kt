package org.arhan.bot

import org.slf4j.LoggerFactory

/**
 * Configuration object for the Deduplication Bot.
 *
 * This object manages the bot's configuration settings, which can be set through environment variables:
 * - TELEGRAM_BOT_TOKEN (required): The authentication token from BotFather
 * - TELEGRAM_BOT_USERNAME (optional): The bot's username, defaults to "DeduplicationBot"
 *
 * The configuration is loaded lazily when first accessed, allowing for environment variables
 * to be set after the application starts but before the bot begins operation.
 */
object Configuration {
    private val logger = LoggerFactory.getLogger(Configuration::class.java)

    /**
     * The Telegram Bot API token.
     *
     * This token must be obtained from BotFather and set in the TELEGRAM_BOT_TOKEN environment variable.
     * A warning will be logged if the token seems too short (less than 45 characters).
     *
     * @throws IllegalStateException if TELEGRAM_BOT_TOKEN environment variable is not set
     */
    val botToken: String by lazy {
        System.getenv("TELEGRAM_BOT_TOKEN")?.also { token ->
            if (token.length < 45) {
                logger.warn("Bot token seems too short, please verify it's correct")
            }
        } ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN environment variable is not set")
    }

    /**
     * The bot's username on Telegram.
     *
     * This can be configured using the TELEGRAM_BOT_USERNAME environment variable.
     * If not set, defaults to "DeduplicationBot".
     */
    val botUsername: String by lazy {
        System.getenv("TELEGRAM_BOT_USERNAME") ?: "DeduplicationCheckerBot"
    }

    /**
     * Maximum number of messages to keep in memory for deduplication.
     *
     * This limits the memory usage of the bot while still providing a reasonable
     * window for duplicate detection. When this limit is reached, the oldest
     * messages are removed to make room for new ones.
     */
    const val MAX_MESSAGES_HISTORY = 10000
}
