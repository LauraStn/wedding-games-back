package com.weddinggames.backend.common.audit;

import com.weddinggames.backend.common.audit.dto.AuditLogEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/audit-log")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Journal d'audit", description = "Consultation des actions administratives sensibles (reserve a l'administrateur)")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Liste les actions sensibles journalisees pour cet evenement, les plus recentes d'abord")
    public List<AuditLogEntryResponse> list(@PathVariable UUID eventId) {
        return auditLogService.listByEvent(eventId).stream().map(AuditLogEntryResponse::from).toList();
    }
}
