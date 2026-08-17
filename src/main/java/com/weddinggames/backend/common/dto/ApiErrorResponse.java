package com.weddinggames.backend.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        List<ValidationErrorDetail> details) {

    public static ApiErrorResponse of(String code, String message, int status, String path) {
        return new ApiErrorResponse(code, message, status, path, Instant.now(), List.of());
    }

    public static ApiErrorResponse ofValidation(
            String code, String message, int status, String path, List<ValidationErrorDetail> details) {
        return new ApiErrorResponse(code, message, status, path, Instant.now(), details);
    }

    public record ValidationErrorDetail(String field, String message) {}
}
