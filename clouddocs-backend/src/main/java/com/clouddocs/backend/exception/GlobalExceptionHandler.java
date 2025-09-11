package com.clouddocs.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.error("❌ File size exceeded: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "File Too Large");
        response.put("message", "Uploaded file exceeds maximum allowed size of 5MB");
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException ex) {
        logger.error("❌ Multipart exception: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "File Upload Error");
        response.put("message", "Error processing uploaded file");
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        logger.error("❌ Unhandled exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred while processing your request");
        response.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
