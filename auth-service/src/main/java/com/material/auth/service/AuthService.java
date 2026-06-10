package com.material.auth.service;

import com.material.auth.dto.LoginRequest;
import com.material.auth.dto.LoginResponse;
import com.material.auth.dto.RegisterRequest;
import com.material.common.model.LoginUserDTO;

public interface AuthService {
    /**
     * 作用：根据用户名、密码和用户类型完成登录，并生成登录结果。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    LoginResponse login(LoginRequest request);

    /**
     * 作用：根据注册信息创建账号和资料，并在注册成功后直接登录。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 作用：删除 Redis 中的登录 Token，让用户退出登录。
     * 输入：
     * - token：登录 Token，用来证明用户已经登录。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    void logout(String token);

    /**
     * 作用：根据登录 Token 或用户请求头读取当前登录用户。
     * 输入：
     * - token：登录 Token，用来证明用户已经登录。
     * 输出：返回 LoginUserDTO，这是跨层传递用的数据对象。
     */
    LoginUserDTO currentUser(String token);
}
