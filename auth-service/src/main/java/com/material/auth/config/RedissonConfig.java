package com.material.auth.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    /**
     * 作用：创建一个连接 Redis 的 Redisson 客户端。
     * 输入：
     * - host：Redis 主机地址，类型是 String；方法会读取这个值继续处理。
     * - port：Redis 端口，类型是 int；方法会读取这个值继续处理。
     * 输出：返回 RedissonClient，也就是这个方法处理后的结果。
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.data.redis.host:localhost}") String host,
                                         @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }
}
