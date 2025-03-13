package ru.altacod.noteapp.config;

import ru.altacod.noteapp.bot.NoteBot;
import ru.altacod.noteapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.altacod.noteapp.service.ProjectService;

@Configuration
public class TelegramBotConfig {

    private final UserRepository userRepository;  // 👈 Добавляем зависимость
    private final ProjectService projectService;

    public TelegramBotConfig(UserRepository userRepository, ProjectService projectService,
                             @Value("${telegram.bot.token}") String botToken,
                             @Value("${telegram.bot.username}") String botUsername) {
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.botToken = botToken;
        this.botUsername = botUsername;

    }

    @Bean
    public NoteBot noteBot(TelegramBotsApi telegramBotsApi) throws Exception {
        String projectId = null;
        NoteBot bot = new NoteBot(userRepository ,botToken, botUsername, projectService);
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
