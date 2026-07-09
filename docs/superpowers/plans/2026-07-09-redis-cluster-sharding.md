# Redis Cluster Sharding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local Redis default to a 3-master/3-replica Redis Cluster and update the Java services so Redis Cluster routing works for Spring Data Redis, Gateway rate limiting, Redisson locks, and multi-key Lua scripts.

**Architecture:** Docker Compose will run six Redis nodes plus an idempotent cluster initializer. Spring Boot Redis configuration will use `spring.data.redis.cluster.nodes` by default in both services. Business Redis access stays on existing templates, while multi-key Lua keys gain `{orderId}` hash tags so each script runs within a single hash slot.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data Redis with Lettuce, Spring Cloud Gateway RedisRateLimiter, Redisson 3.32.0, Docker Compose, Redis 7.2, JUnit 5, Mockito.

## Global Constraints

- Local Docker Compose must default to 3 Redis masters and 3 Redis replicas.
- `auth-service` and `gateway-service` must default to `localhost:6379` through `localhost:6384` via `REDIS_CLUSTER_NODES`.
- Do not migrate old single-node Redis data.
- Do not change unrelated frontend or algorithm files already dirty in the worktree.
- Use hash tags for multi-key Lua keys: `panic:{<orderId>}:...` and `transport:claim:{<orderId>}:...`.
- Run at least `mvn -pl auth-service,gateway-service test` before completion.

---

## File Structure

- Modify `docker-compose.yml`: replace the single `redis` service with six Redis nodes and an idempotent `redis-cluster-init` service.
- Modify `auth-service/src/main/resources/application.yml`: switch default Redis connection to cluster nodes.
- Modify `gateway-service/src/main/resources/application.yml`: switch default Redis connection to cluster nodes.
- Modify `auth-service/src/main/java/com/material/auth/config/RedissonConfig.java`: build Redisson Cluster config from Spring Redis cluster properties, with single-server fallback only when cluster nodes are absent.
- Modify `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`: centralize panic-buy and transport-claim key builders with Redis Cluster hash tags.
- Create `auth-service/src/test/java/com/material/auth/config/RedissonConfigTest.java`: verify cluster address normalization and single fallback.
- Create `auth-service/src/test/java/com/material/auth/config/AuthRedisClusterConfigTest.java`: verify auth YAML cluster defaults.
- Modify `gateway-service/src/test/java/com/material/gateway/config/GatewayRateLimitConfigTest.java`: verify gateway YAML cluster defaults alongside existing limiter assertions.
- Modify `auth-service/src/test/java/com/material/auth/service/BusinessDemoServicePersistenceTest.java`: capture Lua key lists for panic-buy and transport-claim behavior.
- Modify `README.md`, `docs/startup.md`, and `docs/demo-guide.md`: document Redis Cluster ports and startup commands.

## Task 1: Add Failing Configuration Tests

**Files:**
- Create: `auth-service/src/test/java/com/material/auth/config/AuthRedisClusterConfigTest.java`
- Modify: `gateway-service/src/test/java/com/material/gateway/config/GatewayRateLimitConfigTest.java`

**Interfaces:**
- Consumes: Existing `application.yml` files.
- Produces: Tests asserting `spring.data.redis.cluster.nodes` has six localhost nodes and `max-redirects` defaults to `3`.

- [ ] **Step 1: Write failing auth YAML test**

Add `AuthRedisClusterConfigTest`:

```java
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
```

- [ ] **Step 2: Extend gateway YAML test**

In `GatewayRateLimitConfigTest.applicationYamlConfiguresRequestRateLimiterForImportantRoutes`, add:

```java
assertThat(properties.getProperty("spring.data.redis.cluster.nodes"))
        .isEqualTo("${REDIS_CLUSTER_NODES:localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384}");
assertThat(properties.getProperty("spring.data.redis.cluster.max-redirects"))
        .isEqualTo("${REDIS_CLUSTER_MAX_REDIRECTS:3}");
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```bash
mvn -pl auth-service,gateway-service -Dtest=AuthRedisClusterConfigTest,GatewayRateLimitConfigTest test
```

Expected: fails because `spring.data.redis.cluster.nodes` is not configured yet.

## Task 2: Implement Spring and Redisson Cluster Configuration

**Files:**
- Modify: `auth-service/src/main/resources/application.yml`
- Modify: `gateway-service/src/main/resources/application.yml`
- Modify: `auth-service/src/main/java/com/material/auth/config/RedissonConfig.java`
- Create: `auth-service/src/test/java/com/material/auth/config/RedissonConfigTest.java`

**Interfaces:**
- Consumes: Spring properties `spring.data.redis.cluster.nodes`, `spring.data.redis.password`, `spring.data.redis.host`, `spring.data.redis.port`.
- Produces: `RedissonConfig.redissonClient(...)` that uses cluster mode when nodes are configured.

- [ ] **Step 1: Write failing Redisson config tests**

Add `RedissonConfigTest` with reflection against Redisson `Config` internals exposed by a package-visible helper:

```java
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
        Config config = new RedissonConfig().redissonConfig(List.of(), "secret", "127.0.0.1", 6388);

        assertThat(config.isSingleConfig()).isTrue();
        assertThat(config.useSingleServer().getAddress()).isEqualTo("redis://127.0.0.1:6388");
        assertThat(config.useSingleServer().getPassword()).isEqualTo("secret");
    }
}
```

- [ ] **Step 2: Run Redisson test and verify RED**

Run:

```bash
mvn -pl auth-service -Dtest=RedissonConfigTest test
```

Expected: fails because `redissonConfig(...)` helper and cluster support do not exist.

- [ ] **Step 3: Change YAML configs**

Replace each service's single Redis block with:

```yaml
  data:
    redis:
      password: ${REDIS_PASSWORD:}
      cluster:
        nodes: ${REDIS_CLUSTER_NODES:localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384}
        max-redirects: ${REDIS_CLUSTER_MAX_REDIRECTS:3}
```

- [ ] **Step 4: Implement Redisson cluster config**

Change `RedissonConfig` to accept:

```java
@Value("${spring.data.redis.cluster.nodes:}") List<String> clusterNodes
@Value("${spring.data.redis.password:}") String password
@Value("${spring.data.redis.host:localhost}") String host
@Value("${spring.data.redis.port:6379}") int port
```

Add helper:

```java
Config redissonConfig(List<String> clusterNodes, String password, String host, int port)
```

Cluster mode filters blank nodes, normalizes `redis://`, and only calls `setPassword` when `password` has text.

- [ ] **Step 5: Run config tests and verify GREEN**

Run:

```bash
mvn -pl auth-service,gateway-service -Dtest=AuthRedisClusterConfigTest,GatewayRateLimitConfigTest,RedissonConfigTest test
```

Expected: pass.

## Task 3: Add Hash-Tagged Redis Keys for Lua Scripts

**Files:**
- Modify: `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`
- Modify: `auth-service/src/test/java/com/material/auth/service/BusinessDemoServicePersistenceTest.java`

**Interfaces:**
- Produces: `panicStockKey(String orderId)`, `panicBuyerKey(String orderId, Long purchaserId)`, `transportClaimStockKey(String orderId)`, `transportClaimDriverKey(String orderId, Long driverId)`.

- [ ] **Step 1: Write failing transport claim key assertion**

In `driverClaimReservesTransportOrderWithRedisAndPublishesMqBeforeDatabaseAssignment`, capture key list:

```java
@SuppressWarnings("unchecked")
ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
verify(redisTemplate).execute(any(), keysCaptor.capture(), eq("8"), eq(String.valueOf(Duration.ofHours(2).toSeconds())));
assertThat(keysCaptor.getValue()).containsExactly(
        "transport:claim:{PO-TRANSPORT-001}:stock",
        "transport:claim:{PO-TRANSPORT-001}:driver:8"
);
```

- [ ] **Step 2: Write failing panic-buy key test**

Add test:

```java
@Test
void panicBuyUsesHashTaggedRedisKeysForClusterLuaScript() {
    BusinessDemoService service = service();
    PurchaseOrder order = panicBuyingOrder();
    when(purchaseOrderMapper.selectById("PO-PANIC-001")).thenReturn(order);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
    when(redisTemplate.execute(any(), anyList(), any(), any())).thenReturn(0L);

    var view = service.panicBuyOrder(20L, "PO-PANIC-001");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(any(), keysCaptor.capture(), eq("20"), eq(String.valueOf(Duration.ofHours(2).toSeconds())));
    assertThat(keysCaptor.getValue()).containsExactly(
            "panic:{PO-PANIC-001}:stock",
            "panic:{PO-PANIC-001}:buyer:20"
    );
    assertThat(view.status()).isEqualTo("抢购处理中");
}
```

Add helper:

```java
private PurchaseOrder panicBuyingOrder() {
    PurchaseOrder order = purchaserClaimedOrder();
    order.setStatus("待抢购");
    order.setDriverId(null);
    return order;
}
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```bash
mvn -pl auth-service -Dtest=BusinessDemoServicePersistenceTest#driverClaimReservesTransportOrderWithRedisAndPublishesMqBeforeDatabaseAssignment,BusinessDemoServicePersistenceTest#panicBuyUsesHashTaggedRedisKeysForClusterLuaScript test
```

Expected: fails because current keys lack hash tags.

- [ ] **Step 4: Implement key builders and replace string concatenation**

In `BusinessDemoService`, replace direct prefix concatenation for panic and transport Lua keys with helper methods returning exactly:

```java
private String panicStockKey(String orderId) {
    return "panic:{" + orderId + "}:stock";
}

private String panicBuyerKey(String orderId, Long purchaserId) {
    return "panic:{" + orderId + "}:buyer:" + purchaserId;
}

private String transportClaimStockKey(String orderId) {
    return "transport:claim:{" + orderId + "}:stock";
}

private String transportClaimDriverKey(String orderId, Long driverId) {
    return "transport:claim:{" + orderId + "}:driver:" + driverId;
}
```

- [ ] **Step 5: Run key tests and verify GREEN**

Run:

```bash
mvn -pl auth-service -Dtest=BusinessDemoServicePersistenceTest#driverClaimReservesTransportOrderWithRedisAndPublishesMqBeforeDatabaseAssignment,BusinessDemoServicePersistenceTest#panicBuyUsesHashTaggedRedisKeysForClusterLuaScript test
```

Expected: pass.

## Task 4: Update Docker Compose and Documentation

**Files:**
- Modify: `docker-compose.yml`
- Modify: `README.md`
- Modify: `docs/startup.md`
- Modify: `docs/demo-guide.md`

**Interfaces:**
- Produces: default local Redis Cluster services `redis-node-1` through `redis-node-6` and `redis-cluster-init`.

- [ ] **Step 1: Replace single Redis service**

In `docker-compose.yml`, remove `redis` and add six Redis services with host ports `6379` to `6384`, bus ports `16379` to `16384`, `cluster-enabled yes`, and `cluster-announce-*` matching each host port.

- [ ] **Step 2: Add idempotent cluster initializer**

Add `redis-cluster-init` using `redis:7.2` and a shell command that:

```bash
until redis-cli -h host.docker.internal -p 6379 ping; do sleep 1; done
if redis-cli -h host.docker.internal -p 6379 cluster info | grep -q 'cluster_state:ok'; then exit 0; fi
yes yes | redis-cli --cluster create host.docker.internal:6379 ... host.docker.internal:6384 --cluster-replicas 1
```

- [ ] **Step 3: Update docs**

Change startup examples from `docker compose up -d mysql redis rabbitmq nacos` to include `redis-node-1` through `redis-node-6` and `redis-cluster-init`, or `docker compose up -d` for all dependencies. Update Redis port text to `6379-6384` and mention `REDIS_CLUSTER_NODES`.

- [ ] **Step 4: Verify compose syntax**

Run:

```bash
docker compose config
```

Expected: config renders successfully.

## Task 5: Full Verification

**Files:**
- All modified Redis Cluster files.

**Interfaces:**
- Produces: passing project tests for touched backend modules.

- [ ] **Step 1: Run focused tests**

Run:

```bash
mvn -pl auth-service,gateway-service -Dtest=AuthRedisClusterConfigTest,GatewayRateLimitConfigTest,RedissonConfigTest,BusinessDemoServicePersistenceTest test
```

Expected: pass.

- [ ] **Step 2: Run module tests**

Run:

```bash
mvn -pl auth-service,gateway-service test
```

Expected: pass.

- [ ] **Step 3: Review diff**

Run:

```bash
git diff -- docker-compose.yml auth-service/src/main/resources/application.yml gateway-service/src/main/resources/application.yml auth-service/src/main/java/com/material/auth/config/RedissonConfig.java auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java auth-service/src/test/java/com/material/auth/config/AuthRedisClusterConfigTest.java auth-service/src/test/java/com/material/auth/config/RedissonConfigTest.java auth-service/src/test/java/com/material/auth/service/BusinessDemoServicePersistenceTest.java gateway-service/src/test/java/com/material/gateway/config/GatewayRateLimitConfigTest.java README.md docs/startup.md docs/demo-guide.md
```

Expected: diff only includes Redis Cluster changes.
