package com.jhan.userapi.config.filter;

import com.jhan.userapi.config.handler.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;

public class CustomExceptionTranslationFilter extends ExceptionTranslationFilter {

    public CustomExceptionTranslationFilter() {
        super(new CustomAuthenticationEntryPoint());
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        try {
            super.doFilter(request, response, chain);
        } catch (jakarta.servlet.ServletException e) {
            Throwable cause = e.getCause();
            
            // Si la causa es NoHandlerFoundException, re-lanzar
            if (cause instanceof NoHandlerFoundException) {
                throw (NoHandlerFoundException) cause;
            }
            
            // Si la causa es HttpRequestMethodNotSupportedException, re-lanzar
            if (cause instanceof HttpRequestMethodNotSupportedException) {
                throw (HttpRequestMethodNotSupportedException) cause;
            }
            
            throw e;
        }
    }
}