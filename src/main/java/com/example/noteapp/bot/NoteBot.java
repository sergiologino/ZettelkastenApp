package com.example.noteapp.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

@Component
public class NoteBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public NoteBot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            // Проверяем тип сообщения
            if (message.hasText()) {
                handleTextMessage(message);
            } else if (message.hasDocument()) {
                handleDocumentMessage(message);
            } else if (message.hasVoice()) {
                handleVoiceMessage(message);
            } else {
                sendResponse(message.getChatId().toString(), "Неизвестный тип сообщения. Поддерживаются текст, документы и голосовые сообщения.");
            }
        }
    }
    // Обработка документов
    private void handleDocumentMessage(Message message) {
        String chatId = message.getChatId().toString();
        try {
            String fileId = message.getDocument().getFileId();
            String fileName = message.getDocument().getFileName();

            String fileUrl = getFileUrl(fileId);
            String result = sendNoteToBackend(null, fileUrl, fileName);
            sendResponse(chatId, result);
        } catch (Exception e) {
            sendResponse(chatId, "Ошибка при обработке документа: " + e.getMessage());
        }
    }

    // Обработка голосовых сообщений
    private void handleVoiceMessage(Message message) {
        String chatId = message.getChatId().toString();
        try {
            String fileId = message.getVoice().getFileId();
            String fileUrl = getFileUrl(fileId);

            String result = sendNoteToBackend(null, fileUrl, "voice.ogg");
            sendResponse(chatId, result);
        } catch (Exception e) {
            sendResponse(chatId, "Ошибка при обработке голосового сообщения: " + e.getMessage());
        }
    }

    // Обработка текстового сообщения
    private void handleTextMessage(Message message) {
        String chatId = message.getChatId().toString();
        String userMessage = message.getText();

        if (userMessage.startsWith("/addnote ")) {
            String noteContent = userMessage.substring(9).trim();
            if (noteContent.isEmpty()) {
                sendResponse(chatId, "Ошибка: текст заметки не может быть пустым.");
            } else {
                String result = sendNoteToBackend(noteContent, null, null);
                sendResponse(chatId, result);
            }
        } else {
            sendResponse(chatId, "Неизвестная команда. Используйте /addnote {текст} для создания заметки.");
        }
    }

    private void handleIncomingMessage(Message message) {
        String chatId = message.getChatId().toString();
        String userMessage = message.getText();

        String response;
        if (userMessage.equalsIgnoreCase("/start")) {
            response = "Добро пожаловать! Используйте команды:\n" +
                    "/addnote - создать заметку\n" +
                    "/help - список команд";
        } else if (userMessage.startsWith("/addnote")) {
            response = "Введите текст заметки в формате: /addnote {ваш текст}";
        } else if (userMessage.startsWith("/addnote ")) {
            String noteContent = userMessage.substring(9).trim();
            if (noteContent.isEmpty()) {
                response = "Ошибка: текст заметки не может быть пустым.";
            } else {
                // Отправка заметки на сервер
                boolean success = Boolean.parseBoolean(sendNoteToBackend(noteContent,null,null));
                response = success ? "Заметка успешно добавлена!" : "Ошибка при добавлении заметки.";
            }
        } else {
            response = "Неизвестная команда. Используйте /help для получения списка команд.";
        }

        sendResponse(chatId, response);
    }

    // Получение URL файла по его ID
    private String getFileUrl(String fileId) {
        try {
            String filePath = execute(new GetFile(fileId)).getFilePath();
            return "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при получении файла: " + e.getMessage(), e);
        }
    }

    // Отправка данных на бэкенд
    private String sendNoteToBackend(String content, String fileUrl, String fileName) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Создаем тело запроса
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            if (content != null) {
                requestBody.add("content", content);
            }
            if (fileUrl != null) {
                requestBody.add("fileUrl", fileUrl);
                requestBody.add("fileName", fileName);
            }

            // Отправляем данные на сервер
            ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:8080/api/notes", requestBody, Map.class);

            // Обработка ответа
            Map<String, Object> responseBody = response.getBody();
            return "Заметка добавлена!\nСсылка: " + responseBody.get("noteUrl") +
                    "\nРезультат анализа: " + responseBody.get("analysis") +
                    "\nТеги: " + responseBody.get("tags");
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при добавлении заметки: " + e.getMessage();
        }
    }

    private void sendResponse(String chatId, String response) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(response);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

