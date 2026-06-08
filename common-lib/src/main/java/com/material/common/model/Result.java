package com.material.common.model;

import com.material.common.enums.ErrorCode;

public record Result<T>(int code, String message, T data) {
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static Result<Void> success() {
        return new Result<>(200, "success", null);
    }

    public static Result<Void> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static Result<Void> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
