package com.clouddocs.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        
        logger.warn("ResponseStatusException: {} for path: {}", ex.getReason(), request.getRequestURI());
        
        Map<String, Object> body = createErrorBody(request, ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        logger.warn("IllegalArgumentException: {} for path: {}", ex.getMessage(), request.getRequestURI());
        
        Map<String, Object> body = createErrorBody(request, 
            ex.getMessage() != null ? ex.getMessage() : "Invalid request parameters");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        logger.warn("AccessDeniedException for path: {} by user: {}", 
            request.getRequestURI(), request.getRemoteUser());
        
        Map<String, Object> body = createErrorBody(request, "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        // ✅ LOG THE FULL STACK TRACE HERE
        logger.error("Unhandled exception for path: {} - Exception: {}", 
            request.getRequestURI(), ex.getClass().getSimpleName(), ex);
        
        Map<String, Object> body = createErrorBody(request, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> createErrorBody(HttpServletRequest request, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("path", request.getRequestURI());
        body.put("message", message);
        body.put("status", getStatusFromRequest(request));
        return body;
    }

    private int getStatusFromRequest(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        return status != null ? status : 500;
    }
}

