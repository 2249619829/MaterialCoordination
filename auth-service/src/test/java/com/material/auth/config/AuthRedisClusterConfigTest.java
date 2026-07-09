package com.material.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRedisClusterConfigTest {
    @Test
    void applicationYamlDefaultsToRedisClusterNodes() {
        Properties properties = loadAuthProperties();

        assertThat(properties.getProperty("spring.data.redis.cluster.nodes"))
                .isEqualTo("${REDIS_CLUSTER_NODES:localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384}");
        assertThat(properties.getProperty("spring.data.redis.cluster.max-redirects"))
                .isEqualTo("${REDIS_CLUSTER_MAX_REDIRECTS:3}");
    }

    private static Properties loadAuthProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        return factory.getObject();
    }
}
