package com.example.noteapp.config;

import com.example.noteapp.bot.NoteBot;
import com.example.noteapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    private final UserRepository userRepository;  // 👈 Добавляем зависимость

    public TelegramBotConfig(UserRepository userRepository,
                             @Value("${telegram.bot.token}") String botToken,
                             @Value("${telegram.bot.username}") String botUsername) {
        this.userRepository = userRepository;
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Bean
    public NoteBot noteBot(TelegramBotsApi telegramBotsApi) throws Exception {
        NoteBot bot = new NoteBot(userRepository ,botToken, botUsername);
        telegramBotsApi.registerBot(bot);
        return bot;
    }

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws Exception {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    public String botToken() {
        return botToken;
    }

    @Bean
    public String botUsername() {
        return botUsername;
    }
}
