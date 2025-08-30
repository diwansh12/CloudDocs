package com.clouddocs.backend.controller;

import com.clouddocs.backend.entity.TaskStatus;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowTask;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.WorkflowTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskController {

    @Autowired private WorkflowTaskRepository taskRepository;
    @Autowired private UserRepository userRepository;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyTasks(
            @RequestParam(defaultValue = "PENDING") TaskStatus status,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User currentUser = getCurrentUser();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<WorkflowTask> pageResult = taskRepository.findByAssignedToAndStatus(currentUser, status, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("tasks", pageResult.getContent());
        response.put("currentPage", pageResult.getNumber());
        response.put("pageSize", pageResult.getSize());
        response.put("totalItems", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("hasNext", pageResult.hasNext());
        response.put("hasPrevious", pageResult.hasPrevious());

        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
    }
}
