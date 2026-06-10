package com.material.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GatewayServiceApplication {
    /**
     * 作用：启动这个 Spring Boot 应用。
     * 输入：
     * - args：程序启动参数，通常由命令行传入。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
