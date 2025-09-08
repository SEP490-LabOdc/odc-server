package com.odc.common.exception;

import com.odc.common.constant.ApiConstants;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = ApiConstants.BUSINESS_ERROR;
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ApiConstants.BUSINESS_ERROR;
    }
}