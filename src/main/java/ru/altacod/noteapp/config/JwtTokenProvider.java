package ru.altacod.noteapp.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.altacod.noteapp.model.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.expiration.access}")
    private long accessTokenExpiration;

    @Value("${jwt.expiration.refresh}")
    private long refreshTokenExpiration;

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

//    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long validityInMilliseconds = accessTokenExpiration;// 3600000; // 1 час
    private final UserDetailsService userDetailsService;

    public JwtTokenProvider(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public String generateAccessToken(String username) {

        return generateToken(username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username) {

        return generateToken(username, refreshTokenExpiration);
    }

    // Генерация токена
    public String generateToken(String username, long expiration) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

//    public String generateToken(User user) {
//        Date now = new Date();
//        Date expiry = new Date(now.getTime() + accessTokenExpiration); // из настроек
//
//        return Jwts.builder()
//                .setSubject(user.getUsername()) // 👈 username как subject
//                .setIssuedAt(now)
//                .setExpiration(expiry)
//                .signWith(кey)
//                .compact();
//    }

    // Извлечение userId из токена
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            System.out.println("Claims в токене: " + claims);
            return claims.getSubject();
        } catch (Exception e) {
            System.out.println("Ошибка извлечения claims из токена: " + e.getMessage());
            return null;
        }
    }


    // Получение username из токена
    public String getUsernameFromToken(String token) {
        if (StringUtils.isEmpty(token)) return null;  // защита
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Получение любого claim из токена
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        }catch (Exception e) {
//            System.out.println("Ошибка парсинга токена: " + e.getMessage());
            return null;
        }
    }

    // Проверка валидности токена
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    // Получение UserDetails по имени пользователя
    public UserDetails loadUserByUsername(String username) {
        return userDetailsService.loadUserByUsername(username);
    }

    // Получение Authentication из токена
    public Authentication getAuthentication(String token, UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}