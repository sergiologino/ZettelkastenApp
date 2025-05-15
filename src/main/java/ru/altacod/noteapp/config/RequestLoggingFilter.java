package ru.altacod.noteapp.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1) // Обеспечивает приоритет — выполнится первым
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest req) {
            String method = req.getMethod();
            String uri = req.getRequestURI();
            String query = req.getQueryString();
            String fullUrl = uri + (query != null ? "?" + query : "");

            String origin = req.getHeader("Origin");
            String referer = req.getHeader("Referer");

            System.out.println("===> [HTTP REQUEST] " + method + " " + fullUrl);
            System.out.println("     Origin : " + origin);
            System.out.println("     Referer: " + referer);
        }

        chain.doFilter(request, response);
    }
}
