package ru.altacod.noteapp.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.debug(false); // 💡 Выключаем debug mode
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
//                .cors(withDefaults())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/sync",
                                "/api/auth/**",
                                "/oauth2/authorize/yandex",
                                "/login/oauth2/code/yandex",
                                "/api/notes/download/audio/**",
                                "/api/notes/download/file/**",
                                "/uploads/**",
                                "/api/users/debug/uploads/**"
                        ).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // Разрешаем preflight-запросы
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/notes/mixed").permitAll() // Разрешить запрос
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/notes/text").permitAll() // Разрешить запрос
                        .requestMatchers("/api/notes").authenticated()  // ✅ Доступ только авторизованным
                        .requestMatchers("/api/projects/**").authenticated()
//                        .requestMatchers("/api/notes/download/audio/**").authenticated()
//                        .requestMatchers("/api/notes/download/file/**").authenticated()
                        .anyRequest().authenticated()

                )
                .anonymous(anonymous -> anonymous.disable()) // 💡 Отключаем анонимную аутентификацию

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            System.out.println("🚨 Запрос отклонён в AuthorizationFilter: " + request.getRequestURI());
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Доступ запрещён");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // Убираем oauth2Login, так как используем кастомную обработку
                // .oauth2Login(oauth2 -> oauth2
                //         .loginPage("/login/oauth2/code/yandex") // Настройка Yandex OAuth2
                //         .defaultSuccessUrl("http://localhost:3000/")
                //         .failureUrl("http://localhost:3000/auth?error=oauth")
                // )
                .logout(logout -> logout
                        .logoutSuccessUrl("/auth")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")

                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

//        System.out.println("SecurityConfig загружен. Проверяем доступ к /api/projects.");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://sergiologino-note-app-new-design-eaa6.twc1.net",
                "https://altanote.ru"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // ⬅️ важно!

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}