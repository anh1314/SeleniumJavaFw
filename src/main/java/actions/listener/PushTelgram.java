package actions.listener;

import actions.utils.LogUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

public class PushTelgram extends TelegramLongPollingBot {
    public static final int MAX_CHUNK_SIZE = 4000;

    private final String userName_bot;
    private final String token_bot;

    public PushTelgram(String userName_bot, String token_bot) {
        this.userName_bot = userName_bot;
        this.token_bot = token_bot;
    }

    @Override
    public void onUpdateReceived(Update update) {
    }

    @Override
    public String getBotUsername() {
        return userName_bot;
    }

    @Override
    public String getBotToken() {
        return token_bot;
    }

    /**
     * Splits a message into chunks of at most chunkSize characters.
     */
    public static List<String> splitMessage(String messageText, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (messageText == null || messageText.isEmpty()) {
            return chunks;
        }
        int totalLength = messageText.length();
        for (int offset = 0; offset < totalLength; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, totalLength);
            chunks.add(messageText.substring(offset, end));
        }
        return chunks;
    }

    public static boolean sendMessageText(String chatId, String token, String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            LogUtils.info("Telegram message is empty, skipping send.");
            return true;
        }

        TelegramBot bot = new TelegramBot(token);
        List<String> chunks = splitMessage(messageText, MAX_CHUNK_SIZE);
        boolean allSuccess = true;

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            SendMessage request = new SendMessage(chatId, chunk)
                    .parseMode(ParseMode.HTML)
                    .disableWebPagePreview(false)
                    .disableNotification(true);

            SendResponse response = bot.execute(request);
            if (response == null || !response.isOk()) {
                allSuccess = false;
                LogUtils.error("Send Telegram message chunk " + (i + 1) + "/" + chunks.size() + " failed: " +
                        (response != null ? response.description() : "null response"));
            }
        }

        if (allSuccess) {
            LogUtils.info("Send message to Telegram success (" + chunks.size() + " chunk(s))");
        }
        return allSuccess;
    }
}
