package ru.altacod.noteapp.controller;

import ru.altacod.noteapp.config.JwtTokenProvider;
import ru.altacod.noteapp.model.User;
import ru.altacod.noteapp.repository.ProjectRepository;
import ru.altacod.noteapp.repository.UserRepository;
import ru.altacod.noteapp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProjectRepository projectRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, ProjectRepository projectRepository, UserService userService) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String token) {
        String userName = jwtTokenProvider.getUserIdFromToken(token.replace("Bearer ", ""));
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new RuntimeException("Пользователь не найден: " + userName);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("tlgUsername", user.getTlgUsername());
        response.put("phoneNumber", user.getPhoneNumber());

        // Корректно возвращаем avatarUrl
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            response.put("avatarUrl", user.getAvatarUrl());
        } else if (user.getAvatar() != null && user.getAvatar().length > 0) {
            String base64Avatar = "data:image/png;base64," + Base64.getEncoder().encodeToString(user.getAvatar());
            response.put("avatarUrl", base64Avatar);
        } else {
            response.put("avatarUrl", "/default-avatar.png");
        }
        return ResponseEntity.ok(response);
    }


    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(@RequestBody User user) {
        // Проверяем, существует ли пользователь с указанным именем
        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            return ResponseEntity.badRequest().body("Пользователь уже существует.");
        }

        // Сохраняем нового пользователя
        userRepository.save(user);
        return ResponseEntity.ok("Пользователь успешно синхронизирован.");
    }


    @PutMapping("/{userId}/avatar")
    public ResponseEntity<Map<String, String>> updateAvatar(@PathVariable UUID userId, @RequestParam("avatar") MultipartFile avatar) {
        try {
            String avatarUrl = userService.updateUserAvatar(userId, avatar);

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Ошибка при обновлении аватара."));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable UUID userId, @RequestBody User userData) {
        try {
            userService.updateUserData(userId, userData);
            return ResponseEntity.ok("Профиль успешно обновлён.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при обновлении профиля.");
        }
    }


    public User getUserByUserId(UUID userId) {
        User existingUser = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден."));
        return existingUser;
    }
}