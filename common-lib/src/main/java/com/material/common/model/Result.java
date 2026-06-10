package com.material.common.model;

import com.material.common.enums.ErrorCode;

public record Result<T>(int code, String message, T data) {
    /**
     * 作用：创建统一的成功响应。
     * 输入：
     * - data：Data，类型是 T；方法会读取这个值继续处理。
     * 输出：返回 <T> Result<T>，也就是这个方法处理后的结果。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 作用：创建统一的成功响应。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Result<Void>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    public static Result<Void> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 作用：创建统一的错误响应。
     * 输入：
     * - errorCode：错误码，类型是 ErrorCode；方法会读取这个值继续处理。
     * 输出：返回 Result<Void>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    public static Result<Void> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 作用：创建统一的错误响应。
     * 输入：
     * - code：编码，类型是 int；方法会读取这个值继续处理。
     * - message：消息内容，通常来自 RabbitMQ 或错误提示。
     * 输出：返回 Result<Void>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    public static Result<Void> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
