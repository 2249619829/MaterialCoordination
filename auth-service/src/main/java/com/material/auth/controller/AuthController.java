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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @DeleteMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = AuthConstants.AUTHORIZATION, required = false) String authorization) {
        authService.logout(extractBearerToken(authorization));
        return Result.success();
    }

    @GetMapping("/me")
    public Result<LoginUserDTO> currentUser(@RequestHeader(value = AuthConstants.AUTHORIZATION, required = false) String authorization) {
        return Result.success(authService.currentUser(extractBearerToken(authorization)));
    }

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
