package com.clouddocs.backend.exception;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, 
            HttpHeaders headers, 
            HttpStatusCode status, 
            WebRequest request) {
        
        String path = request.getDescription(false).replace("uri=", "");
        logger.error("❌ JSON parsing error for path: {} - Error: {}", path, ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", "Bad Request");
        body.put("path", path);
        
        // ✅ Handle specific JSON errors with helpful messages
        if (ex.getCause() instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException) ex.getCause();
            body.put("message", String.format("Unknown field '%s' in request. Expected fields: %s", 
                upe.getPropertyName(), upe.getKnownPropertyIds()));
        } else {
            body.put("message", "Invalid JSON format in request body");
        }
        
        // ✅ Add CORS headers
        headers.add("Access-Control-Allow-Origin", "https://cloud-docs-tan.vercel.app");
        headers.add("Access-Control-Allow-Credentials", "true");
        
        return new ResponseEntity<>(body, headers, status);
    }
}

