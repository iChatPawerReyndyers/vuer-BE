package com.vuer.common.exception;

public class DeviceLimitExceededException extends RuntimeException {
    public DeviceLimitExceededException(String message) {
        super(message);
    }
}