package com.material.auth.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

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
    public RedissonClient redissonClient(@Value("${spring.data.redis.cluster.nodes:}") List<String> clusterNodes,
                                         @Value("${spring.data.redis.password:}") String password,
                                         @Value("${spring.data.redis.host:localhost}") String host,
                                         @Value("${spring.data.redis.port:6379}") int port) {
        return Redisson.create(redissonConfig(clusterNodes, password, host, port));
    }

    Config redissonConfig(List<String> clusterNodes, String password, String host, int port) {
        Config config = new Config();
        List<String> nodeAddresses = normalizedClusterNodes(clusterNodes);
        if (!nodeAddresses.isEmpty()) {
            ClusterServersConfig clusterConfig = config.useClusterServers()
                    .addNodeAddress(nodeAddresses.toArray(String[]::new));
            if (StringUtils.hasText(password)) {
                clusterConfig.setPassword(password);
            }
            return config;
        }

        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(normalizeRedisAddress(host + ":" + port));
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }
        return config;
    }

    private List<String> normalizedClusterNodes(List<String> clusterNodes) {
        if (clusterNodes == null) {
            return List.of();
        }
        return clusterNodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(this::normalizeRedisAddress)
                .toList();
    }

    private String normalizeRedisAddress(String address) {
        String trimmed = address.trim();
        if (trimmed.startsWith("redis://") || trimmed.startsWith("rediss://")) {
            return trimmed;
        }
        return "redis://" + trimmed;
    }
}
