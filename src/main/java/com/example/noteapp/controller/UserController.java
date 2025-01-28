package com.example.noteapp.controller;

import com.example.noteapp.config.JwtTokenProvider;
import com.example.noteapp.model.Project;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.ProjectRepository;
import com.example.noteapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
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
        String userId = jwtTokenProvider.getUserIdFromToken(token.replace("Bearer ", ""));
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Пользователь уже существует.");
        }
        userRepository.save(user);
        return ResponseEntity.ok("Пользователь успешно синхронизирован.");
    }

    public List<Project> getNotesForCurrentUser(UUID userId) {
        return projectRepository.findAllByUserId(userId);
    }
}