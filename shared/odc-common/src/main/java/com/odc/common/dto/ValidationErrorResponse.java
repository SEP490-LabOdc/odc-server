package com.odc.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private String message;
    private String errorCode;
    private Map<String, List<String>> fieldErrors;  // Multiple errors per field
    private List<String> globalErrors;              // Non-field specific errors
    private String requestId;                       // For tracking
}
