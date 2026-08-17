package com.weddinggames.backend.common;

import com.weddinggames.backend.common.dto.ApiErrorResponse;
import com.weddinggames.backend.common.dto.ApiErrorResponse.ValidationErrorDetail;
import com.weddinggames.backend.common.exception.ApiException;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.ConflictException;
import com.weddinggames.backend.common.exception.InvalidInvitationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidInvitationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInvitation(
            InvalidInvitationException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Acces refuse.", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Identifiants invalides.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationErrorDetail(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() == null ? "Valeur invalide." : fieldError.getDefaultMessage()))
                .toList();
        ApiErrorResponse body = ApiErrorResponse.ofValidation(
                "VALIDATION_ERROR",
                "La requete contient des champs invalides.",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleGenericApiException(ApiException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Une erreur inattendue est survenue.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(code, message, status.value(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
