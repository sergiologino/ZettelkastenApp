package com.example.noteapp.controller;

import com.example.noteapp.config.JwtTokenUtil;
import com.example.noteapp.model.User;
import com.example.noteapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@RequestHeader("Authorization") String token) {
        String username = jwtTokenUtil.getUsernameFromToken(token.replace("Bearer ", ""));
        User user = userRepository.findByUsername(username);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}