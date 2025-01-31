package org.arhan.bot

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationTest {
    @Test
    fun `test message history size is reasonable`() {
        assertTrue(Configuration.MAX_MESSAGES_HISTORY in 1000..1000000,
            "Message history size should be between 1000 and 1000000")
    }

    @Test
    fun `test default bot username`() {
        // When no environment variable is set, should return default value
        if (System.getenv("TELEGRAM_BOT_USERNAME") == null) {
            assertEquals("DeduplicationBot", Configuration.botUsername)
        }
    }

    @Test
    fun `test missing bot token throws exception`() {
        // Only run this test if TELEGRAM_BOT_TOKEN is not set in the environment
        if (System.getenv("TELEGRAM_BOT_TOKEN") == null) {
            assertThrows<IllegalStateException>("Should throw when TELEGRAM_BOT_TOKEN is not set") {
                Configuration.botToken
            }
        }
    }

    @Test
    fun `test bot token validation`() {
        val token = System.getenv("TELEGRAM_BOT_TOKEN")
        if (token != null && token.length < 45) {
            // If we have a token and it's short, we should still get it but a warning will be logged
            assertEquals(token, Configuration.botToken)
        }
    }
}
