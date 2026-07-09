package com.material.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitConfigTest {
    /**
     * 作用：测试网关配置文件里已经给公开登录接口、核心抢购接口和普通业务接口配置限流。
     * 输入：
     * - 无输入参数；测试内部会读取 gateway-service 的 application.yml。
     * 输出：无返回值。断言通过表示配置文件中能找到预期的 RequestRateLimiter 设置。
     */
    @Test
    void applicationYamlConfiguresRequestRateLimiterForImportantRoutes() {
        Properties properties = loadGatewayProperties();

        assertThat(properties.getProperty("spring.data.redis.cluster.nodes"))
                .isEqualTo("${REDIS_CLUSTER_NODES:localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384}");
        assertThat(properties.getProperty("spring.data.redis.cluster.max-redirects"))
                .isEqualTo("${REDIS_CLUSTER_MAX_REDIRECTS:3}");

        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[0].id"))
                .isEqualTo("auth-public");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[0].filters[0].name"))
                .isEqualTo("RequestRateLimiter");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[0].filters[0].args.redis-rate-limiter.replenishRate"))
                .isEqualTo("${RATE_LIMIT_AUTH_REPLENISH_RATE:1}");

        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[1].id"))
                .isEqualTo("business-sensitive");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[1].filters[0].name"))
                .isEqualTo("RequestRateLimiter");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[1].filters[0].args.redis-rate-limiter.burstCapacity"))
                .isEqualTo("${RATE_LIMIT_SENSITIVE_BURST_CAPACITY:2}");

        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[3].id"))
                .isEqualTo("business-demo-service");
        assertThat(properties.getProperty("spring.cloud.gateway.server.webflux.routes[3].filters[0].args.redis-rate-limiter.replenishRate"))
                .isEqualTo("${RATE_LIMIT_API_REPLENISH_RATE:20}");
    }

    /**
     * 作用：读取 application.yml，并把 YAML 配置转换成方便断言的 Properties。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Properties，里面是一组配置键和值。
     */
    private static Properties loadGatewayProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        return factory.getObject();
    }
}
