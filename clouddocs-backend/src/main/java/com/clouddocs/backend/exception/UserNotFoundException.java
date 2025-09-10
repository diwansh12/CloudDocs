package com.clouddocs.backend.exception;

/**
 * Exception thrown when a user is not found in the system
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
    
    public UserNotFoundException(String username, boolean byUsername) {
        super("User not found with username: " + username);
    }
}
