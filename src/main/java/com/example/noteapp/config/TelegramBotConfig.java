package com.example.noteapp.config;

import com.example.noteapp.bot.NoteBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    @Bean
    public NoteBot noteBot(TelegramBotsApi telegramBotsApi) throws Exception {
        NoteBot bot = new NoteBot(botToken, botUsername);
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
