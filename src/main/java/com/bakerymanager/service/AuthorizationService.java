package com.bakerymanager.service;

import com.bakerymanager.entity.User;
import com.bakerymanager.exception.AuthorizationException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorizationService {

    private final UserService userService;
    private final AccessAuditService accessAuditService;

    public AuthorizationService(UserService userService,
                                AccessAuditService accessAuditService) {
        this.userService = userService;
        this.accessAuditService = accessAuditService;
    }

    public Optional<User> currentUser() {
        return userService.getCurrentUser();
    }

    public boolean hasAnyRole(User.Role... roles) {
        Optional<User> userOpt = userService.getCurrentUser();
        if (userOpt.isEmpty()) {
            return false;
        }
        User.Role role = userOpt.get().getRole();
        return Arrays.stream(roles).anyMatch(r -> r == role);
    }

    public boolean isOperatorRole(User.Role role) {
        return role == User.Role.OPERATOR || role == User.Role.CASHIER || role == User.Role.PRODUCTION;
    }

    public boolean hasOperatorAccess() {
        Optional<User> userOpt = userService.getCurrentUser();
        return userOpt.map(User::getRole).map(this::isOperatorRole).orElse(false)
            || hasAnyRole(User.Role.ADMIN, User.Role.MANAGER);
    }

    public void requireAnyRole(String action, String resource, User.Role... roles) {
        Optional<User> userOpt = userService.getCurrentUser();
        if (userOpt.isEmpty()) {
            accessAuditService.log(null, action, resource, "DENIED", "No authenticated user");
            throw new AuthorizationException("Acces neautorizat. Este necesară autentificarea.");
        }

        User user = userOpt.get();
        if (!hasAnyRole(roles)) {
            accessAuditService.log(user, action, resource, "DENIED",
                "Required roles=" + List.of(roles) + ", actual=" + user.getRole());
            throw new AuthorizationException("Nu aveți permisiunea necesară pentru această operațiune.");
        }

        accessAuditService.log(user, action, resource, "ALLOWED", "Role=" + user.getRole());
    }

    public void requireOperatorOrAbove(String action, String resource) {
        Optional<User> userOpt = userService.getCurrentUser();
        if (userOpt.isEmpty()) {
            accessAuditService.log(null, action, resource, "DENIED", "No authenticated user");
            throw new AuthorizationException("Acces neautorizat. Este necesară autentificarea.");
        }

        User user = userOpt.get();
        if (!hasOperatorAccess()) {
            accessAuditService.log(user, action, resource, "DENIED",
                "Required operator/manager/admin, actual=" + user.getRole());
            throw new AuthorizationException("Nu aveți permisiunea necesară pentru această operațiune.");
        }

        accessAuditService.log(user, action, resource, "ALLOWED", "Role=" + user.getRole());
    }
}
