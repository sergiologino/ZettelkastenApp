package com.example.noteapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class TelegramService {

    private final RestTemplate restTemplate = new RestTemplate();


    @Value("${telegram.bot.token}")
    private String botToken;

    public void sendMessage(String chatId, String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        restTemplate.postForObject(url, Map.of("chat_id", chatId, "text", message), Void.class);
    }
}
