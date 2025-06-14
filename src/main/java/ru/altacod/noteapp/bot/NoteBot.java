package ru.altacod.noteapp.bot;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import ru.altacod.noteapp.model.Project;
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
import ru.altacod.noteapp.service.ProjectService;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NoteBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ProjectService projectService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${backend.url}")
    private String backendUrl;

    private final Map<String, Message> projectSelectionCache = new HashMap<>();


    public NoteBot(UserRepository userRepository, ProjectService projectService) {
        this.userRepository = userRepository;
        this.projectService = projectService;

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

    @Transactional
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage()) {
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();

            // 🔐 Проверка: является ли сообщение пересланным
            boolean isForwarded = message.getForwardFrom() != null
                    || message.getForwardFromChat() != null
                    || message.getForwardSenderName() != null;

            if (isForwarded) {
                sendResponse(chatId, "⚠️ Пересланные сообщения не поддерживаются. Пожалуйста, отправьте сообщение напрямую.");
                return;
            }

            String username = message.getFrom().getUserName();

            // 🔎 Поиск пользователя по username или по chatId
            Optional<User> userOptional = Optional.empty();
            if (username != null && !username.isEmpty()) {
                userOptional = userRepository.findByTlgUsername(username.replace("@", ""));
            }

            if (userOptional.isEmpty()) {
                userOptional = userRepository.findByTelegramChatId(chatId);
            }

            if (userOptional.isEmpty()) {
                sendResponse(chatId, "❌ Ошибка: ваш Telegram-аккаунт не привязан. Укажите Telegram username в профиле.");
                return;
            }

            User user = userOptional.get();

            // Сохраняем chatId, если его ещё нет
            if (user.getTelegramChatId() == null || user.getTelegramChatId().isEmpty()) {
                user.setTelegramChatId(chatId);
                userRepository.saveAndFlush(user);
            }

            // 🔀 Обработка: выбор проекта или сразу создаём заметку
            if (user.isAskProjectBeforeSave()) {
                sendProjectSelection(chatId, message, user);
            } else {
                UUID projectMock = null;
                handleMixedMessage(message, user, projectMock);
            }

        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String data = callbackQuery.getData();

        if (data.startsWith("PRJ_")) {
            String[] parts = data.split("_", 3); // ✅ Разбиваем на 3 части, чтобы избежать проблем с UUID
            if (parts.length < 3) {
                sendResponse(chatId, "Ошибка: некорректные данные выбора проекта.");
                return;
            }
            String selectionKey  = parts[1]; // Получаем selectionKey - ключ заметки
            String projectIdStr  = parts[2]; // Полный UUID проекта


            Optional<User>  optionalUser = userRepository.findByTelegramChatId(chatId);
                    if (optionalUser.isEmpty()) {
                        // Если chatId не найден, пробуем найти по ID
                        sendResponse(chatId, "Ошибка: пользователь не найден.");
                        return;
                    };
            User user = optionalUser.get();


            // Получаем сохраненный текст заметки
            Message originalMessage = projectSelectionCache.get(selectionKey);
            if (originalMessage == null) {
                sendResponse(chatId, "Ошибка: исходное сообщение не найдено. Попробуйте снова.");
                return;
            }
            projectSelectionCache.remove(selectionKey); // Удаляем запись после использования

          // Найти полный projectId по его укороченной версии
            // ✅ Преобразуем `projectIdStr` в `UUID`
            UUID projectId;
            try {
                projectId = UUID.fromString(projectIdStr);
            } catch (IllegalArgumentException e) {
                sendResponse(chatId, "Ошибка: некорректный идентификатор проекта.");
                return;
            }
            // ✅ Получаем название проекта
            Project selectedProject = projectService.getProjectById(projectId, user.getId());

            // ✅ Удаляем клавиатуру (редактируем предыдущее сообщение)
            removeInlineKeyboard(callbackQuery.getMessage());

            // ✅ Отправляем подтверждающее сообщение
            sendResponse(chatId, "✅ Размещено в **" + selectedProject.getName() + "**");

            // ✅ Передаем исходное сообщение и проект в обработчик
            handleMixedMessage(originalMessage, user, projectId);
        }
    }
    private void removeInlineKeyboard(Message message) {
        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
        editMarkup.setChatId(message.getChatId().toString());
        editMarkup.setMessageId(message.getMessageId());
        editMarkup.setReplyMarkup(null); // Убираем клавиатуру

        try {
            execute(editMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    private void sendProjectSelection(String chatId, Message message, User user) {
        List<Project> projects = projectService.getAllProjectsForUser(user.getId());
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>(); // ✅ исправлено

        // Генерируем уникальный ключ
        String selectionKey = UUID.randomUUID().toString().substring(0, 8);
        projectSelectionCache.put(selectionKey, message); // Сохраняем заметку в памяти

        for (Project project : projects) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(project.getName());
            button.setCallbackData("PRJ_" + selectionKey + "_" + project.getId()); // ✅ Передаем полный UUID проекта
            keyboard.add(Collections.singletonList(button)); // ✅ исправлено
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard); // ✅ исправлено

        SendMessage responseMessage  = new SendMessage(chatId, "Выберите проект для заметки:");
        responseMessage.setReplyMarkup(markup);
        try {
            execute(responseMessage); // Используем стандартный метод API Telegram
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Обработка смешанного сообщения


    private void handleMixedMessage(Message message, User user, UUID projectId) {
        String chatId = message.getChatId().toString();
        String text = message.hasText() ? message.getText() : null;
        List<String> links = new ArrayList<>();
        List<Map<String, Object>> audioFiles = new ArrayList<>();
        List<Map<String, Object>> noteFiles = new ArrayList<>();

        // Разбор текста на ссылки
        StringBuilder contentBuilder = new StringBuilder();
        if (text != null) {
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (word.startsWith("http://") || word.startsWith("https://")) {
                    links.add(word.trim());
                } else {
                    contentBuilder.append(word).append(" ");
                }
            }
        }
        text = contentBuilder.toString().trim();


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

        // 📝 Подпись (если есть)
        String caption="";
        if (noteFiles!= null && noteFiles.size() > 0) {
            caption= message.getCaption();
            if (caption==null || caption.isEmpty()) {
                caption = text.trim();
            }

        }


        // Отправка на бэкенд
        sendMixedNoteToBackend(
                caption != null ? caption : "Новая заметка из Telegram",
                text,
                links,
                audioFiles,
                noteFiles,
                user,
                projectId);
        sendResponse(chatId, "Сообщение обработано.");
    }


    private void sendMixedNoteToBackend(String caption, String content, List<String> links,
                                        List<Map<String, Object>> audioFiles,
                                        List<Map<String, Object>> noteFiles,
                                        User user,
                                        UUID projectId) {
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
            if(!(projectId ==null)){
                requestBody.put("projectId", projectId.toString());
            }

            String url = backendUrl + "/api/notes/mixed";
            restTemplate.postForEntity(
                    url,
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
        message.enableMarkdown(true); // Поддержка **жирного** текста
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

