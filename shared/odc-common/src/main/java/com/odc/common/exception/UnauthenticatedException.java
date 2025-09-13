package com.odc.common.exception;

import com.odc.common.constant.ApiConstants;
import lombok.Getter;

@Getter
public class UnauthenticatedException extends RuntimeException {
    private final String errorCode;

    public UnauthenticatedException(String message) {
        super(message);
        this.errorCode = ApiConstants.AUTHENTICATION_ERROR;
    }

    public UnauthenticatedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
