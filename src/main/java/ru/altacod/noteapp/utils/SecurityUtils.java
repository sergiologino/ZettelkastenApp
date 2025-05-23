// Новый файл: src/main/java/com/example/noteapp/utils/SecurityUtils.java
package ru.altacod.noteapp.utils;

import ru.altacod.noteapp.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {
    private static UserRepository userRepository = null;

    public SecurityUtils(UserRepository userRepository) {
        SecurityUtils.userRepository = userRepository;
    }

    public static String getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername(); // Вернем имя пользователя
        } else if (principal instanceof String) {
            return (String) principal; // Вернем строку, если principal — это имя пользователя
        }
        throw new IllegalStateException("Не удалось определить пользователя");
    }
}
