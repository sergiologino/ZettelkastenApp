package ru.altacod.noteapp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import ru.altacod.noteapp.bot.NoteBot;
import ru.altacod.noteapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.altacod.noteapp.service.ProjectService;

@Configuration
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
public class TelegramBotConfig {


    @Value("${telegram.bot.enabled:true}")
    private boolean botEnabled;

    @Value("${telegram.bot.token}")
    private String botToken;


    @Value("${telegram.bot.username}")
    private String botUsername;

    private final UserRepository userRepository;  // 👈 Добавляем зависимость
    private final ProjectService projectService;

    public TelegramBotConfig(UserRepository userRepository, ProjectService projectService,
                             @Value("${telegram.bot.token}") String botToken,
                             @Value("${telegram.bot.username}") String botUsername) {
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.botToken = botToken;
        this.botUsername = botUsername;
        System.out.println("botToken: " + botToken);
        System.out.println("botUsername: " + botUsername);

    }

    @Bean
    public NoteBot noteBot(TelegramBotsApi telegramBotsApi) throws Exception {

        if (!botEnabled) {
            System.out.println("⛔ TelegramBot отключён через конфигурацию. Регистрация отменена.");
            return null; // не регистрируем бота
        }
        String projectId = null;
        System.out.println("Создаем бин из TelegramBotConfig");
        System.out.println("botToken: " + botToken);
        System.out.println("botUsername: " + botUsername);
        NoteBot bot = new NoteBot(botToken, botUsername, userRepository , projectService);
        telegramBotsApi.registerBot(bot);
        return bot;
    }


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
