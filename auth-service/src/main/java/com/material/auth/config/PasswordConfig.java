package com.material.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {
    /**
     * 作用：创建一个用于加密和校验密码的工具对象。
     * 输入：
     * - 无输入参数。
     * 输出：返回 PasswordEncoder，也就是这个方法处理后的结果。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
