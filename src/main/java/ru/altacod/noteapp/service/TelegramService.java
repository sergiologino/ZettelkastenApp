package ru.altacod.noteapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import ru.altacod.noteapp.model.Note;
import ru.altacod.noteapp.model.User;
import ru.altacod.noteapp.model.NoteAudio;
import ru.altacod.noteapp.model.NoteFile;
import ru.altacod.noteapp.dto.NoteDTO;
import ru.altacod.noteapp.repository.UserRepository;
import ru.altacod.noteapp.service.NoteService;
import ru.altacod.noteapp.service.ProjectService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TelegramService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String botToken = System.getenv("TELEGRAM_BOT_TOKEN");

    @Autowired
    private NoteService noteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectService projectService;

    public void sendMessage(String chatId, String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        restTemplate.postForObject(url, Map.of("chat_id", chatId, "text", message), Void.class);
    }

    /**
     * Создает заметку из Telegram сообщения
     * @param caption Заголовок заметки
     * @param content Содержимое заметки
     * @param links Список ссылок для OpenGraph данных
     * @param audioFiles Список аудиофайлов
     * @param noteFiles Список файлов
     * @param user Пользователь
     * @param projectId ID проекта (может быть null)
     * @return Созданная заметка
     */
    public Note addNoteFromTelegram(String caption, String content, List<String> links,
                                   List<Map<String, Object>> audioFiles,
                                   List<Map<String, Object>> noteFiles,
                                   User user,
                                   UUID projectId) {
        try {
            // Создаем DTO для заметки
            NoteDTO noteDTO = new NoteDTO();
            noteDTO.setId(UUID.randomUUID());
            noteDTO.setContent(content != null ? content : "Новая заметка из Telegram");
            noteDTO.setTitle(caption != null ? caption : "Заметка из Telegram");
            noteDTO.setUserId(user.getId());
            noteDTO.setCreatedAt(LocalDateTime.now());
            noteDTO.setChangedAt(LocalDateTime.now());
            
            // Устанавливаем проект
            if (projectId != null) {
                noteDTO.setProjectId(projectId);
            } else {
                // Используем проект по умолчанию для бота
                noteDTO.setProjectId(projectService.getDefaultBotProjectForUser(user.getId()).getId());
            }
            
            // Добавляем тег telegram
            noteDTO.getTags().add("telegram");

            // Создаем заметку через NoteService
            Note savedNote = noteService.saveMixedNote(noteDTO, user.getId(), links);
            
            if (savedNote == null) {
                throw new RuntimeException("Ошибка при сохранении заметки без вложений.");
            }

            // Прикрепляем файлы, если они есть
            if (noteFiles != null && !noteFiles.isEmpty()) {
                List<NoteFile> files = noteFiles.stream().map(data -> {
                    NoteFile file = new NoteFile();
                    String serverFilePath = (String) data.get("serverFilePath");
                    file.setServerFilePath(serverFilePath);
                    file.setOriginalName((String) data.get("originalName"));
                    file.setFileType((String) data.get("fileType"));
                    file.setCreatedAt(LocalDateTime.now());
                    return file;
                }).toList();
                
                savedNote = noteService.attachFilesToNote(savedNote.getId(), files);
                if (savedNote == null) {
                    throw new RuntimeException("Заметка сохранена, но произошла ошибка при сохранении файлов.");
                }
            }

            // Прикрепляем аудиофайлы, если они есть
            if (audioFiles != null && !audioFiles.isEmpty()) {
                List<NoteAudio> audios = audioFiles.stream().map(data -> {
                    NoteAudio audio = new NoteAudio();
                    audio.setUrl((String) data.get("url"));
                    audio.setCreatedAt(LocalDateTime.now());
                    audio.setAudioType((String) data.get("type"));
                    audio.setAudioFilePath((String) data.get("serverFilePath"));
                    audio.setSize(new BigDecimal(data.get("size").toString()));
                    return audio;
                }).toList();
                
                savedNote = noteService.attachAudiosToNote(savedNote.getId(), audios);
                if (savedNote == null) {
                    throw new RuntimeException("Заметка сохранена, файлы прикреплены, но произошла ошибка при сохранении аудиофайлов.");
                }
            }
            
            return savedNote;
            
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании заметки из Telegram: " + e.getMessage(), e);
        }
    }
}
