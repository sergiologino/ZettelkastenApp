package com.example.noteapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CustomFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

//        System.out.println("🔎 [CustomFilter] Передача запроса в следующий фильтр: " + req.getMethod() + " " + req.getRequestURI());

        chain.doFilter(request, response);

//        System.out.println("✅ [CustomFilter] Запрос обработан, статус: " + res.getStatus());
    }
}
