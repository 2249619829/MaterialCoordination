package com.material.auth.config;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedissonConfigTest {
    @Test
    void clusterConfigUsesRedisClusterNodesAndNormalizesAddresses() {
        Config config = new RedissonConfig().redissonConfig(
                List.of("localhost:6379", "redis://localhost:6380"),
                "",
                "localhost",
                6379
        );

        assertThat(config.isClusterConfig()).isTrue();
        assertThat(config.useClusterServers().getNodeAddresses())
                .containsExactly("redis://localhost:6379", "redis://localhost:6380");
        assertThat(config.useClusterServers().getPassword()).isNull();
    }

    @Test
    void singleServerFallbackIsUsedWhenClusterNodesAreEmpty() {
        Config config = new RedissonConfig().redissonConfig(List.<String>of(), "secret", "127.0.0.1", 6388);

        assertThat(config.isSingleConfig()).isTrue();
        assertThat(config.useSingleServer().getAddress()).isEqualTo("redis://127.0.0.1:6388");
        assertThat(config.useSingleServer().getPassword()).isEqualTo("secret");
    }
}
