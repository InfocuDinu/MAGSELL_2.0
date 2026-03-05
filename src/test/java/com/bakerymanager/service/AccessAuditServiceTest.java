package com.bakerymanager.service;

import com.bakerymanager.entity.AccessAuditLog;
import com.bakerymanager.entity.User;
import com.bakerymanager.repository.AccessAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessAuditServiceTest {

    @Mock
    private AccessAuditLogRepository repository;

    @Test
    void shouldPersistDataChangeAuditWithBeforeAfterState() {
        AccessAuditService service = new AccessAuditService(repository);

        User user = new User();
        user.setUsername("admin");
        user.setFullName("Administrator");
        user.setRole(User.Role.ADMIN);

        service.logDataChange(
            user,
            "SETTINGS_SAVE",
            "config.properties",
            "vat=19",
            "vat=21",
            "TVA update"
        );

        ArgumentCaptor<AccessAuditLog> captor = ArgumentCaptor.forClass(AccessAuditLog.class);
        verify(repository).save(captor.capture());

        AccessAuditLog log = captor.getValue();
        assertEquals("DATA_CHANGE", log.getEventType());
        assertEquals("SETTINGS_SAVE", log.getAction());
        assertEquals("vat=19", log.getBeforeState());
        assertEquals("vat=21", log.getAfterState());
        assertEquals("admin", log.getUsername());
    }
}
