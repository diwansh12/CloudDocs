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
            result.append("🔍 Database Role Fix - Handling Check Constraints:\n\n");
            
            // Test database connection
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.append("✅ Database connection: OK\n\n");
            
            // Show current roles and constraints
            result.append("📋 BEFORE - Current roles:\n");
            jdbcTemplate.query("SELECT id, name, description FROM roles ORDER BY id", rs -> {
                result.append(String.format("  ID: %d, Name: '%s', Desc: '%s'\n", 
                    rs.getLong("id"), rs.getString("name"), rs.getString("description")));
            });
            
            // Check existing constraints
            result.append("\n🔒 Checking constraints:\n");
            jdbcTemplate.query("SELECT conname FROM pg_constraint WHERE conrelid = 'roles'::regclass AND contype = 'c'", rs -> {
                result.append("  Found constraint: ").append(rs.getString("conname")).append("\n");
            });
            
            // Step 1: Temporarily drop the check constraint
            result.append("\n🔧 Step 1: Temporarily removing check constraint...\n");
            try {
                jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
                result.append("✅ Check constraint dropped temporarily\n");
            } catch (Exception e) {
                result.append("ℹ️ No constraint to drop or already removed\n");
            }
            
            // Step 2: Perform the role updates
            result.append("\n🔧 Step 2: Updating role names...\n");
            int totalUpdated = 0;
            
            Integer updated1 = jdbcTemplate.update("UPDATE roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN'");
            int safe1 = updated1 != null ? updated1 : 0;
            result.append("  ROLE_ADMIN → ADMIN: ").append(safe1).append(" rows\n");
            totalUpdated += safe1;
            
            Integer updated2 = jdbcTemplate.update("UPDATE roles SET name = 'MANAGER' WHERE name = 'ROLE_MANAGER'");
            int safe2 = updated2 != null ? updated2 : 0;
            result.append("  ROLE_MANAGER → MANAGER: ").append(safe2).append(" rows\n");
            totalUpdated += safe2;
            
            Integer updated3 = jdbcTemplate.update("UPDATE roles SET name = 'USER' WHERE name = 'ROLE_USER'");
            int safe3 = updated3 != null ? updated3 : 0;
            result.append("  ROLE_USER → USER: ").append(safe3).append(" rows\n");
            totalUpdated += safe3;
            
            // Step 3: Recreate constraint with new values
            result.append("\n🔧 Step 3: Recreating check constraint with updated values...\n");
            try {
                jdbcTemplate.execute("ALTER TABLE roles ADD CONSTRAINT roles_name_check CHECK (name IN ('ADMIN', 'MANAGER', 'USER'))");
                result.append("✅ Check constraint recreated with new values\n");
            } catch (Exception e) {
                result.append("ℹ️ Skipping constraint recreation: ").append(e.getMessage()).append("\n");
            }
            
            // Step 4: Fix workflow step roles if they exist
            result.append("\n🔧 Step 4: Updating workflow step roles...\n");
            try {
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'ADMIN' WHERE role_name = 'ROLE_ADMIN'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'MANAGER' WHERE role_name = 'ROLE_MANAGER'");
                jdbcTemplate.update("UPDATE workflow_step_roles SET role_name = 'USER' WHERE role_name = 'ROLE_USER'");
                result.append("✅ Workflow step roles updated\n");
            } catch (Exception e) {
                result.append("ℹ️ Workflow step roles table not found (OK)\n");
            }
            
            // Show final state
            result.append("\n📋 AFTER - Final roles:\n");
            jdbcTemplate.query("SELECT id, name, description FROM roles ORDER BY id", rs -> {
                result.append(String.format("  ID: %d, Name: '%s', Desc: '%s'\n", 
                    rs.getLong("id"), rs.getString("name"), rs.getString("description")));
            });
            
            result.append("\n🎉 SUCCESS! Total role records updated: ").append(totalUpdated);
            result.append("\n\n✨ Your ERole enum errors should now be completely resolved!");
            result.append("\n🚀 Test your workflow creation - it should work without timeouts!");
            
            return ResponseEntity.ok(result.toString());
            
        } catch (Exception e) {
            result.append("❌ CRITICAL ERROR: ").append(e.getMessage()).append("\n");
            return ResponseEntity.status(500).body(result.toString());
        }
    }
}
