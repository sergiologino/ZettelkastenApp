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