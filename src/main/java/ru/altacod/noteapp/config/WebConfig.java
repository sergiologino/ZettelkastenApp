package ru.altacod.noteapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // Разрешить CORS для всех эндпоинтов API
                        .allowedOrigins("http://localhost:3003") // Укажите порт вашего фронтенда
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Разрешённые методы
                        .allowedHeaders("*") // Разрешённые заголовки
                        .allowCredentials(true); // Если используются куки или авторизация
            }
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String fileStoragePath = System.getenv("FILE_STORAGE_PATH");
                String audioStoragePath = System.getenv("AUDIO_STORAGE_PATH");
                // Настраиваем маппинг для файлов
                registry.addResourceHandler("/files/audio/**") // URL для доступа к файлам
                        .addResourceLocations("file:" + audioStoragePath + "/"); // Абсолютный путь до директории с файлами

                registry.addResourceHandler("/files/documents/**")// URL для доступа к файлам
                        .addResourceLocations("file:" + fileStoragePath + "/"); // Абсолютный путь до директории с файлами
            }

        };

    }
}
