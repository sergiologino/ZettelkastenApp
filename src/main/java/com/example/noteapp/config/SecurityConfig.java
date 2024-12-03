package com.example.noteapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // Отключаем CSRF для упрощения
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/api/notes/**" // Разрешаем публичный доступ к эндпоинтам API заметок
                        ).permitAll()
                        .anyRequest().authenticated() // Остальные эндпоинты требуют авторизации
                )
                .httpBasic(); // Базовая аутентификация для защищенных эндпоинтов
        return http.build();
    }
}
