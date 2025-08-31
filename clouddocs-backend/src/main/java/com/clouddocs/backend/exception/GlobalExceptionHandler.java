package com.clouddocs.backend.exception;

import org.hibernate.LazyInitializationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<Map<String, Object>> handleLazyInitializationException(
            LazyInitializationException ex, WebRequest request) {
        
        System.err.println("❌ LazyInitializationException: " + ex.getMessage());
        ex.printStackTrace();
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Data Loading Error");
        response.put("message", "Unable to load document data. Service temporarily unavailable.");
        response.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(
            Exception ex, WebRequest request) {
        
        System.err.println("❌ Unhandled exception: " + ex.getMessage());
        ex.printStackTrace();
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", 500);
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        response.put("path", request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
