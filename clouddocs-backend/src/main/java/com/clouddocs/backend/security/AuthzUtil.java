package com.clouddocs.backend.security;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.WorkflowInstance;
import com.clouddocs.backend.entity.WorkflowTask;
import com.clouddocs.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("authzUtil")
public class AuthzUtil {

    @Autowired
    private UserRepository userRepository;

    public User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean isCurrentUser(User target) {
        User me = currentUser();
        if (me == null || target == null) return false;
        return me.getId() != null && me.getId().equals(target.getId());
    }

    public boolean isInitiator(WorkflowInstance instance) {
        if (instance == null) return false;
        return isCurrentUser(instance.getInitiatedBy());
    }

    public boolean isAssignee(WorkflowTask task) {
        if (task == null) return false;
        return isCurrentUser(task.getAssignedTo());
    }

    public boolean hasAnyRole(String... roles) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var authorities = auth.getAuthorities();
        if (authorities == null) return false;
        for (var g : authorities) {
            String name = g.getAuthority();
            for (String r : roles) {
                if (name.equalsIgnoreCase(r) || name.equalsIgnoreCase("ROLE_" + r)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInitiatorOrManager(WorkflowInstance instance) {
        return isInitiator(instance) || hasAnyRole("MANAGER", "ADMIN");
    }

    public boolean isInitiatorOrAssigneeOrManager(WorkflowInstance instance, WorkflowTask task) {
        return isInitiator(instance) || isAssignee(task) || hasAnyRole("MANAGER", "ADMIN");
    }

    public boolean isManagerOrAdmin() {
        return hasAnyRole("MANAGER", "ADMIN");
    }
}
