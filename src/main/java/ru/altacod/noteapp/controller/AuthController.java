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
    public ResponseEntity<String> handleYandexCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {
        if (!stateStore.containsKey(state)) {
            return ResponseEntity.badRequest().body("Invalid state parameter");
        }

        // Удаляем state, чтобы избежать повторного использования
        stateStore.remove(state);

        // Отправляем запрос на получение accessToken
        RestTemplate restTemplate = new RestTemplate();
        String tokenUri = "https://oauth.yandex.ru/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_id", "a0bc7b7381a84739be01111f12d9447e");
        body.add("client_secret", "c0701b6fad07403c8a8b6f9e99874e1f");
        body.add("redirect_uri", "https://sergiologino-zettelkastenapp-19f3.twc1.net/login/oauth2/code/yandex");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        System.out.println("Запрос на Яндекс: " + request);

        ResponseEntity<String> responseYand = restTemplate.postForEntity(tokenUri, request, String.class);


        if (responseYand.getStatusCode().is2xxSuccessful()) {

            String appBackendUrl = "http://sergiologino-zettelkastenapp-19f3.twc1.net/api/users/sync";
            ResponseEntity<String> response = restTemplate.postForEntity(appBackendUrl, request, String.class);

            return ResponseEntity.ok("Токен успешно получен: " + response.getBody());
        } else {
            return ResponseEntity.status(responseYand.getStatusCode()).body("Ошибка получения токена");
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

