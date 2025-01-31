# Telegram Deduplication Bot

A Telegram bot that detects and prevents duplicate messages in chats. The bot keeps track of messages and notifies users when they send duplicate content.

## Features

- Detects duplicate messages in chats
- Case-insensitive message comparison
- Configurable message history size
- Proper logging with both console and file output

## Setup

1. Create a new bot using [@BotFather](https://t.me/botfather) on Telegram
2. Get your bot token from BotFather
3. Set the following environment variables:
   ```bash
   export TELEGRAM_BOT_TOKEN="your_bot_token_here"
   export TELEGRAM_BOT_USERNAME="your_bot_username"  # Optional, defaults to "DeduplicationCheckerBot"
   ```

## Building and Running

Make sure you have JDK 17 installed.

### Dependencies
- Kotlin 2.1.10
- Telegram Bot API 6.9.7.1
- Logback 1.2.11 for logging
- JUnit 5.9.2 and MockK for testing

```bash
# Build the project
./gradlew build

# Run the bot
./gradlew run
```

## Configuration

The bot can be configured using the following environment variables:

- `TELEGRAM_BOT_TOKEN` (required): Your bot token from BotFather
- `TELEGRAM_BOT_USERNAME` (optional): Your bot's username (defaults to "DeduplicationCheckerBot")

The bot keeps track of the last 10,000 messages by default. This can be modified by changing `MAX_MESSAGES_HISTORY` in the Configuration class.

## Logging

Logs are written to both console and file:
- Console: Shows basic information
- File: Detailed logs are written to `logs/deduplication-bot.log`

Log files are automatically rotated daily and kept for 30 days.

## Usage

1. Start a private chat with your bot on Telegram
2. Add the bot to your group chat:
   - Open your group
   - Click on group settings
   - Select "Administrators"
   - Click "Add Admin"
   - Search for your bot by username and add it

The bot requires the following permissions in the group:
- Read Messages
- Send Messages
- Delete Messages (optional, for automatic duplicate removal)

Once added, the bot will automatically start monitoring messages for duplicates. When a duplicate message is detected, the bot will reply to the message indicating that it's a duplicate.
