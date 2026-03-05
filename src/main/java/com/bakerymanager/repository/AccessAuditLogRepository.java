package com.bakerymanager.repository;

import com.bakerymanager.entity.AccessAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessAuditLogRepository extends JpaRepository<AccessAuditLog, Long> {
}
