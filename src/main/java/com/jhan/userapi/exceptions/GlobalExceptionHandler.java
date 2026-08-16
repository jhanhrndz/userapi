package com.jhan.userapi.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "";
    }

    private Map<String, Object> buildErrorResponse(HttpStatus status, String error, String message,
                                                   WebRequest request, String traceId, Map<String, String> errors) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", getPath(request));
        response.put("traceId", traceId);
        if (errors != null && !errors.isEmpty()) {
            response.put("errors", errors);
        }
        return response;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Error de Validación",
                "Los datos enviados no son válidos", request, traceId, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        String traceId = generateTraceId();
        String message = "Cuerpo de la petición inválido o faltante. Se requiere un cuerpo JSON válido.";

        if (ex.getCause() instanceof MismatchedInputException) {
            message = "Cuerpo de la petición vacío. Se requiere un JSON con los campos requeridos.";
        } else if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) ex.getCause();
            message = String.format("Formato inválido para el campo '%s'. Valor esperado: %s",
                    ife.getPath().get(0).getFieldName(), ife.getTargetType().getSimpleName());
        }

        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Cuerpo de Petición Inválido",
                message, request, traceId, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Argumento Inválido",
                ex.getMessage(), request, traceId, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, "Recurso No Encontrado",
                ex.getMessage(), request, traceId, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(DuplicateResourceException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.CONFLICT, "Conflicto",
                ex.getMessage(), request, traceId, null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.BAD_REQUEST, "Credenciales Inválidas",
                ex.getMessage(), request, traceId, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.FORBIDDEN, "Acceso Denegado",
                "No tiene permisos suficientes para acceder a este recurso", request, traceId, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.UNAUTHORIZED, "No Autorizado",
                "Credenciales inválidas o token expirado", request, traceId, null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest request) {
        String traceId = generateTraceId();
        Map<String, Object> response = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno",
                "Ha ocurrido un error inesperado. Contacte al administrador.", request, traceId, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest request) {
        String traceId = generateTraceId();
        String message = String.format("El endpoint %s %s no existe", ex.getHttpMethod(), ex.getRequestURL());
        Map<String, Object> response = buildErrorResponse(HttpStatus.NOT_FOUND, "Recurso No Encontrado",
                message, request, traceId, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String traceId = generateTraceId();
        String message = String.format("Método HTTP %s no soportado para este endpoint. Métodos permitidos: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        Map<String, Object> response = buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Método No Permitido",
                message, request, traceId, null);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }
}