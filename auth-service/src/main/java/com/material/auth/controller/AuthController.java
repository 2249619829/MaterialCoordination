package com.material.auth.controller;

import com.material.auth.dto.LoginRequest;
import com.material.auth.dto.LoginResponse;
import com.material.auth.dto.RegisterRequest;
import com.material.auth.service.AuthService;
import com.material.common.constant.AuthConstants;
import com.material.common.enums.ErrorCode;
import com.material.common.exception.BusinessException;
import com.material.common.model.LoginUserDTO;
import com.material.common.model.Result;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * 作用：创建 AuthController 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - authService：auth 业务服务对象，类型是 AuthService；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 作用：根据用户名、密码和用户类型完成登录，并生成登录结果。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<LoginResponse>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 作用：根据注册信息创建账号和资料，并在注册成功后直接登录。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<LoginResponse>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 作用：删除 Redis 中的登录 Token，让用户退出登录。
     * 输入：
     * - authorization：Authorization 请求头，通常长得像 Bearer 加 Token。
     * 输出：返回 Result<Void>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @DeleteMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = AuthConstants.AUTHORIZATION, required = false) String authorization) {
        authService.logout(extractBearerToken(authorization));
        return Result.success();
    }

    /**
     * 作用：根据登录 Token 或用户请求头读取当前登录用户。
     * 输入：
     * - authorization：Authorization 请求头，通常长得像 Bearer 加 Token。
     * 输出：返回 Result<LoginUserDTO>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/me")
    public Result<LoginUserDTO> currentUser(@RequestHeader(value = AuthConstants.AUTHORIZATION, required = false) String authorization) {
        return Result.success(authService.currentUser(extractBearerToken(authorization)));
    }

    /**
     * 作用：从 Authorization 请求头中取出真正的 Token 字符串。
     * 输入：
     * - authorization：Authorization 请求头，通常长得像 Bearer 加 Token。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(AuthConstants.BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String token = authorization.substring(AuthConstants.BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return token;
    }
}
