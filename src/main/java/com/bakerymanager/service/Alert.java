package com.bakerymanager.service;

import java.time.LocalDateTime;

public class Alert {

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    private final LocalDateTime timestamp;
    private final String type;
    private final String message;
    private final Severity severity;

    public Alert(LocalDateTime timestamp, String type, String message, Severity severity) {
        this.timestamp = timestamp;
        this.type = type;
        this.message = message;
        this.severity = severity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }
}
