package com.clouddocs.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class DatabaseFixController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/fix-roles")
    public ResponseEntity<String> fixRoles() {
        try {
            // Fix the enum mismatch that's causing 500 errors
            int updated1 = jdbcTemplate.update("UPDATE roles SET name = 'MANAGER' WHERE name = 'ROLE_MANAGER'");
            int updated2 = jdbcTemplate.update("UPDATE roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN'");
            int updated3 = jdbcTemplate.update("UPDATE roles SET name = 'USER' WHERE name = 'ROLE_USER'");

            // Also fix workflow step roles if they exist
            jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'MANAGER' WHERE role_name = 'ROLE_MANAGER'");
            jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'ADMIN' WHERE role_name = 'ROLE_ADMIN'");
            jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'USER' WHERE role_name = 'ROLE_USER'");

            return ResponseEntity.ok("Database roles fixed! Updated: " + (updated1 + updated2 + updated3) + " role records");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
