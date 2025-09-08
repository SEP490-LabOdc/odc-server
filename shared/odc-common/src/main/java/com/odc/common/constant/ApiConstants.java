package com.odc.common.constant;

public class ApiConstants {

    // API Versions
    public static final String API_V1 = "/api/v1";

    // Common Endpoints
    public static final String HEALTH_CHECK = "/health";
    public static final String METRICS = "/metrics";

    // Authentication
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    // Error Codes
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
    public static final String AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String PARAMETER_VALIDATION_ERROR = "PARAMETER_VALIDATION_ERROR";
}
