package com.example.noteapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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
        };
    }
}
