package com.example.noteapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Setter
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Note> notes;

    @Column(name = "tlg_username", length = 32)
    private String tlgUsername;  // Telegram username

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;  // Номер телефона

    @Column(name = "billing", nullable = false)
    private boolean billing;  // Признак платного тарифа

    @Lob
    @Column(name = "avatar", columnDefinition = "BYTEA")
    private byte[] avatar;  // Аватар пользователя (хранится как BLOB)

    @Column(name = "color_theme", nullable = false)
    private boolean colorTheme;  // Темная/светлая тема

    private boolean enabled = true;

    // Геттеры и сеттеры
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public String getTlgUsername() {
        return tlgUsername;
    }

    public void setTlgUsername(String tlgUsername) {
        this.tlgUsername = tlgUsername;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isBilling() {
        return billing;
    }

    public void setBilling(boolean billing) {
        this.billing = billing;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public boolean isColorTheme() {
        return colorTheme;
    }

    public void setColorTheme(boolean colorTheme) {
        this.colorTheme = colorTheme;
    }
}
