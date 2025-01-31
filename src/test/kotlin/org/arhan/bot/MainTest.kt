package org.arhan.bot

import ch.qos.logback.classic.Level
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun `test parseLogLevel with valid levels`() {
        val testCases = mapOf(
            arrayOf("--logging", "ERROR") to Level.ERROR,
            arrayOf("--logging", "WARN") to Level.WARN,
            arrayOf("--logging", "INFO") to Level.INFO,
            arrayOf("--logging", "DEBUG") to Level.DEBUG,
            arrayOf("--logging", "TRACE") to Level.TRACE,
            // Test case insensitivity
            arrayOf("--logging", "info") to Level.INFO,
            arrayOf("--logging", "Debug") to Level.DEBUG
        )

        testCases.forEach { (args, expectedLevel) ->
            assertEquals(expectedLevel, parseLogLevel(args), "Failed for level ${args[1]}")
        }
    }

    @Test
    fun `test parseLogLevel with invalid level`() {
        val args = arrayOf("--logging", "INVALID")
        assertEquals(Level.DEBUG, parseLogLevel(args), "Should default to DEBUG for invalid level")
    }

    @Test
    fun `test parseLogLevel with missing level argument`() {
        val args = arrayOf("--logging")
        assertEquals(Level.DEBUG, parseLogLevel(args), "Should default to DEBUG when level is missing")
    }

    @Test
    fun `test parseLogLevel with no logging argument`() {
        val args = arrayOf("other", "arguments")
        assertEquals(Level.DEBUG, parseLogLevel(args), "Should default to DEBUG when --logging is not present")
    }

    @Test
    fun `test parseLogLevel with empty args`() {
        val args = emptyArray<String>()
        assertEquals(Level.DEBUG, parseLogLevel(args), "Should default to DEBUG for empty args")
    }
}