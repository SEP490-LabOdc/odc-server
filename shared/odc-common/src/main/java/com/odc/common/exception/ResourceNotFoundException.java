package com.odc.common.exception;

import com.odc.common.constant.ApiConstants;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, ApiConstants.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),  ApiConstants.RESOURCE_NOT_FOUND);
    }
}
