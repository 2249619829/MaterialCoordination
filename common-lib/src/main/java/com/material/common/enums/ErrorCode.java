package com.material.common.enums;

public enum ErrorCode {
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    LOGIN_FAILED(1001, "username or password is incorrect"),
    ACCOUNT_DISABLED(1002, "account is disabled"),
    TOKEN_WRITE_FAILED(1003, "login state write failed");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
