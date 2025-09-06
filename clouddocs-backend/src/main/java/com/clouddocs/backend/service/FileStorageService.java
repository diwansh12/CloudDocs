package com.clouddocs.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private final Path fileStorageLocation;
    
    public FileStorageService(@Value("${file.upload.dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

 @PostConstruct
public void initDirectories() {
    try {
        // Ensure main upload directory exists
        Files.createDirectories(this.fileStorageLocation);
        
        // Ensure profile-pictures subdirectory exists
        Path profilePicturesDir = this.fileStorageLocation.resolve("profile-pictures");
        Files.createDirectories(profilePicturesDir);
        
        System.out.println("✅ Upload directories initialized:");
        System.out.println("    Main: " + this.fileStorageLocation.toAbsolutePath());
        System.out.println("    Profile Pictures: " + profilePicturesDir.toAbsolutePath());
        
    } catch (IOException e) {
        throw new RuntimeException("Could not create upload directories", e);
    }
}

    
    /**
     * Store file with date-based folder structure
     */
    public String storeFile(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RuntimeException("File must have a valid filename");
        }
        
        originalFileName = StringUtils.cleanPath(originalFileName);
        
        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
            }
            
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String uniqueFileName = generateUniqueFileName(originalFileName, fileExtension);
            
            // Create directory structure by date (year/month)
            LocalDateTime now = LocalDateTime.now();
            String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path targetLocation = this.fileStorageLocation.resolve(yearMonth).resolve(uniqueFileName);
            
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return yearMonth + "/" + uniqueFileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
    
    /**
     * ✅ NEW: Store file in specific subfolder
     */
    public String storeFile(MultipartFile file, String subfolder) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RuntimeException("File must have a valid filename");
        }
        
        originalFileName = StringUtils.cleanPath(originalFileName);
        
        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
            }
            
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String uniqueFileName = generateUniqueFileName(originalFileName, fileExtension);
            
            // Use subfolder instead of date-based structure
            Path targetLocation = this.fileStorageLocation.resolve(subfolder).resolve(uniqueFileName);
            
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return subfolder + "/" + uniqueFileName;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
    
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }
    
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName, ex);
        }
    }
    
    public long getFileSize(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.size(filePath);
        } catch (IOException ex) {
            return 0;
        }
    }
    
    public boolean fileExists(String fileName) {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        return Files.exists(filePath);
    }
    
    private String generateUniqueFileName(String originalFileName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        String baseName = originalFileName;
        if (baseName.contains(".")) {
            baseName = baseName.substring(0, baseName.lastIndexOf("."));
        }
        
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = "file";
        }
        
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }
        
        return String.format("%s_%s_%s%s", baseName, timestamp, uuid, extension);
    }
    
    public String getAbsolutePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize().toString();
    }
}
