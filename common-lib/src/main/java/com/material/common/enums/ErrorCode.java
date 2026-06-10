package com.material.common.enums;

public enum ErrorCode {
    /**
     * 作用：完成 BAD_REQUEST 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    BAD_REQUEST(400, "bad request"),
    /**
     * 用户未认证或登录态无效。
     */
    UNAUTHORIZED(401, "unauthorized"),
    /**
     * 用户无权访问当前资源。
     */
    FORBIDDEN(403, "forbidden"),
    /**
     * 用户名或密码错误。
     */
    LOGIN_FAILED(1001, "用户名或密码错误"),
    /**
     * 用户不存在。
     */
    ACCOUNT_NOT_FOUND(1004, "用户不存在，请检查用户名和用户类型"),
    /**
     * 密码不正确。
     */
    PASSWORD_INCORRECT(1005, "密码错误，请重新输入"),
    /**
     * 账号已被禁用。
     */
    ACCOUNT_DISABLED(1002, "账号已被禁用，请联系平台管理员"),
    /**
     * 登录态写入失败。
     */
    TOKEN_WRITE_FAILED(1003, "login state write failed");

    private final int code;
    private final String message;

    /**
     * 作用：创建 ErrorCode 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - code：编码，类型是 int；方法会读取这个值继续处理。
     * - message：消息内容，通常来自 RabbitMQ 或错误提示。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 作用：读取枚举或错误码中的编码。
     * 输入：
     * - 无输入参数。
     * 输出：返回 int，表示当前对象里这个字段保存的值。
     */
    public int getCode() {
        return code;
    }

    /**
     * 作用：读取枚举或错误码中的提示信息。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getMessage() {
        return message;
    }
}
