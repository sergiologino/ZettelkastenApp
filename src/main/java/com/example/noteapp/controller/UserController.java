package com.example.noteapp.controller;

import com.example.noteapp.config.JwtTokenProvider;
import com.example.noteapp.model.Project;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProjectRepository projectRepository;

//    @Autowired
//    private JwtTokenUtil jwtTokenUtil;

    public UserController(UserRepository userRepository, JwtTokenProvider jwtTokenProvider, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.projectRepository = projectRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@RequestHeader("Authorization") String token) {
        String userName = jwtTokenProvider.getUserIdFromToken(token.replace("Bearer ", ""));
        User user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new RuntimeException("Пользователь не найден: " + userName);
        }
        return ResponseEntity.ok(user);
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

    public List<Project> getNotesForCurrentUser(UUID userId) {
        return projectRepository.findAllByUserId(userId);
    }

    public User getUserByUserId(UUID userId) {
        User existingUser = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + userId + " не найден."));
        return existingUser;
    }
}