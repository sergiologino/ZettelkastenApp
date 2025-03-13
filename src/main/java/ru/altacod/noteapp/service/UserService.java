package ru.altacod.noteapp.service;

import ru.altacod.noteapp.model.User;
import ru.altacod.noteapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {



    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    private String uploadDir = System.getenv("AVATAR_STORAGE_PATH");


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) {
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {

        return Optional.ofNullable(userRepository.findByUsername(username));
    }

    public Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }

        return Optional.of((User) authentication.getPrincipal());
    }

    @Transactional
    public String updateUserAvatar(UUID userId, MultipartFile avatarFile) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Генерируем уникальное имя файла
            String fileName = userId + "_" + avatarFile.getOriginalFilename();
            Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();
            System.out.println("путь общий для uploads "+Paths.get("uploads"));
            System.out.println("путь для сохранения аватаров: "+uploadPath);

            // Создаем папку, если ее нет
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Полный путь к файлу
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(avatarFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Сохраняем путь к файлу в БД
            String avatarUrl = "/uploads/" + fileName;
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);

            return avatarUrl; // Возвращаем новый URL
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки аватара", e);
        }
    }

    @Transactional
    public void updateUserData(UUID userId, User updatedUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));


        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        user.setTlgUsername(updatedUser.getTlgUsername());
        user.setPhoneNumber(updatedUser.getPhoneNumber());
        user.setAskProjectBeforeSave(updatedUser.isAskProjectBeforeSave());

        userRepository.save(user);
    }
}
