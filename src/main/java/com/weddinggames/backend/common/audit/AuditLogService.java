package com.weddinggames.backend.common.audit;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final StaffAccountRepository staffAccountRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, StaffAccountRepository staffAccountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    /**
     * Records a sensitive action. {@code staffAccountId} is {@code null} only for actions with no
     * real authenticated actor (e.g. dev/test fixture seeding at startup) - those are silently
     * skipped rather than logged, since there is no genuine admin to attribute them to.
     */
    @Transactional
    public AuditLogEntry record(UUID staffAccountId, AuditAction action, UUID eventId, UUID entityId, String details) {
        if (staffAccountId == null) {
            return null;
        }
        StaffAccount staff = staffAccountRepository
                .findById(staffAccountId)
                .orElseThrow(() -> new NotFoundException("Compte staff introuvable."));
        AuditLogEntry entry =
                new AuditLogEntry(staffAccountId, staff.getDisplayName(), action, eventId, entityId, details);
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> listByEvent(UUID eventId) {
        return auditLogRepository.findByEventIdOrderByCreatedAtDesc(eventId);
    }
}
