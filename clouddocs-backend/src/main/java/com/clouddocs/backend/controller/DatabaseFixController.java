package com.clouddocs.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class DatabaseFixController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/fix-roles")
    public ResponseEntity<String> fixRoles() {
        try {
            // Store update results as Integer objects to handle potential nulls
            Integer updated1 = jdbcTemplate.update("UPDATE roles SET name = 'MANAGER' WHERE name = 'ROLE_MANAGER'");
            Integer updated2 = jdbcTemplate.update("UPDATE roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN'");
            Integer updated3 = jdbcTemplate.update("UPDATE roles SET name = 'USER' WHERE name = 'ROLE_USER'");

            // Safely handle null values by defaulting to 0
            int safeUpdated1 = updated1 != null ? updated1 : 0;
            int safeUpdated2 = updated2 != null ? updated2 : 0;
            int safeUpdated3 = updated3 != null ? updated3 : 0;

            // Also fix workflow step roles if they exist
            try {
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'MANAGER' WHERE role_name = 'ROLE_MANAGER'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'ADMIN' WHERE role_name = 'ROLE_ADMIN'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'USER' WHERE role_name = 'ROLE_USER'");
            } catch (Exception e) {
                // Workflow step roles table might not exist - that's okay
            }

            int totalUpdated = safeUpdated1 + safeUpdated2 + safeUpdated3;
            
            return ResponseEntity.ok("✅ Database roles fixed! Updated " + totalUpdated + " role records. " +
                "ROLE_MANAGER→MANAGER: " + safeUpdated1 + ", " +
                "ROLE_ADMIN→ADMIN: " + safeUpdated2 + ", " +
                "ROLE_USER→USER: " + safeUpdated3);
                
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage() + 
                "\nThis might be due to constraint violations. Check if 'MANAGER' already exists in your roles table.");
        }
    }
}
