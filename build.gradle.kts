plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("java")
}

group = "ru.altacod"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")
    implementation("org.telegram:telegrambots-spring-boot-starter:6.5.0")
    implementation("javax.servlet:javax.servlet-api:4.0.1")
    // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jsoup:jsoup:1.17.2")
    implementation ("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly ("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly ("io.jsonwebtoken:jjwt-jackson:0.11.5")





    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    // https://mvnrepository.com/artifact/jakarta.servlet/jakarta.servlet-api
//    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.test {
    useJUnitPlatform()

    environment("DB_URL", "jdbc:postgresql://altanotedb:5x*V~TGcTfgVMX@192.168.0.6:5432/altanote_db")
    environment("DB_USERNAME", "altanotedb")
    environment("DB_PASSWORD", "5x*V~TGcTfgVMX")
    environment("JWT_SECRET", "e0ccMN3fqRE30HcE6Me2xnGF88e1xVrGwNndNzTd")
    environment("TELEGRAM_BOT_TOKEN", "7645020182:AAESVweSGS_n7ZbxFx3NNRE4YmrvZ6HsXvg")
    environment("TELEGRAM_BOT_USERNAME", "AltaBrain_bot")
    environment("FILE_STORAGE_PATH", "/data/files")
    environment("AUDIO_STORAGE_PATH", "/data/audio")
    environment("YANDEX_CLIENT_ID", "a0bc7b7381a84739be01111f12d9447e")
    environment("YANDEX_CLIENT_SECRET", "c0701b6fad07403c8a8b6f9e99874e1f")
    environment("BASE_URL", "https://server.altanote.ru")
    environment("SERVER_PORT", "8090")
    environment("MAX_FILE_SIZE", "200MB")
    environment("MAX_REQUEST_SIZE", "250MB")
    environment("EXPIRATION_TOKEN_ACCESS", "3600")
    environment("EXPIRATION_TOKEN_REFRESH", "86400")

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
tasks.bootJar {
    archiveFileName.set("app.jar")
}
