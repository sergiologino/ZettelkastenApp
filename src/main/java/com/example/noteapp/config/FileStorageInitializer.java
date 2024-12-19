package com.example.noteapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Component
public class FileStorageInitializer {

    @Value("${file.storage-path}")
    private String fileStoragePath;

    @Value("${audio.storage-path}")
    private String audioStoragePath;

    @PostConstruct
    public void initializeDirectories() {
        createDirectoryIfNotExists(fileStoragePath);
        createDirectoryIfNotExists(audioStoragePath);
    }

    private void createDirectoryIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Created directory: " + path);
            } else {
                throw new RuntimeException("Failed to create directory: " + path);
            }
        }
    }
}

