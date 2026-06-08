package com.material.auth.service;

import com.material.auth.dto.LoginRequest;
import com.material.auth.dto.LoginResponse;
import com.material.auth.dto.RegisterRequest;
import com.material.common.model.LoginUserDTO;

public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    void logout(String token);

    LoginUserDTO currentUser(String token);
}
