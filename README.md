# 应急物流运力调度与订单协同平台

面向自然灾害、园区应急和企业集中采购场景的物流协同平台。系统参考 Fleetbase 的订单运营模型，围绕采购方、供应商、司机三类角色，提供登录鉴权、供应商限量物资发布、采购方高并发抢购、待分配运输订单池、司机高并发抢单、发货地/目的地经纬度定位、智能调度推荐、订单追踪、三方履约评价和采购方/供应商/司机履约排行榜等能力。

## 技术栈

- Java 21, Spring Boot 3.5
- Spring Cloud Gateway, Nacos
- OpenResty / Nginx Lua
- MyBatis-Plus, MySQL
- Redis, Redisson
- RabbitMQ
- 原生 HTML/CSS/JavaScript 前端

## 模块说明

- `common-lib`：公共响应体、常量、用户类型、异常模型。
- `gateway-service`：统一网关入口，校验 Redis Token，并向下游服务透传用户身份请求头。
- `auth-service`：登录、三角色业务接口、Redis 缓存、MQ 消费、抢购、评价、推送等核心逻辑。
- `web-frontend`：三角色工作台页面，按用户类型展示不同业务功能。
- `infra/openresty`：OpenResty 入口层配置，使用令牌桶做第一层限流。
- `sql/init`：数据库建表和演示数据。

## 核心能力

- Redis Token 登录：Token 存 Redis Hash，实现多端登录和分布式登录态。
- 三层限流：OpenResty 入口令牌桶、Gateway RedisRateLimiter 令牌桶、业务 Redis Lua 原子扣减。
- 三角色注册：按用户类型写入独立账号表和资料表，密码加密存储，注册成功自动登录。
- Cache Aside：供应商目录读缓存，供应商物资写操作删除缓存并做延迟双删。
- 采购版美团工作台：供应商店铺详情、物资菜单、搜索筛选、采购清单/询价单批量提交。
- 订单闭环：供应商确认/拒单、库存扣减、司机接单、运输中、已完成、时间线追踪。
- 高并发抢购：Redis + Lua 原子扣减名额，RabbitMQ 异步落库，Redisson 锁兜底。
- 高并发运力抢单：司机抢运输单先由 Redis Lua 原子预占，再通过 RabbitMQ 异步绑定司机，避免重复接单。
- 订单推拉结合：订单创建后按关注关系推送给司机；司机也可在待分配运输订单池主动拉取。
- 物流位置建模：订单记录发货地、目的地和经纬度，司机可上传到达节点，运输追踪接口聚合起终点、司机节点和状态时间线。
- 智能调度推荐：待司机接单订单按司机在线状态、距发货地距离和司机评分生成可解释推荐榜。
- 消息通知中心：按采购方、供应商、司机角色聚合订单、推送和 MQ 异常提醒。
- Redis 数据结构：ZSet 做高频供应商履约榜，订单评价沉淀采购方/供应商/司机三方榜，GEO 查询附近供应商并缓存司机/订单最新位置，BitMap 记录司机出勤。
- RabbitMQ 可靠性：订单创建/抢购消息异步处理，死信队列统计，推送补偿接口兜底。
- 三方评价：采购方、供应商、司机围绕订单做履约评价，三类角色首页都能看到采购方、供应商、司机三张履约榜。

## 本地启动

更完整的启动、验证和排障步骤见：[docs/startup.md](docs/startup.md)。

Windows / WSL2 迁移启动步骤见：[docs/windows-setup.md](docs/windows-setup.md)。

### 新电脑推荐启动流程

Docker Compose 会启动 MySQL、Redis Cluster、RabbitMQ、Nacos 这些中间件；`auth-service`、`gateway-service` 和前端仍在本机通过 JDK 21、Maven、Python 启动。

```bash
git clone https://github.com/2249619829/MaterialCoordination.git
cd MaterialCoordination

docker compose up -d
docker compose ps

export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1

scripts/start-local.sh
scripts/smoke-test.sh
```

如果 `scripts/start-local.sh` 提示 Java 路径不存在，先确认本机 `java -version` 是 21，再按本机 JDK 安装位置调整 `use-java21.sh`，或参考 [docs/startup.md](docs/startup.md) 手动启动两个后端服务。

如果本机 MySQL、Redis Cluster、RabbitMQ、Nacos 已经启动，推荐直接使用脚本启动应用：

```bash
cd MaterialCoordination
scripts/start-local.sh
scripts/start-openresty.sh
scripts/smoke-test.sh
```

停止应用：

```bash
scripts/stop-local.sh
scripts/stop-openresty.sh
```

如果运行环境会自动回收后台进程，可以用前台保活模式：

```bash
scripts/start-local.sh --keep-alive
```

### 1. 启动中间件

本项目支持本地中间件或 Docker Compose。当前开发环境使用本地 MySQL、Redis Cluster、RabbitMQ、Nacos。
配置项支持环境变量覆盖，首次运行可参考 `.env.example`。

使用 Docker Compose 启动中间件时：

```bash
docker compose up -d
export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1
```

注意：Docker Compose 默认 MySQL root 密码是 `root`，而后端默认 `MYSQL_PASSWORD` 为空；如果不设置 `MYSQL_PASSWORD=root`，`auth-service` 会连接 MySQL 失败。

常用端口：

- MySQL：`3306`
- Redis Cluster：`6379-6384`（默认 `REDIS_CLUSTER_NODES=localhost:6379,...,localhost:6384`）
- RabbitMQ：`5672`
- RabbitMQ Management：`15672`
- Nacos：`8848`
- Gateway：`8080`
- Auth Service：`8081`
- Frontend：`5173`
- OpenResty：`8088`

### 2. 初始化数据库

```bash
cd MaterialCoordination
mysql -uroot material_coordination < sql/init/01_schema.sql
mysql -uroot material_coordination < sql/init/02_seed.sql
mysql -uroot material_coordination < sql/init/03_order_timeline.sql
```

如果是已有旧库升级，再执行：

```bash
mysql -uroot material_coordination < sql/migrations/20260610_logistics_places_and_claim_indexes.sql
```

### 3. 启动后端

```bash
cd MaterialCoordination
source use-java21.sh
mvn -q -pl auth-service spring-boot:run
```

网关服务如果未启动：

```bash
source use-java21.sh
mvn -q -pl gateway-service spring-boot:run
```

本地开发默认把 Nacos 服务实例注册为 `127.0.0.1`，避免 Mac 网络变化后网关拿到失效内网 IP。需要改成真实局域网 IP 时，可以设置：

```bash
export NACOS_DISCOVERY_IP=192.168.x.x
```

如果 RabbitMQ 使用 Docker Compose 默认配置，账号密码为 `.env.example` 中的 `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD`。

### 4. 启动前端

```bash
cd MaterialCoordination/web-frontend
python3 -m http.server 5173
```

访问：`http://localhost:5173`

如果要通过 OpenResty 入口体验 Nginx Lua 令牌桶限流：

```bash
cd MaterialCoordination
scripts/start-openresty.sh
```

访问：`http://127.0.0.1:8088`

## 演示账号

密码均为 `123456`。

- 供应商：`supplier01`
- 采购方：`purchaser01`
- 司机：`driver01`

## 关键接口

- 登录：`POST /auth/login`
- 注册：`POST /auth/register`
- 当前用户：`GET /auth/me`
- 供应商目录：`GET /api/suppliers/catalog`
- 供应商店铺详情：`GET /api/suppliers/{supplierId}/store`
- 供应商物资管理：`GET/POST/PUT /api/supplier/materials`
- 采购下单：`POST /api/purchase-orders`
- 采购清单批量提交：`POST /api/purchase-orders/cart/checkout`
- 订单时间线：`GET /api/orders/{orderId}/timeline`
- 供应商确认供货：`POST /api/supplier/orders/{orderId}/confirm`
- 供应商拒单：`POST /api/supplier/orders/{orderId}/reject`
- 采购方抢购：`POST /api/purchase-orders/{orderId}/panic-buy`
- 消息通知中心：`GET /api/notifications`
- 司机推送订单：`GET /api/transport-orders/push`
- 司机我的运输单：`GET /api/transport-orders/mine`
- 推送已读：`POST /api/transport-orders/push/{orderId}/read`
- 司机抢运输单：`POST /api/transport-orders/{orderId}/claim`
- 司机开始运输：`POST /api/transport-orders/{orderId}/start`
- 司机上传到达节点：`POST /api/transport-orders/{orderId}/location`
- 司机完成运输：`POST /api/transport-orders/{orderId}/complete`
- 运输追踪：`GET /api/transport-orders/{orderId}/tracking`
- 智能调度推荐：`GET /api/orders/{orderId}/dispatch-recommendations`
- 订单评价：`POST /api/orders/{orderId}/reviews`
- 供应商排行榜：`GET /api/suppliers/ranking`
- 三方履约排行榜：`GET /api/rankings/fulfillment`
- 附近供应商：`GET /api/suppliers/nearby`
- 司机出勤：`POST /api/drivers/attendance`
- 死信队列统计：`GET /api/mq/dead-letters`

## 架构文档

详细架构、流程图和面试讲解话术见：[docs/architecture.md](docs/architecture.md)。

代码结构、接口覆盖和功能完成状态见：[docs/project-code-guide.md](docs/project-code-guide.md)。

接口清单见：[docs/api.md](docs/api.md)。

JMeter 压测报告见：[docs/performance.md](docs/performance.md)。

项目演示路线和面试讲解稿见：[docs/demo-guide.md](docs/demo-guide.md)。

项目阶段拆分和迭代记录见：[docs/iteration-history.md](docs/iteration-history.md)。
