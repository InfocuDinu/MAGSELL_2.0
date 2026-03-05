package com.bakerymanager.smartbill.production.api.dto;

import java.time.LocalDateTime;

public record SchedulerAlertDto(
    String severity,
    String code,
    String message,
    String resource,
    LocalDateTime at
) {
}
