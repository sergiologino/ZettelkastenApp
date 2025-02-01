package com.example.noteapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        System.out.println("Запрос: " + requestURI);
        // Разрешить запросы без токена для регистрации и логина
        if (requestURI.startsWith("/api/auth/register") || requestURI.startsWith("/api/auth/login") || requestURI.startsWith("/auth/login") || requestURI.startsWith("/auth/register")) {
            chain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // 💡 Извлекаем токен из заголовка
            username = jwtTokenProvider.getUsernameFromToken(token); // 💡 Получаем username из токена
            System.out.println("username: " + username);
        }

        if (token != null && username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = jwtTokenProvider.loadUserByUsername(username);

            if (jwtTokenProvider.validateToken(token, userDetails)) {
                var authentication = jwtTokenProvider.getAuthentication(token, userDetails);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.getContext().setAuthentication(null);
            }
        }
    }
}

