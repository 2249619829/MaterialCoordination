package com.material.auth.handler;

import com.material.common.enums.ErrorCode;
import com.material.common.exception.BusinessException;
import com.material.common.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    /**
     * 作用：把业务异常转换成统一的接口响应。
     * 输入：
     * - exception：异常对象，类型是 BusinessException；方法会读取这个值继续处理。
     * 输出：返回 ResponseEntity<Result<Void>>，也就是这个方法处理后的结果。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, ACCOUNT_DISABLED -> HttpStatus.FORBIDDEN;
            case BAD_REQUEST, LOGIN_FAILED, ACCOUNT_NOT_FOUND, PASSWORD_INCORRECT -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(Result.error(exception.getErrorCode()));
    }

    /**
     * 作用：完成 handleBadRequest 这一步处理。
     * 输入：
     * - exception：异常对象，类型是 RuntimeException；方法会读取这个值继续处理。
     * 输出：返回 ResponseEntity<Result<Void>>，也就是这个方法处理后的结果。
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Result<Void>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, exception.getMessage()));
    }

    /**
     * 作用：完成 handleDuplicateKey 这一步处理。
     * 输入：
     * - exception：异常对象，类型是 DuplicateKeyException；方法会读取这个值继续处理。
     * 输出：返回 ResponseEntity<Result<Void>>，也就是这个方法处理后的结果。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(400, "数据已存在，请检查用户名或唯一字段"));
    }

    /**
     * 作用：把未预料到的异常转换成统一的接口响应。
     * 输入：
     * - exception：异常对象，类型是 Exception；方法会读取这个值继续处理。
     * 输出：返回 ResponseEntity<Result<Void>>，也就是这个方法处理后的结果。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        log.error("Unhandled auth-service exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "internal server error"));
    }
}
