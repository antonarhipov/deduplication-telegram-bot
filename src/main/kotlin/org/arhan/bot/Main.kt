package org.arhan.bot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import kotlin.system.exitProcess

/**
 * Main entry point for the Deduplication Bot application.
 *
 * This file contains the main function that:
 * 1. Initializes the Telegram Bots API
 * 2. Creates and registers the DeduplicationBot
 * 3. Keeps the application running
 * 4. Handles any errors that occur during startup
 *
 * The bot requires the TELEGRAM_BOT_TOKEN environment variable to be set.
 * Optionally, TELEGRAM_BOT_USERNAME can be set to customize the bot's username.
 */

private val logger = LoggerFactory.getLogger("Main")

/**
 * Configures Logback for console output with a specific pattern.
 */
internal fun parseLogLevel(args: Array<String>): Level {
    val loggingArg = args.indexOf("--logging")
    if (loggingArg == -1 || loggingArg == args.lastIndex) {
        return Level.DEBUG // Default level
    }

    return when (val level = args[loggingArg + 1].uppercase()) {
        "ERROR" -> Level.ERROR
        "WARN" -> Level.WARN
        "INFO" -> Level.INFO
        "DEBUG" -> Level.DEBUG
        "TRACE" -> Level.TRACE
        else -> {
            logger.warn("Invalid log level: $level. Using default level DEBUG")
            Level.DEBUG
        }
    }
}

private fun configureLogging(level: Level) {
    val context = LoggerFactory.getILoggerFactory() as LoggerContext
    context.reset()

    val encoder = PatternLayoutEncoder().apply {
        this.context = context
        this.pattern = "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"
        this.start()
    }

    val appender = ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
        this.context = context
        this.name = "STDOUT"
        this.encoder = encoder
        this.start()
    }

    context.getLogger("ROOT").apply { 
        this.addAppender(appender)
        this.level = level
    }
}

/**
 * Application entry point.
 *
 * Starts the bot and keeps it running until the process is terminated.
 * Any errors during startup will be logged and will cause the application to exit
 * with a status code of 1.
 */
fun main(args: Array<String>) {
    try {
        val logLevel = parseLogLevel(args)
        configureLogging(logLevel)
        logger.debug("Log level set to: $logLevel")
        logger.info("Starting Deduplication Bot...")

        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        val bot = DeduplicationBot()
        botsApi.registerBot(bot)

        logger.info("Bot started successfully! Username: ${Configuration.botUsername}")
        logger.info("Bot is now ready to handle messages")

        // Keep the application running
        Thread.currentThread().join()
    } catch (e: TelegramApiException) {
        logger.error("Failed to start bot: ${e.message}", e)
        exitProcess(1)
    } catch (e: Exception) {
        logger.error("Unexpected error: ${e.message}", e)
        exitProcess(1)
    }
}
