package ru.altacod.noteapp.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonManagedReference // Обозначаем "родительскую" сторону связи
    private List<Note> notes;

    @Column(name = "tlg_username", length = 32)
    private String tlgUsername;  // Telegram username

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;  // Номер телефона

    @Column(name = "billing", nullable = false)
    private boolean billing;  // Признак платного тарифа

    @Column(name = "avatar", columnDefinition = "BYTEA")
    private byte[] avatar;  // Аватар пользователя (хранится как BLOB)

    @Column(name = "avatar_url", nullable = true)
    private String avatarUrl;

    @Column(name = "color_theme", nullable = false)
    private boolean colorTheme;  // Темная/светлая тема


    @Column(name = "ask_project_before_save", nullable = false)
    private boolean askProjectBeforeSave = false; // По умолчанию выключено

    private boolean enabled = true;


    @Column(name = "telegram_chat_id", unique = true)
    private String telegramChatId;

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

//    public void setAvatar(MultipartFile file) throws IOException {
//        this.avatar = file.getBytes();
//    }
public void setAvatar(byte[] avatar) {
        this.avatar = null;
}

    public boolean isColorTheme() {
        return colorTheme;
    }

    public void setColorTheme(boolean colorTheme) {
        this.colorTheme = colorTheme;
    }

    public String getTelegramChatId() {return telegramChatId;

    };

    public void setTelegramChatId(String chatId) {
        this.telegramChatId = chatId;
    };

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isAskProjectBeforeSave() {
        return askProjectBeforeSave;
    }

    public void setAskProjectBeforeSave(boolean askProjectBeforeSave) {
        this.askProjectBeforeSave = askProjectBeforeSave;
    }
}
