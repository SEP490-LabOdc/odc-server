package com.odc.common.exception;

import com.odc.common.constant.ApiConstants;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.ValidationErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex, WebRequest request) {
        log.error("Business exception: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Xử lý validation errors cho @RequestBody
     * Trả về detailed field errors cho Frontend
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        // Group field errors by field name (support multiple errors per field)
        Map<String, List<String>> fieldErrors = new HashMap<>();
        List<String> globalErrors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                String fieldName = fieldError.getField();
                String errorMessage = fieldError.getDefaultMessage();

                fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
            } else {
                // Global errors (class-level validations)
                globalErrors.add(error.getDefaultMessage());
            }
        });

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .message("Dữ liệu không hợp lệ")
                .errorCode(ApiConstants.VALIDATION_ERROR)
                .fieldErrors(fieldErrors)
                .globalErrors(globalErrors.isEmpty() ? null : globalErrors)
                .requestId(generateRequestId())
                .build();

        ApiResponse<ValidationErrorResponse> response = ApiResponse.<ValidationErrorResponse>builder()
                .success(false)
                .message("Validation failed")
                .data(errorResponse)
                .errorCode(ApiConstants.VALIDATION_ERROR)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Xử lý validation errors cho @RequestParam và @PathVariable
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, List<String>> fieldErrors = new HashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();

            fieldErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        }

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .message("Tham số không hợp lệ")
                .errorCode(ApiConstants.PARAMETER_VALIDATION_ERROR)
                .fieldErrors(fieldErrors)
                .requestId(generateRequestId())
                .build();

        ApiResponse<ValidationErrorResponse> response = ApiResponse.<ValidationErrorResponse>builder()
                .success(false)
                .message("Parameter validation failed")
                .data(errorResponse)
                .errorCode(ApiConstants.PARAMETER_VALIDATION_ERROR)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error("Internal server error", ApiConstants.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthenticatedException(
            UnauthenticatedException ex, WebRequest request) {
        log.warn("Unauthenticated: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response); // 403
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Unauthorized: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ApiConstants.AUTHORIZATION_ERROR
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response); // 403
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
