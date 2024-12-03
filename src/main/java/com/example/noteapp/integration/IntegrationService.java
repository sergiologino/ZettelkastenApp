package com.example.noteapp.integration;

import com.example.noteapp.config.IntegrationConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

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
}
