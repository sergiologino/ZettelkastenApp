package com.example.noteapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.noteapp")
public class NoteappApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteappApplication.class, args);
    }

}
