package com.example.noteapp.config;

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
                System.out.println("WebConfig загружен: CORS включен");
                registry.addMapping("/api/**") // Разрешить CORS для всех эндпоинтов API
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Разрешённые методы
                        .allowedHeaders("*") // Разрешённые заголовки
                        .allowCredentials(true); // Если используются куки или авторизация
            }
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Настраиваем маппинг для файлов
                registry.addResourceHandler("/files/audio/**") // URL для доступа к файлам
                        .addResourceLocations("file:/E:/uploaded/uploaded-audio/"); // Абсолютный путь до директории с файлами

                registry.addResourceHandler("/files/documents/*н*")// URL для доступа к файлам
                        .addResourceLocations("file:/E:/uploaded/uploaded-files/");// Абсолютный путь до директории с файлами
            }

        };

    }
}
