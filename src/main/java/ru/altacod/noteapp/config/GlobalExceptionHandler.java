package ru.altacod.noteapp.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Объект не найден", ex, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex, HttpServletRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", ex, request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, Exception ex, HttpServletRequest request) {
        log.error("❌ [{} {}] {}: {}", request.getMethod(), request.getRequestURI(), message, ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                status.value(),
                message,
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    // DTO для ответа
    public record ErrorResponse(
            int status,
            String message,
            String path,
            LocalDateTime timestamp
    ) {}

}
