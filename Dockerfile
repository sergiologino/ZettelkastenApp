## Используем официальный образ OpenJDK 17 с минимальным размером
#FROM openjdk:17-jdk-slim
#
## Указываем рабочую директорию внутри контейнера
#WORKDIR /app
#
## Копируем JAR-файл приложения в контейнер
#COPY build/libs/noteapp.jar app.jar
#
## Открываем порт 8080 для работы приложения
#EXPOSE 8080
#
## Запуск приложения с использованием переменных окружения
#ENTRYPOINT ["java", "-jar", "app.jar"]

# syntax=docker/dockerfile:1

# --- Этап сборки ---
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /app

# Копируем gradle и зависимости отдельно — для кэширования
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN gradle build -x test --no-daemon || return 0

# Копируем всё и собираем
COPY . .
RUN gradle bootJar -x test --no-daemon

# --- Этап запуска ---
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
WORKDIR /app

# Копируем собранный JAR из предыдущего этапа
COPY --from=build /app/build/libs/*.jar app.jar

# Порт, который слушает Spring Boot
EXPOSE 8080

# Переменные среды
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]