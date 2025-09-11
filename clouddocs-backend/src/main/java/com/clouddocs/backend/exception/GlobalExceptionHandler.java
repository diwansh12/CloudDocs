package com.clouddocs.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

// ✅ ADD THIS IMPORT - This fixes the "EOFException cannot be resolved" error
import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EOFException.class)
    public ResponseEntity<Map<String, Object>> handleEOFException(EOFException ex) {
        logger.warn("❌ Client disconnected during upload: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Upload Interrupted");
        response.put("message", "Connection lost during upload. Please try again with a stable connection.");
        response.put("code", "CLIENT_DISCONNECTED");
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException ex) {
        logger.error("❌ Multipart parsing failed: {}", ex.getMessage());
        
        String message = "Invalid file upload format";
        if (ex.getCause() instanceof EOFException) {
            message = "Upload interrupted. Please check your connection and try again.";
        } else if (ex.getMessage().contains("exceeds")) {
            message = "File size exceeds maximum allowed limit of 5MB";
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Upload Format Error");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.warn("❌ File size exceeded: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "File Too Large");
        response.put("message", "File exceeds maximum size limit of 5MB");
        response.put("maxSizeMB", 5);
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        logger.error("❌ Unhandled exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred. Please try again.");
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        
    }
}
