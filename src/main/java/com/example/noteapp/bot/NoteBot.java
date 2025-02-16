package com.example.noteapp.bot;

import com.example.noteapp.model.User;
import com.example.noteapp.repository.UserRepository;
import com.example.noteapp.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class NoteBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;



    public NoteBot(UserRepository userRepository, String botToken, String botUsername) {
        this.userRepository = userRepository;
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    public UUID getCurrentUserId(String username) {
        Optional<User> currentUser=userRepository.findByTlgUsername(username.replace("@", ""));
        if(currentUser.isPresent()) {
            return currentUser.get().getId();
        }
        return null;
    }


//    public User getCurrentUser() {
//
//    }
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
            String username = message.getFrom().getUserName(); // Telegram username
            String text = message.getText();

            // Ищем пользователя по Telegram username
            Optional<User> userOptional = userRepository.findByTlgUsername(username.replace("@", ""));

            if (userOptional.isEmpty()) {
                sendResponse(chatId, "Ошибка: Ваш Telegram-аккаунт не привязан к системе. Укажите в профиле в поле 'Telegram username' имя пользователя из telegram ");
                return;
            }

            User user = userOptional.get();

            // Если у пользователя еще нет Telegram chatId — сохраняем его
            if (Objects.isNull(user.getTelegramChatId())) {  // ✅ Если поле chatID  у пользака не заполнено то заполняем
               user.setTelegramChatId(chatId);
                userRepository.save(user);
            }



            // Проверяем тип сообщения
            // TODO Animation потом поменять на Text
            if (text != null && (message.hasPhoto() || text.contains("http"))) {
                handleMixedMessage(message, user);
            } else if (message.hasAnimation()) {
                handleTextMessage(message);
            } else if (message.hasDocument()) {
                handleDocumentMessage(message, user);
//                handleMixedMessage(message, user);
            } else if (message.hasVoice()) {
                handleVoiceMessage(message, user);
//                handleMixedMessage(message, user);
            } else if (message.hasText()) {
                handleIncomingMessage(message, user);
//                handleMixedMessage(message, user);
            } else if (message.hasPhoto()) {
                handlePhotoMessage(message, user);
//                handleMixedMessage(message, user);
            } else {
                sendResponse(message.getChatId().toString(), "Неизвестный тип сообщения. Поддерживаются текст, документы, ссылки и голосовые сообщения.");
            }
        }
    }

    // Обработка смешанного сообщения

    //TODO USERID
    private void handleMixedMessage(Message message, User user) { // UserId
        String chatId = message.getChatId().toString();
        String text = message.getText();
        String photoUrl = null;
        String response =null;
        System.out.println("Начинаем обработку смешанного сообщения");

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
        if (link == null && (text == null || text.trim().isEmpty())) {
            sendResponse(chatId, "Ошибка: Сообщение пустое или не содержит ссылки.");
            return;
        }

        // Формируем описание
        String comment = commentBuilder.toString().trim();
        if (comment.isEmpty()){
            comment="Без контекста";
        }

        System.out.println("Отправляем на создание заметки на бэке: " + link);
        System.out.println("comment: " + comment);
        System.out.println("link: " + link);
        System.out.println("photoUrl: " + photoUrl);


        String result = sendMixedNoteToBackend(comment, link, photoUrl, user); //userId

        response="Смешанное сообщение обработано ";//+result;

        sendResponse(chatId, response);
    }

    // Обработка фото
    private void handlePhotoMessage(Message message, User user) {
        String chatId = message.getChatId().toString();
        try {
            String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
            String photoUrl = getFileUrl(fileId);
            String response = sendMixedNoteToBackend(null, null, photoUrl, user);
            sendResponse(chatId, response);
        } catch (Exception e) {
            sendResponse(chatId, "Ошибка при обработке изображения: " + e.getMessage());
        }
    }


    // Обработка документов
    private void handleDocumentMessage(Message message, User user) {
        String chatId = message.getChatId().toString();
        try {
            String fileId = message.getDocument().getFileId();
            String fileName = message.getDocument().getFileName();

            String fileUrl = getFileUrl(fileId);
            String result = sendNoteToBackend(null, fileUrl, fileName, user);
            sendResponse(chatId, result);
        } catch (Exception e) {
            sendResponse(chatId, "Ошибка при обработке документа: " + e.getMessage());
        }
    }

    // Обработка голосовых сообщений
    private void handleVoiceMessage(Message message, User user) {
        String chatId = message.getChatId().toString();
        try {
            String fileId = message.getVoice().getFileId();
            String fileUrl = getFileUrl(fileId);

            String result = sendNoteToBackend(null, fileUrl, "voice.ogg", user);
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

    private void handleIncomingMessage(Message message, User user) {
        String chatId = message.getChatId().toString();
        String userMessage = message.getText();

        String response;
//        if (userMessage.equalsIgnoreCase("/start")) {
//            response = "Добро пожаловать! Используйте команды:\n" +
//                    "/addnote {текст} - создать заметку\n" +
//                    "/help - список команд";
//        } else if (userMessage.startsWith("/addnote ")) {
//            String title = userMessage.substring(6).trim();
            String noteContent = userMessage.substring(6).trim();
//            if (noteContent.isEmpty()) {
//                response = "Ошибка: текст заметки не может быть пустым.";
//            } else {
//                // Отправка заметки на сервер
                String result = sendNoteToBackend(noteContent, null, null, user);
                response = "Заметка добавлена ";// + result;
//            }
//        } else if (userMessage.equalsIgnoreCase("/help")) {
//            response = "Доступные команды:\n" +
//                    "/addnote {текст} - добавить новую заметку\n" +
//                    "/help - показать команды";
//        } else {
//            response = "Неизвестная команда. Используйте /help для списка команд.";
//        }

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
    private String sendNoteToBackend(String content, String fileUrl, String fileName, User user) {
        UUID userId = user.getId();
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();
            if (content != null) requestBody.put("content", content);
            if (fileUrl != null) requestBody.put("url", fileUrl);
            List<String> tags = new ArrayList<>();
            tags.add("telegram");
            requestBody.put("tags", tags);
            requestBody.put("userId", userId.toString());



            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/text", requestBody, String.class
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }
//  TODO USERID
    private String sendMixedNoteToBackend(String content, String url, String photoUrl, User user) {
//        UUID userId = getCurrentUserId();
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();
            if (content != null) requestBody.put("content", content);
            if (url != null) requestBody.put("url", url);
            if (photoUrl != null) requestBody.put("photoUrl", photoUrl);
            requestBody.put("fromTelegram", true);
            requestBody.put("userId", user.getId().toString());

//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Content-Type", "application/json");
//
//            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/mixed",
                    requestBody,
                    String.class
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при отправке смешанного сообщения: " + e.getMessage();
        }
    }

//    private String getAuthToken() {
//        return null;
//    }


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

