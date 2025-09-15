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
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("🔍 Database Role Fix Status:\n\n");
            
            // Test database connection
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.append("✅ Database connection: OK\n\n");
            
            // Show current roles
            result.append("📋 Current roles:\n");
            jdbcTemplate.query("SELECT id, name FROM roles ORDER BY id", rs -> {
                result.append(String.format("  ID: %d, Name: '%s'\n", 
                    rs.getLong("id"), rs.getString("name")));
            });
            result.append("\n");
            
            // Apply fixes with safe handling
            int totalUpdated = 0;
            
            // Handle ROLE_MANAGER -> MANAGER
            try {
                Integer updated1 = jdbcTemplate.update(
                    "UPDATE roles SET name = 'MANAGER' WHERE name = 'ROLE_MANAGER'");
                int safe1 = updated1 != null ? updated1 : 0;
                result.append("ROLE_MANAGER → MANAGER: ").append(safe1).append(" rows\n");
                totalUpdated += safe1;
            } catch (Exception e) {
                // Check if MANAGER already exists
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE name = 'MANAGER'", Integer.class);
                if (count != null && count > 0) {
                    // Delete old ROLE_MANAGER entries
                    Integer deleted = jdbcTemplate.update("DELETE FROM roles WHERE name = 'ROLE_MANAGER'");
                    result.append("Deleted duplicate ROLE_MANAGER: ").append(deleted != null ? deleted : 0).append(" rows\n");
                }
            }
            
            // Handle ROLE_ADMIN -> ADMIN  
            try {
                Integer updated2 = jdbcTemplate.update(
                    "UPDATE roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN'");
                int safe2 = updated2 != null ? updated2 : 0;
                result.append("ROLE_ADMIN → ADMIN: ").append(safe2).append(" rows\n");
                totalUpdated += safe2;
            } catch (Exception e) {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE name = 'ADMIN'", Integer.class);
                if (count != null && count > 0) {
                    Integer deleted = jdbcTemplate.update("DELETE FROM roles WHERE name = 'ROLE_ADMIN'");
                    result.append("Deleted duplicate ROLE_ADMIN: ").append(deleted != null ? deleted : 0).append(" rows\n");
                }
            }
            
            // Handle ROLE_USER -> USER
            try {
                Integer updated3 = jdbcTemplate.update(
                    "UPDATE roles SET name = 'USER' WHERE name = 'ROLE_USER'");
                int safe3 = updated3 != null ? updated3 : 0;
                result.append("ROLE_USER → USER: ").append(safe3).append(" rows\n");
                totalUpdated += safe3;
            } catch (Exception e) {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE name = 'USER'", Integer.class);
                if (count != null && count > 0) {
                    Integer deleted = jdbcTemplate.update("DELETE FROM roles WHERE name = 'ROLE_USER'");
                    result.append("Deleted duplicate ROLE_USER: ").append(deleted != null ? deleted : 0).append(" rows\n");
                }
            }
            
            // Fix workflow step roles if they exist
            try {
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'MANAGER' WHERE role_name = 'ROLE_MANAGER'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'ADMIN' WHERE role_name = 'ROLE_ADMIN'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'USER' WHERE role_name = 'ROLE_USER'");
                result.append("✅ Workflow step roles updated\n");
            } catch (Exception e) {
                result.append("ℹ️ Workflow step roles table not found (OK)\n");
            }
            
            result.append("\n📋 Final roles:\n");
            jdbcTemplate.query("SELECT id, name FROM roles ORDER BY id", rs -> {
                result.append(String.format("  ID: %d, Name: '%s'\n", 
                    rs.getLong("id"), rs.getString("name")));
            });
            
            result.append("\n🎉 SUCCESS! Total updates: ").append(totalUpdated);
            
            return ResponseEntity.ok(result.toString());
            
        } catch (Exception e) {
            result.append("❌ ERROR: ").append(e.getMessage());
            return ResponseEntity.status(500).body(result.toString());
        }
    }
}
