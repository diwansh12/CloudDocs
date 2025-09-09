package com.clouddocs.backend.service;

/**
 * 🚨 Custom exception for embedding generation failures
 */
public class EmbeddingException extends Exception {
    
    private final String providerName;
    private final int statusCode;
    
    public EmbeddingException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
        this.statusCode = 500;
    }
    
    public EmbeddingException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.statusCode = 500;
    }
    
    public EmbeddingException(String providerName, String message, int statusCode) {
        super(message);
        this.providerName = providerName;
        this.statusCode = statusCode;
    }
    
    public EmbeddingException(String providerName, String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.statusCode = statusCode;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    @Override
    public String toString() {
        return String.format("EmbeddingException{provider='%s', statusCode=%d, message='%s'}", 
            providerName, statusCode, getMessage());
    }
}
