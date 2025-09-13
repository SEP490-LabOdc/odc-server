package com.odc.common.exception;

import com.odc.common.constant.ApiConstants;
import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {
    private final String errorCode;

    public UnauthorizedException(String message) {
        super(message);
        errorCode = ApiConstants.AUTHORIZATION_ERROR;
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
