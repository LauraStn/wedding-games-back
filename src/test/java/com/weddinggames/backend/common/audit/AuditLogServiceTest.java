package com.weddinggames.backend.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffAccountRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for recording and listing audit entries. */
class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private StaffAccountRepository staffAccountRepository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        staffAccountRepository = mock(StaffAccountRepository.class);
        service = new AuditLogService(auditLogRepository, staffAccountRepository);
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordsAnEntrySnapshottingTheStaffDisplayName() {
        UUID staffAccountId = UUID.randomUUID();
        StaffAccount staff = mock(StaffAccount.class);
        when(staff.getDisplayName()).thenReturn("Laura");
        when(staffAccountRepository.findById(staffAccountId)).thenReturn(Optional.of(staff));
        UUID eventId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();

        AuditLogEntry entry =
                service.record(staffAccountId, AuditAction.PARTICIPANT_DELETED, eventId, entityId, "Jessika Dijoux");

        assertThat(entry.getStaffAccountId()).isEqualTo(staffAccountId);
        assertThat(entry.getStaffDisplayName()).isEqualTo("Laura");
        assertThat(entry.getAction()).isEqualTo(AuditAction.PARTICIPANT_DELETED);
        assertThat(entry.getEventId()).isEqualTo(eventId);
        assertThat(entry.getEntityId()).isEqualTo(entityId);
        assertThat(entry.getDetails()).isEqualTo("Jessika Dijoux");
    }

    @Test
    void skipsRecordingWhenThereIsNoRealActor() {
        AuditLogEntry entry = service.record(null, AuditAction.PARTICIPANT_DELETED, UUID.randomUUID(), null, null);

        assertThat(entry).isNull();
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void rejectsRecordingForAnUnknownStaffAccount() {
        UUID staffAccountId = UUID.randomUUID();
        when(staffAccountRepository.findById(staffAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(staffAccountId, AuditAction.PARTICIPANT_DELETED, null, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsEntriesForAnEvent() {
        UUID eventId = UUID.randomUUID();
        AuditLogEntry entry = mock(AuditLogEntry.class);
        when(auditLogRepository.findByEventIdOrderByCreatedAtDesc(eventId)).thenReturn(List.of(entry));

        List<AuditLogEntry> entries = service.listByEvent(eventId);

        assertThat(entries).containsExactly(entry);
    }
}
