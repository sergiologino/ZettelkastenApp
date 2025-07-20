package ru.altacod.noteapp.controller;

import io.jsonwebtoken.Claims;
import org.springframework.http.*;
import ru.altacod.noteapp.config.JwtTokenProvider;
import ru.altacod.noteapp.dto.NoteDTO;
import ru.altacod.noteapp.dto.UserRegistrationDTO;
import ru.altacod.noteapp.model.Project;
import ru.altacod.noteapp.model.User;
import ru.altacod.noteapp.repository.ProjectRepository;
import ru.altacod.noteapp.repository.UserRepository;
import ru.altacod.noteapp.service.ProjectService;
import ru.altacod.noteapp.service.UserService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.client.RestTemplate;
import ru.altacod.noteapp.utils.JwtUtils;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = {
//         "http://localhost:3000",
//         "https://sergiologino-note-app-new-design-eaa6.twc1.net",
//         "https://altanote.ru"
// })
@Tag(name = "Auth API", description = "API для регистрации и авторизации")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, String> stateStore = new HashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final JwtUtils jwtUtils;
//    private final User user;


    public AuthController(UserService userService, UserRepository userRepository, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder, ProjectService projectService, ProjectRepository projectRepository, JwtUtils jwtUtils) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.jwtUtils = jwtUtils;

    }


    @PostMapping("/login")
    @Operation(summary = "Авторизация пользователя", description = "Авторизация пользователя и выдача токенов")
    public ResponseEntity<Map<String, String>> login(@RequestBody User user) {
    //    System.out.println("Попытка авторизации: " + user.getUsername());
    //    System.out.println("Введенный пароль: " + user.getPassword());

        return userService.findByUsername(user.getUsername())
                .map(u -> {
//                    System.out.println("Проверяем пароль: " + user.getPassword());
//                    System.out.println("Пароль из БД: " + u.getPassword());
//                    System.out.println("Совпадает ли пароль? " + passwordEncoder.matches(user.getPassword(), u.getPassword()));

                    if (!passwordEncoder.matches(user.getPassword(), u.getPassword())) {
                        return ResponseEntity.status(401).body(Collections.singletonMap("error", "Неверный логин или пароль"));
                    }
                    String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
                    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

                    Map<String, String> tokens = new HashMap<>();
                    tokens.put("accessToken", accessToken);
                    tokens.put("refreshToken", refreshToken);
//                    System.out.println("Пароль, который пришел: " + user.getPassword());
//                    System.out.println("Пароль из БД: " + u.getPassword());
//                    System.out.println("Пароли совпадают? " + passwordEncoder.matches(user.getPassword(), u.getPassword()));
                    return ResponseEntity.ok(tokens);
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @RestController
    public class OAuth2LoginController {

        @GetMapping("/dashboard")
        public String getDashboard(@AuthenticationPrincipal OAuth2User user) {
            return "Добро пожаловать, " + user.getAttribute("name");
        }
    }

    @GetMapping("/oauth2/authorize/yandex")
    @Operation(summary = "Инициализация OAuth с Яндекс")
    public ResponseEntity<Map<String, String>> initiateYandexOAuth() {

        String state = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
//        System.out.println("Сгенерированное state (до сохранения): " + state);
        stateStore.put(state, "valid"); // Сохраняем state в памяти
        Map<String, String> response = new HashMap<>();
        response.put("state", state);

//        System.out.println("Инициализация OAuth с Яндекс, stateStore: " + stateStore);

        return ResponseEntity.ok(response);
    }

    //  /auth/oauth2/${provider}/callback
    @GetMapping("/login/oauth2/code/yandex")
    @Operation(summary = "Обработка редиректа от Яндекса")
    public ResponseEntity<Void> handleYandexCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        System.out.println("Обработка редиректа от Яндекса, method handleYandexCallback ");
        if (!stateStore.containsKey(state)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "http://localhost:3000/auth?error=state")
                .build();
        }
        stateStore.remove(state);

        RestTemplate restTemplate = new RestTemplate();
        String tokenUri = "https://oauth.yandex.ru/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_id", "a0bc7b7381a84739be01111f12d9447e");
        body.add("client_secret", "c0701b6fad07403c8a8b6f9e99874e1f");
//        body.add("redirect_uri", "https://sergiologino-zettelkastenapp-19f3.twc1.net/login/oauth2/code/yandex");
        body.add("redirect_uri", "https://localhost:8080/login/oauth2/code/yandex");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        System.out.println("Запрос на Яндекс: " + request);

        ResponseEntity<String> responseYand = restTemplate.postForEntity(tokenUri, request, String.class);

        if (responseYand.getStatusCode().is2xxSuccessful()) {
            // Можно добавить свою логику сохранения пользователя и т.д.
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "http://localhost:3000/")
                .build();
        } else {
            System.out.println("Обработка редиректа от Яндекса не сработала, method handleYandexCallback ");
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "http://localhost:3000/auth?error=oauth")
                .build();
        }
    }

    @GetMapping("/yandex/callback")
    @Operation(summary = "Обработка OAuth callback от фронтенда для Яндекса")
    public ResponseEntity<Map<String, Object>> handleYandexFrontendCallback(
            @RequestParam("code") String code) {
        try {
            System.out.println("=== НАЧАЛО ОБРАБОТКИ YANDEX CALLBACK ===");
            System.out.println("Получен код: " + code);
            
            // Обмен code на access token
            RestTemplate restTemplate = new RestTemplate();
            String tokenUri = "https://oauth.yandex.ru/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("client_id", "a0bc7b7381a84739be01111f12d9447e");
            body.add("client_secret", "c0701b6fad07403c8a8b6f9e99874e1f");
            body.add("redirect_uri", "http://localhost:3000/auth/yandex/callback");

            System.out.println("Отправляем запрос на Яндекс с параметрами:");
            System.out.println("- grant_type: authorization_code");
            System.out.println("- code: " + code);
            System.out.println("- client_id: a0bc7b7381a84739be01111f12d9447e");
            System.out.println("- redirect_uri: http://localhost:3000/auth/yandex/callback");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> responseYand = restTemplate.postForEntity(tokenUri, request, String.class);

            System.out.println("Ответ от Яндекса:");
            System.out.println("- Статус: " + responseYand.getStatusCode());
            System.out.println("- Тело ответа: " + responseYand.getBody());

            if (!responseYand.getStatusCode().is2xxSuccessful()) {
                System.err.println("ОШИБКА: Яндекс вернул не успешный статус: " + responseYand.getStatusCode());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Ошибка получения токена от Яндекса: " + responseYand.getStatusCode()));
            }

            // Парсим JSON ответ от Яндекса
            String responseBody = responseYand.getBody();
            System.out.println("Парсим ответ от Яндекса: " + responseBody);
            
            // Используем Jackson ObjectMapper для парсинга JSON
            String accessToken = null;
            if (responseBody != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    if (jsonNode.has("access_token")) {
                        accessToken = jsonNode.get("access_token").asText();
                        System.out.println("Извлечен access_token (Jackson): " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга JSON с Jackson: " + e.getMessage());
                    
                    // Fallback: ручной парсинг
                    if (responseBody.contains("access_token")) {
                        int startIndex = responseBody.indexOf("\"access_token\":\"") + 16;
                        int endIndex = responseBody.indexOf("\"", startIndex);
                        if (startIndex > 15 && endIndex > startIndex) {
                            accessToken = responseBody.substring(startIndex, endIndex);
                            System.out.println("Извлечен access_token (ручной парсинг): " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");
                        }
                    }
                    
                    if (accessToken == null) {
                        System.err.println("Попробуем альтернативный способ парсинга...");
                        
                        // Альтернативный способ парсинга
                        String[] parts = responseBody.split("\"access_token\":\"");
                        if (parts.length > 1) {
                            String tokenPart = parts[1];
                            String[] tokenParts = tokenPart.split("\"");
                            if (tokenParts.length > 0) {
                                accessToken = tokenParts[0];
                                System.out.println("Извлечен access_token (альтернативный способ): " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");
                            }
                        }
                    }
                }
            }

            if (accessToken == null) {
                System.err.println("ОШИБКА: Не удалось извлечь access_token из ответа Яндекса");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Не удалось получить access_token от Яндекса"));
            }

            // Получаем информацию о пользователе от Яндекса
            System.out.println("Получаем информацию о пользователе от Яндекса...");
            String userInfoUri = "https://login.yandex.ru/info";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "OAuth " + accessToken);
            
            HttpEntity<String> userInfoRequest = new HttpEntity<>(userInfoHeaders);
            ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                userInfoUri, 
                HttpMethod.GET, 
                userInfoRequest, 
                String.class
            );

            System.out.println("Ответ с информацией о пользователе:");
            System.out.println("- Статус: " + userInfoResponse.getStatusCode());
            System.out.println("- Тело ответа: " + userInfoResponse.getBody());

            if (!userInfoResponse.getStatusCode().is2xxSuccessful()) {
                System.err.println("ОШИБКА: Не удалось получить информацию о пользователе: " + userInfoResponse.getStatusCode());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Ошибка получения информации о пользователе"));
            }

            // Парсим информацию о пользователе
            String userInfoBody = userInfoResponse.getBody();
            String email = null;
            String username = null;
            String realName = null;

            if (userInfoBody != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode userInfoNode = objectMapper.readTree(userInfoBody);
                    
                    if (userInfoNode.has("default_email")) {
                        email = userInfoNode.get("default_email").asText();
                        System.out.println("Извлечен email (Jackson): " + email);
                    }
                    
                    if (userInfoNode.has("real_name")) {
                        realName = userInfoNode.get("real_name").asText();
                        System.out.println("Извлечено real_name (Jackson): " + realName);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка парсинга информации о пользователе с Jackson: " + e.getMessage());
                    
                    // Fallback: ручной парсинг
                    if (userInfoBody.contains("\"default_email\":")) {
                        int startIndex = userInfoBody.indexOf("\"default_email\":\"") + 17;
                        int endIndex = userInfoBody.indexOf("\"", startIndex);
                        if (startIndex > 16 && endIndex > startIndex) {
                            email = userInfoBody.substring(startIndex, endIndex);
                            System.out.println("Извлечен email (ручной парсинг): " + email);
                        }
                    }
                    
                    if (userInfoBody.contains("\"real_name\":")) {
                        int startIndex = userInfoBody.indexOf("\"real_name\":\"") + 13;
                        int endIndex = userInfoBody.indexOf("\"", startIndex);
                        if (startIndex > 12 && endIndex > startIndex) {
                            realName = userInfoBody.substring(startIndex, endIndex);
                            System.out.println("Извлечено real_name (ручной парсинг): " + realName);
                        }
                    }
                }
            }

            // Определяем username (email или real_name)
            if (email != null) {
                username = email;
            } else if (realName != null) {
                username = realName.replaceAll("\\s+", "_").toLowerCase();
            } else {
                username = "yandex_user_" + System.currentTimeMillis();
            }
            System.out.println("Определен username: " + username);

            // Ищем или создаем пользователя в БД
            User user = userRepository.findByUsername(username);
            if (user == null) {
                System.out.println("Создаем нового пользователя...");
                // Создаем нового пользователя (автоматическая регистрация)
                user = new User();
                user.setUsername(username);
                user.setEmail(email);
                
                // Генерируем случайный пароль (пользователь будет входить через OAuth)
                String randomPassword = UUID.randomUUID().toString();
                user.setPassword(passwordEncoder.encode(randomPassword));
                
                user = userService.registerUser(user);
                
                // Создаем проект по умолчанию для нового пользователя
                Project defaultProject = new Project();
                defaultProject.setName("Мой проект");
                defaultProject.setDescription("Проект по умолчанию");
                defaultProject.setColor("#BD10E0");
                defaultProject.setPosition(1);
                defaultProject.setDefault(true);
                defaultProject.setUserId(user.getId());
                defaultProject.setCreatedAt(LocalDateTime.now());
                
                projectRepository.save(defaultProject);
                
                System.out.println("Создан новый пользователь через Яндекс OAuth: " + username);
            } else {
                System.out.println("Найден существующий пользователь: " + username);
            }

            // Генерируем JWT токены
            System.out.println("Генерируем JWT токены...");
            String jwtAccessToken = jwtTokenProvider.generateAccessToken(username);
            String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", jwtAccessToken);
            response.put("refreshToken", jwtRefreshToken);
            response.put("user", Map.of(
                "id", user.getId().toString(),
                "username", user.getUsername(),
                "email", user.getEmail()
            ));

            System.out.println("=== УСПЕШНО ЗАВЕРШЕНА ОБРАБОТКА YANDEX CALLBACK ===");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("=== ОШИБКА ПРИ ОБРАБОТКЕ YANDEX CALLBACK ===");
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Внутренняя ошибка сервера: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "Регистрация пользователя",
            description = "Регистрация нового пользователя в системе и синхронизация данных с основным приложением.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Пользователь успешно зарегистрирован.",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Некорректные данные или пользователь уже существует.",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Ошибка при синхронизации с основным приложением.",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<String> register(@RequestBody UserRegistrationDTO userDTO) {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("Контекст не должен быть установлен при регистрации!");
        }

        // 🛑 Проверяем, есть ли пользователь в БД перед синхронизацией
        if (userService.findByUsername(userDTO.getUsername()).isPresent()) {
//            System.out.println("ОБНАРУЖЕН ПОЛЬЗОВАТЕЛЬ! " + userDTO.getUsername());
            return ResponseEntity.badRequest().body("Пользователь уже существует.");
        }
        String hashedPassword = passwordEncoder.encode(userDTO.getPassword());
//        System.out.println("Хешированный пароль: " + hashedPassword);

        // ✅ Сначала вызываем `syncUser()`, но НЕ сохраняем пользователя!
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(hashedPassword);
        user.setEmail(userDTO.getEmail());


        ResponseEntity<User> response = sync(user.getUsername());
//        System.out.println("Ответ syncUser: " + response.getStatusCode());

        if (!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(response.getStatusCode()).body("Ошибка при синхронизации с приложением.");
        }

        // ✅ Теперь сохраняем пользователя в БД только после успешного syncUser
        //userService.registerUser(user);

        return ResponseEntity.ok("Пользователь успешно зарегистрирован.");
    }



    @PostMapping("/sync")
    public ResponseEntity<User> sync(@RequestHeader("Authorization") String tokenHeader) {
        try {
            String token = tokenHeader.replace("Bearer ", "");
            Claims claims = jwtUtils.parseToken(token);

            String username = claims.getSubject(); // или UUID, если ты переделал
            User user = userRepository.findByUsername(username);

            if (user == null) {
                // ⬇️ создаём нового пользователя
                user = new User();
                user.setUsername(username); // если есть email — заполни и его
                //            user.setCreatedAt(LocalDateTime.now());

                userService.registerUser(user); // сохранение

                // ✅ проект по умолчанию
                Project defaultProject = new Project();
                defaultProject.setName("Мой проект");
                defaultProject.setDescription("Проект по умолчанию");
                defaultProject.setColor("#BD10E0");
                defaultProject.setPosition(1);
                defaultProject.setDefault(true);
                defaultProject.setUserId(user.getId());
                defaultProject.setCreatedAt(LocalDateTime.now());

                projectRepository.save(defaultProject);
            }

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            //        log.error("Ошибка в /auth/sync", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

