package com.weddinggames.backend.participant.dto;

import java.util.List;

public record ParticipantImportPreviewResponse(
        List<ParticipantImportRow> rows, int totalRows, int validCount, int duplicateCount, int rejectedCount) {

    public static ParticipantImportPreviewResponse from(List<ParticipantImportRow> rows) {
        int validCount = 0;
        int duplicateCount = 0;
        int rejectedCount = 0;
        for (ParticipantImportRow row : rows) {
            switch (row.status()) {
                case VALID -> validCount++;
                case DUPLICATE_IN_FILE, DUPLICATE_EXISTING -> duplicateCount++;
                case REJECTED -> rejectedCount++;
            }
        }
        return new ParticipantImportPreviewResponse(rows, rows.size(), validCount, duplicateCount, rejectedCount);
    }
}
