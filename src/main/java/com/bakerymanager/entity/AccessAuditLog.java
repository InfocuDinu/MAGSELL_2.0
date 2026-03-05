package com.bakerymanager.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_audit_log")
public class AccessAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "role_name", length = 50)
    private String roleName;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType = "AUTH";

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "resource", length = 160)
    private String resource;

    @Column(name = "outcome", nullable = false, length = 30)
    private String outcome;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "before_state", length = 2000)
    private String beforeState;

    @Column(name = "after_state", length = 2000)
    private String afterState;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }

    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
