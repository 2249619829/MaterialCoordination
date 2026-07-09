# Redis Cluster Sharding Design

## Goal

将项目从单节点 Redis 改造为默认 3 主 3 从 Redis Cluster，本地 `docker-compose` 启动后即可提供分片能力。`auth-service`、`gateway-service`、Gateway RedisRateLimiter、Spring Data Redis、Redisson 分布式锁、业务 Lua/GEO/ZSet/Bitmap 等能力需要继续可用。

## Current State

当前项目使用单 Redis：

- `docker-compose.yml` 只有一个 `redis` 服务，暴露 `6379`。
- `auth-service/src/main/resources/application.yml` 使用 `spring.data.redis.host`、`port`、`password`。
- `gateway-service/src/main/resources/application.yml` 使用同样的单机 Redis 配置。
- `auth-service/src/main/java/com/material/auth/config/RedissonConfig.java` 固定 `useSingleServer()`。
- 业务代码大量通过 `StringRedisTemplate` 访问 Redis，Gateway 通过 `ReactiveStringRedisTemplate` 和 `RequestRateLimiter` 访问 Redis。
- `BusinessDemoService` 中采购抢购和运输抢单使用 Lua 同时操作两个 key。Redis Cluster 要求同一个脚本中的多个 key 必须落在同一个 hash slot，否则会触发 `CROSSSLOT`。

## Recommended Approach

采用 Spring Boot 原生 Redis Cluster 配置承接 Spring Data Redis 和 Gateway RedisRateLimiter，使用 Redisson Cluster 配置承接分布式锁。本地 Docker Compose 默认启动 6 个 Redis 节点，并由初始化容器创建 3 主 3 从集群。

这个方案保持应用层 Redis 访问 API 基本不变：业务仍注入 `StringRedisTemplate`，网关仍注入 `ReactiveStringRedisTemplate`，消费者仍注入 `RedissonClient`。需要修改的是连接配置、Redisson 客户端创建逻辑，以及多 key Lua 使用的 Redis key 命名。

## Redis Topology

`docker-compose.yml` 默认提供 6 个 Redis 节点：

- `redis-node-1`: container port `6379`, host port `6379`
- `redis-node-2`: container port `6379`, host port `6380`
- `redis-node-3`: container port `6379`, host port `6381`
- `redis-node-4`: container port `6379`, host port `6382`
- `redis-node-5`: container port `6379`, host port `6383`
- `redis-node-6`: container port `6379`, host port `6384`

每个节点启用：

- `cluster-enabled yes`
- `cluster-config-file nodes.conf`
- `cluster-node-timeout 5000`
- `appendonly yes`
- `protected-mode no`
- `cluster-announce-ip host.docker.internal`
- `cluster-announce-port <host redis port>`
- `cluster-announce-bus-port <host bus port>`

新增 `redis-cluster-init` 初始化服务，等待 6 个节点可达后执行：

```bash
redis-cli --cluster create \
  host.docker.internal:6379 host.docker.internal:6380 host.docker.internal:6381 \
  host.docker.internal:6382 host.docker.internal:6383 host.docker.internal:6384 \
  --cluster-replicas 1 --cluster-yes
```

本机服务通过 `localhost:6379` 到 `localhost:6384` 访问集群节点。Redis Cluster 的 MOVED/ASK 重定向地址由 `cluster-announce-*` 控制，节点应宣布宿主机可访问的端口，避免 Java 进程收到 Docker 内部 IP 或 Compose service name 后无法继续路由。

Compose 还需要暴露 Cluster bus 端口：

- `redis-node-1`: host bus port `16379`
- `redis-node-2`: host bus port `16380`
- `redis-node-3`: host bus port `16381`
- `redis-node-4`: host bus port `16382`
- `redis-node-5`: host bus port `16383`
- `redis-node-6`: host bus port `16384`

所有 Redis 节点和 `redis-cluster-init` 增加 `extra_hosts: ["host.docker.internal:host-gateway"]`，兼容 Docker Desktop 和支持 host-gateway 的 Linux Docker。项目当前运行在 macOS 时，Docker Desktop 会解析 `host.docker.internal`。

## Application Configuration

`auth-service` 和 `gateway-service` 的 Redis 配置改为 Cluster 默认：

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:}
      cluster:
        nodes: ${REDIS_CLUSTER_NODES:localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384}
        max-redirects: ${REDIS_CLUSTER_MAX_REDIRECTS:3}
```

`REDIS_HOST` 和 `REDIS_PORT` 不再作为默认主路径。文档和脚本应引导用户使用 `REDIS_CLUSTER_NODES`。如果后续确实需要兼容单机 Redis，可以另设 profile，但本次需求是本地默认 3 主 3 从 Cluster，因此主配置直接走 Cluster。

## Redisson Configuration

`RedissonConfig` 从 Spring 配置读取：

- `spring.data.redis.cluster.nodes`
- `spring.data.redis.password`

当 `cluster.nodes` 存在且非空时，创建：

```java
config.useClusterServers()
        .addNodeAddress("redis://localhost:6379", ...)
        .setPassword(passwordOrNull);
```

节点地址需要自动补齐 `redis://` 前缀。空密码不传给 Redisson，避免 Redisson 对无密码 Redis 发送认证。

由于本次默认是 Cluster，`useSingleServer()` 可只保留为测试或兼容兜底：当未配置 cluster nodes 时，才回退到 `spring.data.redis.host` 和 `spring.data.redis.port`。生产和本地默认配置都会走 Cluster。

## Redis Key Slot Design

单 key 访问不需要改名：

- 登录态 Hash：`login:token:<token>`
- 供应商目录缓存：`cache:supplier:catalog:v1`
- 供应商排行 ZSet：`ranking:supplier:fulfillment`
- 供应商 GEO：`geo:supplier`
- 司机位置 GEO：`driver:location:geo`
- 运输订单位置 GEO：`transport:order:location:geo`
- 司机出勤 Bitmap：`attendance:driver:<yyyyMMdd>`
- 待落库订单 Hash：`order:pending:<orderId>`

多 key Lua 必须改为同 slot。Redis Cluster 使用 `{...}` 中的内容作为 hash tag，因此同一订单的相关 key 需要共享 `{orderId}`。

采购抢购：

```text
panic:{<orderId>}:stock
panic:{<orderId>}:buyer:<purchaserId>
```

运输抢单：

```text
transport:claim:{<orderId>}:stock
transport:claim:{<orderId>}:driver:<driverId>
```

这保证 `PANIC_BUY_SCRIPT` 和 `TRANSPORT_CLAIM_SCRIPT` 的 `KEYS[1]`、`KEYS[2]` 在同一个 hash slot，Cluster 下 Lua 仍保持原子性。

## Data Flow

登录：

1. `auth-service` 写入 `login:token:<token>` Hash。
2. `gateway-service` 按相同 key 读取登录态并续期。
3. Spring Data Redis Cluster 根据 key slot 路由到对应主节点。

抢购：

1. `auth-service` 初始化 `panic:{orderId}:stock`。
2. Lua 同时读取库存 key 和采购方防重 key。
3. 成功后扣减库存、写入 `panic:{orderId}:buyer:<purchaserId>`，并发送 RabbitMQ。
4. 消费者仍使用 MySQL 条件更新兜底。

运输抢单：

1. `auth-service` 初始化 `transport:claim:{orderId}:stock`。
2. Lua 同时读取运力名额 key 和司机防重 key。
3. 成功后扣减名额、写入 `transport:claim:{orderId}:driver:<driverId>`，并发送 RabbitMQ。
4. 消费者通过 Redisson Cluster 锁和 MySQL 条件更新兜底。

## Error Handling

- Redis Cluster 未创建完成时，应用连接会失败；启动文档需要要求先执行 `docker compose up -d mysql redis-node-1 ... redis-node-6 redis-cluster-init rabbitmq nacos` 或直接启动全部依赖。
- `redis-cluster-init` 需要具备幂等性：如果集群已创建，再次启动不应破坏已有集群。初始化脚本可检查 `cluster_state:ok` 后直接退出。
- Redisson 空密码不设置 password；有密码时所有 Redis 节点必须使用同一个密码。
- 多 key Lua 使用 hash tag 后，新 key 与旧单机 key 名不兼容。开发环境可以清空 Redis 数据重建；文档需说明这是本地中间件拓扑升级，不做旧 Redis 数据迁移。

## Testing

新增或修改测试覆盖：

- `auth-service` 配置测试：`application.yml` 默认包含 6 个 `spring.data.redis.cluster.nodes`。
- `gateway-service` 配置测试：`application.yml` 默认包含 6 个 `spring.data.redis.cluster.nodes`，现有限流配置仍存在。
- `RedissonConfig` 单元测试：配置 cluster nodes 时创建的 Redisson config 使用 cluster servers，并补齐 `redis://` 地址；空密码不设置 password。
- `BusinessDemoService` 单元测试：采购抢购 Lua key 分别是 `panic:{orderId}:stock` 和 `panic:{orderId}:buyer:<purchaserId>`。
- `BusinessDemoService` 单元测试：运输抢单 Lua key 分别是 `transport:claim:{orderId}:stock` 和 `transport:claim:{orderId}:driver:<driverId>`。
- Maven 验证：至少运行 `mvn -pl auth-service,gateway-service test`。

## Out of Scope

- 不引入应用层自研分片。
- 不把 MySQL、RabbitMQ、Nacos 改造成集群。
- 不迁移旧单机 Redis 数据。
- 不重构业务服务的大型类结构；只修改 Redis Cluster 必需的 key 构造和配置。
