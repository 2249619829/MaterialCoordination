package com.material.common.exception;

import com.material.common.enums.ErrorCode;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    /**
     * 作用：创建 BusinessException 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - errorCode：错误码，类型是 ErrorCode；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 作用：读取业务异常里的错误码。
     * 输入：
     * - 无输入参数。
     * 输出：返回 ErrorCode，表示当前对象里这个字段保存的值。
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
