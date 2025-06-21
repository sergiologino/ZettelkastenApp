package ru.altacod.noteapp.repository;

import ru.altacod.noteapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    User findByUsername(String username);

    Optional<User> findByTlgUsername(String tlgUsername);

    Optional<User> findByTelegramChatId(String telegramChatId);


}

//    @Modifying
//    @Query("INSERT INTO users (avatar, ...) VALUES (:avatar, ...)")
//    void saveUser(@Param("avatar") byte[] avatar);

