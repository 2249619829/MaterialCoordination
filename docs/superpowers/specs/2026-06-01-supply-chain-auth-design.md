# 数字化供应链物资协同平台第一阶段设计

## 目标

第一阶段搭建微服务整体架构，并实现登录模块。系统采用 Spring Boot、Spring Cloud Gateway、MyBatis、Nacos、MySQL、Redis、RabbitMQ。中间件通过本地 Docker Compose 启动，业务服务在本地开发环境运行。

第一阶段只实现以下能力：

- 本地微服务工程骨架
- Nacos 服务注册发现
- Gateway 统一入口与登录态鉴权
- Auth Service 账号密码登录、登出、当前用户查询
- Redis Token 登录态存储，支持多端登录
- MySQL 核心账号表、资料表、物资表初始化

暂不实现供应商读写、采购商抢单、订单推送、MQ 异步削峰、排行榜、GEO 和 BitMap 业务。这些作为后续模块逐步实现。

## 技术栈

- Java 21
- Spring Boot 3.x
- Spring Cloud Gateway
- Spring Cloud Alibaba Nacos
- MyBatis Plus
- MySQL 8
- Redis 7
- RabbitMQ 3
- Docker Compose
- BCrypt 密码加密

Nacos 使用本地自建开源版本。Nacos 是 Apache-2.0 License 的开源项目，本地开发和自部署可免费使用；云厂商托管版属于另一个使用形态，不作为第一阶段依赖。

## 微服务架构

项目采用多服务结构：

```text
supply-chain-platform
├── common-lib
├── gateway-service
├── auth-service
├── supplier-service
├── procurement-service
├── order-service
└── push-service
```

第一阶段真正实现：

- `common-lib`
- `gateway-service`
- `auth-service`
- `docker-compose.yml`
- 数据库初始化 SQL

以下服务只在架构设计中预留，后续模块再实现：

- `supplier-service`
- `procurement-service`
- `order-service`
- `push-service`

## 本地中间件

使用 Docker Compose 启动：

```text
nacos
mysql
redis
rabbitmq
```

本地端口：

```text
gateway-service      8080
auth-service         8081
nacos                8848
mysql                3306
redis                6379
rabbitmq             5672
rabbitmq-management  15672
```

第一阶段配置策略：

- 每个服务保留本地 `application.yml`
- 服务注册到 Nacos
- Gateway 通过 Nacos 服务发现路由到 Auth Service
- 第一阶段不强制接入 Nacos Config
- RabbitMQ 先在 Docker Compose 中启动，业务队列后续订单模块再使用

## 登录鉴权方案

采用 Gateway 统一鉴权 + Auth Service 颁发 Redis Token。

登录链路：

```text
客户端
  -> gateway-service
  -> auth-service 校验账号密码
  -> auth-service 生成随机 token
  -> auth-service 写入 Redis Hash
  -> 返回 token
```

业务请求链路：

```text
客户端携带 Authorization: Bearer {token}
  -> gateway-service
  -> gateway-service 查询 Redis 登录态
  -> 登录态有效则刷新 TTL
  -> gateway-service 透传用户请求头
  -> 下游业务服务
```

下游服务不重复查询 Redis 登录态，只读取网关透传的请求头：

```text
X-User-Id
X-User-Type
X-Username
```

白名单：

```text
POST /auth/login
```

需要登录：

```text
DELETE /auth/logout
GET /auth/me
其他业务接口
```

## Redis 登录态设计

Redis Token 结构参考黑马点评的登录态方案，并适配微服务网关鉴权。

Key：

```text
login:token:{token}
```

类型：

```text
Hash
```

Fields：

```text
id          -> 用户 ID
userType    -> PURCHASER / SUPPLIER / DRIVER
username    -> 登录账号
displayName -> 展示名称
```

TTL：

```text
30 分钟滑动过期
```

多端登录：

- 每次登录生成独立 token
- 每个 token 对应一个独立 `login:token:{token}` key
- 同一用户可以同时拥有多个有效 token
- 第一阶段登出只删除当前 token
- 后续可增加用户 token 集合，用于批量踢下线和设备管理

## 账号与业务资料边界

账号表只负责认证身份。业务资料、物资、供应能力不写入账号表。

第一阶段核心表：

```text
purchaser_account
purchaser_profile
supplier_account
supplier_profile
driver_account
driver_profile
material
supplier_material
```

### 采购商账号表

```text
purchaser_account
- id
- username
- password_hash
- status
- create_time
- update_time
```

### 采购商资料表

```text
purchaser_profile
- id
- purchaser_id
- company_name
- contact_name
- contact_phone
- address
- create_time
- update_time
```

### 供应商账号表

```text
supplier_account
- id
- username
- password_hash
- status
- create_time
- update_time
```

### 供应商资料表

```text
supplier_profile
- id
- supplier_id
- company_name
- contact_name
- contact_phone
- license_no
- address
- longitude
- latitude
- rating_score
- create_time
- update_time
```

### 司机账号表

```text
driver_account
- id
- username
- password_hash
- status
- create_time
- update_time
```

### 司机资料表

```text
driver_profile
- id
- driver_id
- real_name
- contact_phone
- vehicle_no
- vehicle_type
- longitude
- latitude
- attendance_status
- rating_score
- create_time
- update_time
```

### 物资基础表

```text
material
- id
- material_code
- material_name
- category
- unit
- description
- status
- create_time
- update_time
```

### 供应商可供应物资表

```text
supplier_material
- id
- supplier_id
- material_id
- supply_price
- stock_quantity
- daily_capacity
- delivery_radius_km
- status
- create_time
- update_time
```

## 关键索引

```text
purchaser_account.username                     唯一索引
purchaser_profile.purchaser_id                 唯一索引
supplier_account.username                      唯一索引
supplier_profile.supplier_id                   唯一索引
driver_account.username                        唯一索引
driver_profile.driver_id                       唯一索引
material.material_code                         唯一索引
supplier_material.supplier_id                  普通索引
supplier_material.material_id                  普通索引
supplier_material(supplier_id, material_id)    唯一索引
```

## Auth Service 职责

Auth Service 负责：

- 账号密码登录
- 根据 `userType` 路由到对应账号表
- 使用 BCrypt 校验密码
- 查询对应资料表得到展示名
- 生成随机 opaque token
- 将登录态写入 Redis Hash
- 删除当前 token 完成登出
- 返回当前登录用户信息

统一登录接口：

```http
POST /auth/login
Content-Type: application/json

{
  "userType": "SUPPLIER",
  "username": "supplier01",
  "password": "123456"
}
```

登录返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "random-token",
    "userId": 1,
    "userType": "SUPPLIER",
    "username": "supplier01",
    "displayName": "示例供应商"
  }
}
```

登出接口：

```http
DELETE /auth/logout
Authorization: Bearer {token}
```

当前用户接口：

```http
GET /auth/me
Authorization: Bearer {token}
```

错误处理：

- 账号不存在、密码错误、账号禁用统一返回登录失败
- 不暴露具体失败原因，避免账号枚举
- Redis 写入失败时登录失败，不返回 token

## Gateway Service 职责

Gateway Service 负责：

- 统一入口
- Nacos 服务发现路由
- 登录白名单放行
- 读取 `Authorization` 请求头
- 校验 Bearer Token 格式
- 查询 Redis Hash 登录态
- 登录态存在时刷新 TTL
- 向下游服务透传用户上下文请求头
- 登录态不存在或过期时返回 401

第一阶段 Gateway 不做细粒度 RBAC。后续可在 Gateway 增加：

- 基于角色的路径权限
- 令牌桶限流
- 黑名单
- 防刷策略

## Common Lib 职责

Common Lib 提供跨服务共享能力：

- 统一响应结构 `Result`
- 统一错误码 `ErrorCode`
- 业务异常 `BusinessException`
- 用户类型枚举 `UserTypeEnum`
- 登录用户 DTO `LoginUserDTO`
- Redis Key 常量
- 请求头常量
- 状态枚举

Common Lib 不依赖具体业务服务，避免循环依赖。

## 后续模块演进

第一阶段完成后，按以下顺序继续：

1. 供应商读写模块
   - 供应商资料维护
   - 供应商物资维护
   - Cache Aside 查询供应商资料和物资

2. 高并发采购商抢单模块
   - Redis + Lua 原子库存扣减
   - 一企一单校验
   - Redisson 分布式锁

3. RabbitMQ 异步削峰
   - 抢单成功消息入队
   - 异步落库
   - 死信队列
   - 补偿任务

4. 订单推送平台
   - 采购方下单
   - 推模式按关注关系推送司机
   - 拉模式订单大厅主动拉取
   - 三方履约评价

5. Redis 数据结构扩展
   - ZSet 供应商履约排行榜
   - GEO 前置仓、供应商、司机位置
   - BitMap 司机出勤状态

6. 高可用与一致性增强
   - Redis Cluster
   - Gateway 令牌桶限流
   - 缓存空值
   - 布隆过滤器
   - TTL 随机化
   - 逻辑过期
   - 延迟双删
   - MQ 重试

## 验收标准

第一阶段完成时，应满足：

- `docker compose up -d` 能启动 Nacos、MySQL、Redis、RabbitMQ
- Auth Service 能注册到 Nacos
- Gateway Service 能注册到 Nacos
- 通过 Gateway 调用 `POST /auth/login` 能成功登录
- 登录成功后 Redis 中存在 `login:token:{token}` Hash
- 携带 token 调用 `GET /auth/me` 能返回当前用户
- Gateway 能对未登录请求返回 401
- Gateway 能刷新 token TTL
- `DELETE /auth/logout` 能删除当前 token
- 登出后再次访问受保护接口返回 401
