package ru.altacod.noteapp.bot;

import ru.altacod.noteapp.model.User;
import ru.altacod.noteapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
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
                handleMixedMessage(message, user);
        }
    }

    // Обработка смешанного сообщения


    private void handleMixedMessage(Message message, User user) {
        String chatId = message.getChatId().toString();
        String text = message.hasText() ? message.getText() : null;
        List<String> links = new ArrayList<>();
        List<Map<String, Object>> audioFiles = new ArrayList<>();
        List<Map<String, Object>> noteFiles = new ArrayList<>();

        // Разбор текста на ссылки
        if (text != null) {
            String[] words = text.split("\\s+");
            StringBuilder contentBuilder = new StringBuilder();
            for (String word : words) {
                if (word.startsWith("http://") || word.startsWith("https://")) {
                    links.add(word.trim());

                } else {
                    contentBuilder.append(word).append(" ");
                }
            }
            text = contentBuilder.toString().trim();
        }


        // Загрузка голосовых сообщений
        if (message.hasVoice()) {
            String fileId = message.getVoice().getFileId();
            String downloadedPath = downloadFileFromTelegram(fileId, "audio");
            if (downloadedPath != null) {
                Map<String, Object> audioData = new HashMap<>();
                audioData.put("serverFilePath", downloadedPath);
                audioData.put("originalName", "voice_message.ogg");
                audioData.put("audioType", "ogg");
                audioData.put("size", new File(downloadedPath).length());
                audioData.put("createdAt", LocalDateTime.now());
                audioFiles.add(audioData);
            }
        }

        // Загрузка документов и изображений
        if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();
            String fileName = message.getDocument().getFileName();
            String downloadedPath = downloadFileFromTelegram(fileId, "files");
            if (downloadedPath != null) {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("serverFilePath", downloadedPath);
                fileData.put("originalName", fileName);
                fileData.put("fileType", detectFileType(fileName));
                fileData.put("size", new File(downloadedPath).length());
                fileData.put("createdAt", LocalDateTime.now());
                noteFiles.add(fileData);
            }
        } else if (message.hasPhoto()) {
            String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
            String downloadedPath = downloadFileFromTelegram(fileId, "files");
            if (downloadedPath != null) {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("serverFilePath", downloadedPath);
                fileData.put("originalName", "photo.jpg");
                fileData.put("fileType", "image");
                fileData.put("size", new File(downloadedPath).length());
                fileData.put("createdAt", LocalDateTime.now());
                noteFiles.add(fileData);
            }
        }
        String caption="";
        if (noteFiles!= null && noteFiles.size() > 0) {
            caption= message.getCaption();
            if (caption==null || caption.isEmpty()) {
                caption = text.trim();
            }

        }

        // Отправка на бэкенд
        sendMixedNoteToBackend(caption, text, links, audioFiles, noteFiles, user);
        sendResponse(chatId, "Сообщение обработано.");
    }


    private void sendMixedNoteToBackend(String caption, String content, List<String> links,
                                        List<Map<String, Object>> audioFiles,
                                        List<Map<String, Object>> noteFiles,
                                        User user) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();


            if (content != null) {
                requestBody.put("content", content);
            }else{
                requestBody.put("content", "message from telegram");
            };
            if (caption != null) {
                requestBody.put("caption", caption);
            }else{
                requestBody.put("caption", "caption");
            };

            if (!links.isEmpty()) requestBody.put("openGraph", links);
            if (!audioFiles.isEmpty()) requestBody.put("audios", audioFiles);
            if (!noteFiles.isEmpty()) requestBody.put("files", noteFiles);
            requestBody.put("userId", user.getId().toString());

            restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/mixed",
                    requestBody,
                    String.class
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
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


    private void sendFilesToBackend(String noteId, List<String> fileUrls, User user) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", user.getId().toString());
            requestBody.put("noteId", noteId);
            requestBody.put("fileUrls", fileUrls);

            restTemplate.postForEntity(
                    "http://localhost:8080/api/notes/bot/files",
                    requestBody,
                    String.class
            );
        } catch (Exception e) {
            e.printStackTrace();
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


private String downloadFileFromTelegram(String fileId, String folder) {
    try {
        System.out.println("📥 Начало загрузки файла из Telegram: fileId = " + fileId);

        // Получаем путь к файлу на сервере Telegram
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        String filePath = execute(getFile).getFilePath();

        if (filePath == null || filePath.isEmpty()) {
            System.err.println("❌ Ошибка: Telegram не вернул путь к файлу для fileId = " + fileId);
            return null;
        }

        // Формируем URL для скачивания
        String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
        System.out.println("🔗 Ссылка на скачивание: " + fileUrl);

        // Определяем локальное имя файла
        String fileName = UUID.randomUUID() + "_" + filePath.substring(filePath.lastIndexOf("/") + 1);
        String localFilePath = "E:/uploaded/" + folder + "/" + fileName;

        // Создаём папку, если её нет
        Path savePath = Paths.get("E:/uploaded/" + folder);
        if (!Files.exists(savePath)) {
            Files.createDirectories(savePath);
        }

        // Скачиваем файл
        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, Paths.get(localFilePath), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("✅ Файл успешно загружен: " + localFilePath);
        return localFilePath;
    } catch (Exception e) {
        System.err.println("❌ Ошибка при загрузке файла из Telegram: " + e.getMessage());
        return null;
    }
}


    private String detectFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }

        String lowerCaseName = fileName.toLowerCase();

        if (lowerCaseName.endsWith(".pdf")) {
            return "pdf";
        } else if (lowerCaseName.endsWith(".doc") || lowerCaseName.endsWith(".docx")) {
            return "document";
        } else if (lowerCaseName.endsWith(".xls") || lowerCaseName.endsWith(".xlsx")) {
            return "spreadsheet";
        } else if (lowerCaseName.endsWith(".ppt") || lowerCaseName.endsWith(".pptx")) {
            return "presentation";
        } else if (lowerCaseName.endsWith(".txt") || lowerCaseName.endsWith(".md")) {
            return "text";
        } else if (lowerCaseName.endsWith(".csv")) {
            return "csv";
        } else if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".gif") || lowerCaseName.endsWith(".bmp") || lowerCaseName.endsWith(".tiff") || lowerCaseName.endsWith(".svg")) {
            return "image";
        } else if (lowerCaseName.endsWith(".mp3") || lowerCaseName.endsWith(".wav") || lowerCaseName.endsWith(".ogg") || lowerCaseName.endsWith(".flac") || lowerCaseName.endsWith(".aac")) {
            return "audio";
        } else if (lowerCaseName.endsWith(".mp4") || lowerCaseName.endsWith(".avi") || lowerCaseName.endsWith(".mov") || lowerCaseName.endsWith(".mkv") || lowerCaseName.endsWith(".flv")) {
            return "video";
        } else if (lowerCaseName.endsWith(".zip") || lowerCaseName.endsWith(".rar") || lowerCaseName.endsWith(".7z") || lowerCaseName.endsWith(".tar") || lowerCaseName.endsWith(".gz")) {
            return "archive";
        } else if (lowerCaseName.endsWith(".json") || lowerCaseName.endsWith(".xml") || lowerCaseName.endsWith(".yaml") || lowerCaseName.endsWith(".yml")) {
            return "data";
        } else {
            return "unknown";
        }
    }

}

