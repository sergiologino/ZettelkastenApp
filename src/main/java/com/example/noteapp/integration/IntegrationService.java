package com.example.noteapp.integration;

import com.example.noteapp.config.IntegrationConfig;
import com.example.noteapp.model.Note;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntegrationService {

    private final RestTemplate restTemplate;
    private final IntegrationConfig integrationConfig;

    public IntegrationService(IntegrationConfig integrationConfig) {
        this.restTemplate = new RestTemplate();
        this.integrationConfig = integrationConfig;
    }

    public List<String> analyzeNoteContent(String content) {
        String apiUrl = integrationConfig.getApiUrl();
        try {
            // Отправляем запрос на внешний API
            List<String> tags = restTemplate.postForObject(apiUrl, content, List.class);
            if (tags == null) {
                throw new IntegrationException("Ответ от API пуст.");
            }
            return tags;
        } catch (Exception e) {
            throw new IntegrationException("Ошибка при вызове внешнего API: " + apiUrl, e);
        }
    }

    public List<String> analyzeNoteContent(Note note) {
        String apiUrl = integrationConfig.getApiUrl();
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("content", note.getContent());
            requestBody.put("neuralNetwork", note.getNeuralNetwork());

            if (note.getAudioFilePath() != null) {
                requestBody.put("audioFilePath", note.getAudioFilePath());
                requestBody.put("processAudio", true); // Указываем, что требуется транскрибация
            }

            if (note.getFilePath() != null) {
                requestBody.put("filePath", note.getFilePath());
                requestBody.put("fileType", note.getFileType());
            }

            List<String> tags = restTemplate.postForObject(apiUrl, requestBody, List.class);
            return tags != null ? tags : Collections.emptyList();
        } catch (Exception e) {
            throw new IntegrationException("Ошибка при вызове внешнего API: " + apiUrl, e);
        }
    }

}
