package com.bakerymanager.service;

import com.bakerymanager.entity.AccessAuditLog;
import com.bakerymanager.entity.User;
import com.bakerymanager.repository.AccessAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccessAuditService {

    private final AccessAuditLogRepository accessAuditLogRepository;

    public AccessAuditService(AccessAuditLogRepository accessAuditLogRepository) {
        this.accessAuditLogRepository = accessAuditLogRepository;
    }

    public void log(User user, String action, String resource, String outcome, String details) {
        log(user, "AUTH", action, resource, outcome, details, null, null);
    }

    public void logBusinessEvent(User user,
                                 String action,
                                 String resource,
                                 String details) {
        log(user, "BUSINESS", action, resource, "SUCCESS", details, null, null);
    }

    public void logDataChange(User user,
                              String action,
                              String resource,
                              String beforeState,
                              String afterState,
                              String details) {
        log(user, "DATA_CHANGE", action, resource, "SUCCESS", details, beforeState, afterState);
    }

    public void log(User user,
                    String eventType,
                    String action,
                    String resource,
                    String outcome,
                    String details,
                    String beforeState,
                    String afterState) {
        AccessAuditLog log = new AccessAuditLog();
        if (user != null) {
            log.setUsername(user.getUsername());
            log.setFullName(user.getFullName());
            log.setRoleName(user.getRole() != null ? user.getRole().name() : null);
        }
        log.setEventType(eventType != null ? eventType : "AUTH");
        log.setAction(action);
        log.setResource(resource);
        log.setOutcome(outcome);
        log.setDetails(details);
        log.setBeforeState(beforeState);
        log.setAfterState(afterState);
        accessAuditLogRepository.save(log);
    }
}
