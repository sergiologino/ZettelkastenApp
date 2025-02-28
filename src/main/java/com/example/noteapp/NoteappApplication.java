package com.example.noteapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication(scanBasePackages = "com.example.noteapp")
@EnableWebMvc
public class NoteappApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteappApplication.class, args);
    }

}
