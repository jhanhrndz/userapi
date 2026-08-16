package com.jhan.userapi.config.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100) // Después de Spring Security (100)
public class PreSecurityExceptionFilter implements jakarta.servlet.Filter {

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            chain.doFilter(request, response);
        } catch (jakarta.servlet.ServletException e) {
            Throwable cause = e.getCause();
            
            // Si la causa es NoHandlerFoundException, re-lanzar para que GlobalExceptionHandler lo maneje
            if (cause instanceof org.springframework.web.servlet.NoHandlerFoundException) {
                throw (org.springframework.web.servlet.NoHandlerFoundException) cause;
            }
            
            // Si la causa es HttpRequestMethodNotSupportedException, re-lanzar
            if (cause instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
                throw (org.springframework.web.HttpRequestMethodNotSupportedException) cause;
            }
            
            // Otras excepciones - re-lanzar la ServletException original
            throw e;
        }
    }
}