package com.example.noteapp.bot;

import com.example.noteapp.model.Tag;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

            String chatId = message.getChatId().toString();
            String text = message.getText();

            // Проверяем тип сообщения
            // TODO Animation потом поменять на Text
            if (text != null && (message.hasPhoto() || text.contains("http"))) {
                handleMixedMessage(message);
            } else if (message.hasAnimation()) {
                handleTextMessage(message);
            } else if (message.hasDocument()) {
                handleDocumentMessage(message);
            } else if (message.hasVoice()) {
                handleVoiceMessage(message);
            } else if (message.hasText()) {
                handleIncomingMessage(message);
            } else {
                sendResponse(message.getChatId().toString(), "Неизвестный тип сообщения. Поддерживаются текст, документы и голосовые сообщения.");
            }
        }
    }

    // Обработка смешанного сообщения
    private void handleMixedMessage(Message message) {
        String chatId = message.getChatId().toString();
        String text = message.getText();
        String photoUrl = null;

        String response =null;

        if (message.hasPhoto()) {
            try {
                String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId(); // Последнее изображение — наибольшего размера
                photoUrl = getFileUrl(fileId);
            } catch (Exception e) {
                sendResponse(chatId, "Ошибка при получении изображения: " + e.getMessage());
            }
        }


        String link = null; // Хранит найденную ссылку
        StringBuilder commentBuilder = new StringBuilder(); // Хранит описание

        // Разделяем сообщение на строки
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.startsWith("http://") || line.startsWith("https://")) {
                link = line.trim(); // Если строка - ссылка, сохраняем её
            } else {
                commentBuilder.append(line.trim()).append("\n"); // Остальное добавляем в описание
            }
        }

        // Если ссылка не найдена, возвращаем сообщение об ошибке
        if (link == null) {
            sendResponse(chatId, "Ошибка: В сообщении отсутствует ссылка.");
            return;
        }

        // Формируем описание
        String comment = commentBuilder.toString().trim();

        System.out.println("Отправляем на создание заметки на бэке: " + link);
        System.out.println("comment: " + comment);
        System.out.println("link: " + link);
        System.out.println("photoUrl: " + photoUrl);


        String result = sendMixedNoteToBackend(comment, link, photoUrl);
        response="Смешанное сообщение обработано: ";

        sendResponse(chatId, response);
    }

    // Обработка фото
    private void handlePhotoMessage(Message message) {
        String chatId = message.getChatId().toString();
        try {
            String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
            String photoUrl = getFileUrl(fileId);
            String response = sendMixedNoteToBackend(null, null, photoUrl);
            sendResponse(chatId, response);
        } catch (Exception e) {
            sendResponse(chatId, "Ошибка при обработке изображения: " + e.getMessage());
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

        if (userMessage.startsWith("/analyze ")) {
            String noteId = userMessage.substring(9).trim();
            analyzeNoteInBackend(noteId, chatId);
        } else {
            sendResponse(chatId, "Неизвестная команда. Используйте /analyze {noteId}.");
        }
    }

    private void handleIncomingMessage(Message message) {
        String chatId = message.getChatId().toString();
        String userMessage = message.getText();

        String response;
        if (userMessage.equalsIgnoreCase("/start")) {
            response = "Добро пожаловать! Используйте команды:\n" +
                    "/addnote {текст} - создать заметку\n" +
                    "/help - список команд";
        } else if (userMessage.startsWith("/addnote ")) {
            String noteContent = userMessage.substring(9).trim();
            if (noteContent.isEmpty()) {
                response = "Ошибка: текст заметки не может быть пустым.";
            } else {
                // Отправка заметки на сервер
                String result = sendNoteToBackend(noteContent, null, null);
                response = "Заметка добавлена: " + result;
            }
        } else if (userMessage.equalsIgnoreCase("/help")) {
            response = "Доступные команды:\n" +
                    "/addnote {текст} - добавить новую заметку\n" +
                    "/help - показать команды";
        } else {
            response = "Неизвестная команда. Используйте /help для списка команд.";
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
    //TODO подставить проект по умолчанию, сделать URL не в коде, а в конфиге
    private String sendNoteToBackend(String content, String fileUrl, String fileName) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();
            if (content != null) requestBody.put("content", content);
            if (fileUrl != null) requestBody.put("url", fileUrl);
            List<String> tags = new ArrayList<>();
            tags.add("telegram");
            requestBody.put("tags", tags);


            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/5c4a3ca8-d911-4ee4-94d6-3386239f8c04", requestBody, String.class
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    private String sendMixedNoteToBackend(String content, String url, String photoUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();
            if (content != null) requestBody.put("content", content);
            if (url != null) requestBody.put("url", url);
            if (photoUrl != null) requestBody.put("photoUrl", photoUrl);
            requestBody.put("fromTelegram", true);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/mixed", requestBody, String.class
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при отправке смешанного сообщения: " + e.getMessage();
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

    private void analyzeNoteInBackend(String noteId, String chatId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/" + noteId + "/analyze?chatId=" + chatId,
                    null, Void.class
            );
            sendResponse(chatId, "Заметка отправлена на анализ. Ожидайте результатов.");
        } catch (Exception e) {
            sendResponse(chatId, "Ошибка при отправке на анализ: " + e.getMessage());
        }
    }
}

