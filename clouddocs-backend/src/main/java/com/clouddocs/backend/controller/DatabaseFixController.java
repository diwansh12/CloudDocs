package com.clouddocs.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
public class DatabaseFixController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/fix-roles")
    public ResponseEntity<String> fixRolesGet() {
        return fixRoles();
    }

    @PostMapping("/fix-roles")
    public ResponseEntity<String> fixRolesPost() {
        return fixRoles();
    }

    private ResponseEntity<String> fixRoles() {
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("🔍 Database Role Fix - Converting ROLE_X to X format:\n\n");
            
            // Test database connection
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.append("✅ Database connection: OK\n\n");
            
            // Show current roles
            result.append("📋 BEFORE - Current roles:\n");
            jdbcTemplate.query("SELECT id, name FROM roles ORDER BY id", rs -> {
                result.append(String.format("  ID: %d, Name: '%s'\n", 
                    rs.getLong("id"), rs.getString("name")));
            });
            result.append("\n");
            
            int totalUpdated = 0;
            
            // Convert ROLE_ADMIN -> ADMIN
            Integer updated1 = jdbcTemplate.update("UPDATE roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN'");
            int safe1 = updated1 != null ? updated1 : 0;
            result.append("ROLE_ADMIN → ADMIN: ").append(safe1).append(" rows updated\n");
            totalUpdated += safe1;
            
            // Convert ROLE_MANAGER -> MANAGER  
            Integer updated2 = jdbcTemplate.update("UPDATE roles SET name = 'MANAGER' WHERE name = 'ROLE_MANAGER'");
            int safe2 = updated2 != null ? updated2 : 0;
            result.append("ROLE_MANAGER → MANAGER: ").append(safe2).append(" rows updated\n");
            totalUpdated += safe2;
            
            // Convert ROLE_USER -> USER
            Integer updated3 = jdbcTemplate.update("UPDATE roles SET name = 'USER' WHERE name = 'ROLE_USER'");
            int safe3 = updated3 != null ? updated3 : 0;
            result.append("ROLE_USER → USER: ").append(safe3).append(" rows updated\n");
            totalUpdated += safe3;
            
            // Fix workflow step roles if they exist
            try {
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'ADMIN' WHERE role_name = 'ROLE_ADMIN'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'MANAGER' WHERE role_name = 'ROLE_MANAGER'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'USER' WHERE role_name = 'ROLE_USER'");
                result.append("✅ Workflow step roles updated\n");
            } catch (Exception e) {
                result.append("ℹ️ Workflow step roles table not found (OK)\n");
            }
            
            result.append("\n📋 AFTER - Final roles:\n");
            jdbcTemplate.query("SELECT id, name FROM roles ORDER BY id", rs -> {
                result.append(String.format("  ID: %d, Name: '%s'\n", 
                    rs.getLong("id"), rs.getString("name")));
            });
            
            result.append("\n🎉 SUCCESS! Total role records updated: ").append(totalUpdated);
            result.append("\n\n✨ Your ERole enum errors should now be resolved!");
            
            return ResponseEntity.ok(result.toString());
            
        } catch (Exception e) {
            result.append("❌ ERROR: ").append(e.getMessage());
            result.append("\nStack trace: ");
            for (StackTraceElement element : e.getStackTrace()) {
                result.append("\n  ").append(element.toString());
            }
            return ResponseEntity.status(500).body(result.toString());
        }
    }
}
