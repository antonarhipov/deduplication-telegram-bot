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
   export TELEGRAM_BOT_USERNAME="your_bot_username"  # Optional, defaults to "DeduplicationBot"
   ```

## Building and Running

Make sure you have JDK 17 or later installed.

```bash
# Build the project
./gradlew build

# Run the bot
./gradlew run
```

## Configuration

The bot can be configured using the following environment variables:

- `TELEGRAM_BOT_TOKEN` (required): Your bot token from BotFather
- `TELEGRAM_BOT_USERNAME` (optional): Your bot's username (defaults to "DeduplicationBot")

The bot keeps track of the last 10,000 messages by default. This can be modified by changing `MAX_MESSAGES_HISTORY` in the Configuration class.

## Logging

Logs are written to both console and file:
- Console: Shows basic information
- File: Detailed logs are written to `logs/deduplication-bot.log`

Log files are automatically rotated daily and kept for 30 days.

## License

This project is licensed under the MIT License - see the LICENSE file for details.