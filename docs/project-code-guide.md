# 项目代码介绍与接口核对文档

本文档按当前代码实现整理，用于后续对话、代码走读、面试讲解和功能核对。判断标准以源码为准，不以 README 或演示文档的表述为准。

## 重要结论

- 后端已经实现运输追踪接口：`GET /api/transport-orders/{orderId}/tracking`。
- 后端追踪响应包含订单状态、司机 ID、发货地、目的地、经纬度、司机上传节点 `locationReports` 和订单时间线。
- 司机到达节点上报已经实现：`POST /api/transport-orders/{orderId}/location`，只允许承运司机在“司机已接单 / 运输中”阶段上传。
- 每次司机位置上报会写入 `transport_location_report`，同时追加 `order_timeline` 履约事件。
- Redis GEO 只保存司机和订单最新位置，key 为 `driver:location:geo` 和 `transport:order:location:geo`；MySQL 仍是位置历史的事实来源。
- 前端已经接入“运输追踪”弹窗，订单卡片可打开 tracking 视图；司机端已接入“到达节点”按钮，通过浏览器定位上传经纬度。
- 当前没有接入地图 SDK，不做路线规划、ETA、轨迹回放或后台连续定位；页面展示的是路线节点、司机上传节点和履约时间线。

## 代码范围

主项目是应急物流运力调度与订单协同平台，核心目录如下：

| 目录 | 作用 |
| --- | --- |
| `common-lib` | 公共响应体、异常、常量、用户类型和登录用户模型。 |
| `gateway-service` | Spring Cloud Gateway，负责统一入口、Redis Token 鉴权、用户头透传、限流 key 生成。 |
| `auth-service` | 核心业务服务，包含登录注册、供应商、采购方、司机、订单、MQ、Redis、评价、调度、后台管理。 |
| `web-frontend` | 原生 HTML/CSS/JavaScript 单页前端，按用户角色渲染采购方、供应商、司机、管理员工作台。 |
| `sql/init` | 初始化表结构和演示数据。 |
| `sql/migrations` | 已有库升级脚本。 |
| `infra/openresty` | OpenResty / Nginx Lua 入口限流配置。 |
| `scripts` | 本地启动、停止、OpenResty 启停和冒烟验证脚本。 |
| `performance` | JMeter 压测脚本、数据准备脚本和说明。 |
| `docs` | 架构、API、启动、压测、演示、迭代和本文档。 |

当前工作区里还存在 `算法/`、`sms-demo/` 等未跟踪内容，它们不是这个主项目的一部分。

## 架构主线

请求路径：

```text
web-frontend -> gateway-service -> auth-service -> MySQL / Redis / RabbitMQ
```

核心机制：

- 登录注册由 `AuthController` 和 `AuthServiceImpl` 处理。
- 登录成功后生成 token，Redis 使用 `login:token:{token}` Hash 保存用户信息。
- Gateway 的 `TokenAuthGlobalFilter` 校验 token，成功后透传 `X-User-Id`、`X-User-Type`、`X-Username`、`X-Display-Name`。
- 业务接口集中在 `BusinessDemoController`，核心逻辑集中在 `BusinessDemoService`。
- 订单创建可先写 Redis 临时订单，再通过 RabbitMQ `order.created` 异步落库。
- 采购抢购和司机抢单使用 Redis Lua 预占，RabbitMQ 异步落库，Redisson 锁和 MySQL 条件更新兜底。
- 前端统一通过 `web-frontend/assets/app.js` 的 `requestJson` 调用后端接口。

## 后端模块说明

### common-lib

| 文件 | 功能 |
| --- | --- |
| `Result` | 统一响应结构，接口一般返回 `{ code, message, data }`。 |
| `BusinessException` | 业务异常。 |
| `ErrorCode` | 通用错误码。 |
| `AuthConstants` | 鉴权请求头、Bearer 前缀、用户头字段。 |
| `RedisConstants` | 登录 token Redis key 前缀和 TTL。 |
| `UserType` / `AccountStatus` | 用户类型和账号状态枚举。 |

### gateway-service

| 文件 | 功能 |
| --- | --- |
| `TokenAuthGlobalFilter` | 跳过登录/注册和 OPTIONS；其他请求校验 Redis token；刷新 TTL；向下游写入用户请求头。 |
| `TraceGlobalFilter` | 为请求生成/透传 trace id。 |
| `RateLimitKeyConfig` | Gateway 限流 key 解析，优先用户 ID，其次 token 摘要，最后 IP。 |
| `GatewayExceptionHandler` | 统一写出未授权响应。 |
| `application.yml` | 路由到 `auth-service`，配置登录、敏感接口和普通 API 的 RedisRateLimiter。 |

### auth-service

| 区域 | 功能 |
| --- | --- |
| `AuthController` | 登录、注册、退出、当前用户。 |
| `BusinessDemoController` | 所有业务接口入口。 |
| `AuthServiceImpl` | 多角色账号登录注册、Redis token 写入/删除/查询。 |
| `BusinessDemoService` | 供应商、采购方、司机、订单、RFQ、评价、调度、后台管理、通知等核心业务。 |
| `OrderCreatedConsumer` | 消费订单创建消息，把 Redis 临时订单异步写入 MySQL。 |
| `OrderClaimedConsumer` | 消费采购抢购和司机抢单消息，使用 Redisson 锁和 MySQL 条件更新兜底。 |
| `DispatchRecommendationSupport` | 根据在线状态、距离发货地距离、评分生成司机推荐榜。 |
| `FulfillmentRankingSupport` | 根据订单评价生成采购方、供应商、司机三方履约榜。 |
| `OrderLifecycleSupport` | 验收、付款状态和摘要文本处理。 |

## 数据模型

主要表来自 `sql/init/01_schema.sql`：

| 表 | 用途 |
| --- | --- |
| `purchaser_account` / `purchaser_profile` | 采购方账号和资料，资料包含地址和经纬度。 |
| `supplier_account` / `supplier_profile` | 供应商账号和资料，资料包含资质、地址和经纬度。 |
| `driver_account` / `driver_profile` | 司机账号和车辆资料，包含车辆、在线状态和经纬度。 |
| `admin_account` | 管理员账号。 |
| `material` | 物资基础数据。 |
| `supplier_material` | 供应商供货关系、库存、价格、上下架状态。 |
| `purchase_order` | 核心订单表，同时承载采购订单和运输任务，包含发货地、目的地、经纬度、司机、验收付款摘要等字段。 |
| `purchase_rfq` / `purchase_rfq_quote` | 采购询价和供应商报价。 |
| `driver_follow` | 司机关注采购方关系，用于订单推送。 |
| `order_push_record` | 司机订单推送记录。 |
| `order_review` | 三方履约评价。 |
| `order_acceptance` | 采购方验收签收。 |
| `order_payment` | 采购方付款登记和超时状态。 |
| `order_timeline` | 订单状态变化时间线。 |
| `transport_location_report` | 司机上传到达节点历史，保存订单、司机、经纬度、备注和上传时间。 |

物流相关字段：

```text
origin_address
origin_longitude
origin_latitude
destination_address
destination_longitude
destination_latitude
```

这些字段已经进入 `purchase_order`，并在订单视图、tracking 响应、smoke test 中使用。

司机主动上传的位置不写回 `purchase_order` 历史字段，而是单独进入 `transport_location_report`；Redis GEO 只缓存最新位置，便于后续扩展附近司机、距离计算或实时看板。

## 接口总览

表中的“前端覆盖”含义：

- `已接入`：`web-frontend/assets/app.js` 明确调用该接口。
- `未接入`：后端有接口，但当前前端没有调用。
- `脚本/压测`：主要由脚本或压测调用。

### 鉴权接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/auth/login` | `login` | 用户登录，按用户类型查对应账号表，校验密码，写 Redis token。 | 公开 | 已接入 |
| `POST` | `/auth/register` | `register` | 注册采购方、供应商、司机或管理员账号，成功后直接登录。 | 公开 | 已接入 |
| `DELETE` | `/auth/logout` | `logout` | 删除 Redis token，退出登录。 | 登录用户 | 已接入 |
| `GET` | `/auth/me` | `currentUser` | 根据 token 读取当前登录用户。 | 登录用户 | 已接入 |

### 公共查询接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/suppliers/catalog` | `supplierCatalog` | 查询采购方可见的供应商目录，后端使用供应商资料和物资组装。 | 登录用户 | 已接入 |
| `GET` | `/api/suppliers/{supplierId}/store` | `supplierStore` | 查询供应商店铺详情、资质和物资菜单。 | 登录用户 | 已接入 |
| `GET` | `/api/materials/options` | `materialOptions` | 查询物资下拉选项。 | 登录用户 | 已接入 |
| `GET` | `/api/suppliers/ranking` | `supplierRanking` | 查询供应商履约/高频排行榜。 | 登录用户 | 已接入 |
| `GET` | `/api/rankings/fulfillment` | `fulfillmentRankings` | 查询采购方、供应商、司机三方履约榜。 | 登录用户 | 已接入 |
| `GET` | `/api/suppliers/nearby` | `nearbySuppliers` | 按经纬度和半径查询附近供应商。 | 登录用户 | 已接入 |

### 供应商接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/supplier/materials` | `supplierMaterials` | 查询当前供应商维护的物资。 | 供应商 | 已接入 |
| `POST` | `/api/supplier/materials` | `createSupplierMaterial` | 新增供应商物资。 | 供应商 | 已接入 |
| `PUT` | `/api/supplier/materials/{id}` | `updateSupplierMaterial` | 修改供应商物资。 | 供应商 | 已接入 |
| `POST` | `/api/supplier/materials/{id}/offline` | `offlineSupplierMaterial` | 下架供应商物资。 | 供应商 | 已接入 |
| `GET` | `/api/supplier/qualification` | `supplierQualification` | 查询供应商资质资料。 | 供应商 | 已接入 |
| `PUT` | `/api/supplier/qualification` | `updateSupplierQualification` | 更新供应商资质资料；地址变化时可触发重新定位。 | 供应商 | 已接入 |
| `GET` | `/api/supplier/orders` | `supplierOrders` | 查询当前供应商供货订单。 | 供应商 | 已接入 |
| `POST` | `/api/supplier/orders/{orderId}/confirm` | `confirmSupplierOrder` | 供应商确认供货，扣减库存，订单进入待司机接单。 | 供应商 | 已接入 |
| `POST` | `/api/supplier/orders/{orderId}/reject` | `rejectSupplierOrder` | 供应商拒单。 | 供应商 | 已接入 |
| `GET` | `/api/supplier/rfqs/open` | `supplierOpenRfqs` | 查询供应商可报价的公开 RFQ。 | 供应商 | 已接入 |
| `GET` | `/api/supplier/rfqs/quotes` | `supplierRfqQuotes` | 查询供应商自己的报价。 | 供应商 | 已接入 |
| `POST` | `/api/supplier/rfqs/quotes` | `quoteRfq` | 供应商对 RFQ 报价。 | 供应商 | 已接入 |

### 采购方接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/purchase-orders` | `createPurchaseOrder` | 创建单笔采购订单，先写 Redis 临时订单并发 MQ 异步落库。 | 采购方 | 已接入 |
| `POST` | `/api/purchase-orders/cart/checkout` | `checkoutPurchaseCart` | 将采购清单批量生成采购订单。 | 采购方 | 已接入 |
| `GET` | `/api/purchase-orders/mine` | `purchaserOrders` | 查询采购方自己的订单。 | 采购方 | 已接入 |
| `POST` | `/api/purchase-orders/{orderId}/acceptance` | `acceptPurchaseOrder` | 采购方验收签收或登记异常。 | 采购方 | 已接入 |
| `POST` | `/api/purchase-orders/{orderId}/payment` | `payPurchaseOrder` | 采购方登记付款。 | 采购方 | 已接入 |
| `POST` | `/api/purchase-rfqs` | `createPurchaseRfq` | 创建采购询价 RFQ。 | 采购方 | 已接入 |
| `GET` | `/api/purchase-rfqs/mine` | `purchaserRfqs` | 查询采购方自己的 RFQ。 | 采购方 | 已接入 |
| `GET` | `/api/purchase-rfqs/{rfqId}/quotes` | `purchaserRfqQuotes` | 查询某个 RFQ 的供应商报价。 | 采购方 | 已接入 |
| `POST` | `/api/purchase-rfqs/quotes/{quoteId}/accept` | `acceptRfqQuote` | 采购方接受报价，并生成订单。 | 采购方 | 已接入 |
| `GET` | `/api/purchase-orders/panic-buy/hall` | `panicBuyHall` | 查询可抢购订单资源。 | 登录用户 | 未接入 |
| `POST` | `/api/purchase-orders/{orderId}/panic-buy` | `panicBuyOrder` | 采购方高并发抢购；Redis Lua 预占，MQ 异步落库。 | 采购方 | 脚本/压测，前端未接入 |

### 订单、评价、追踪和调度接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/orders/{orderId}/reviews` | `createOrderReview` | 创建订单评价，沉淀三方履约评分。 | 订单参与方 | 已接入 |
| `GET` | `/api/orders/{orderId}/reviews` | `orderReviews` | 查询订单评价。 | 订单可见用户 | 未接入 |
| `GET` | `/api/orders/{orderId}/timeline` | `orderTimeline` | 查询订单时间线。 | 订单可见用户 | 已接入 |
| `GET` | `/api/transport-orders/{orderId}/tracking` | `transportTracking` | 查询运输追踪视图：状态、司机、起终点经纬度、司机上传节点、时间线。 | 订单可见用户 | 已接入 |
| `GET` | `/api/orders/{orderId}/dispatch-recommendations` | `dispatchRecommendations` | 根据司机在线状态、距发货地距离、司机评分生成调度推荐。 | 订单可见用户 | 已接入 |

### 司机接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/transport-orders/hall` | `transportHall` | 查询待司机接单的运输订单大厅。 | 登录用户 | 已接入 |
| `GET` | `/api/transport-orders/push` | `driverPushOrders` | 查询推送给当前司机的订单。 | 司机 | 已接入 |
| `GET` | `/api/transport-orders/mine` | `driverOrders` | 查询司机已接的运输订单。 | 司机 | 已接入 |
| `POST` | `/api/transport-orders/push/{orderId}/read` | `markPushRead` | 将司机推送订单标记为已读。 | 司机 | 已接入 |
| `POST` | `/api/transport-orders/{orderId}/claim` | `claimTransportOrder` | 司机高并发抢运输单；Redis Lua 预占，MQ 异步绑定司机。 | 司机 | 已接入 |
| `POST` | `/api/transport-orders/{orderId}/start` | `startTransportOrder` | 司机开始运输，状态改为运输中。 | 司机 | 已接入 |
| `POST` | `/api/transport-orders/{orderId}/location` | `reportTransportLocation` | 司机上传到达节点，经纬度入 MySQL 历史表并同步 Redis GEO 最新位置。 | 司机 | 已接入 |
| `POST` | `/api/transport-orders/{orderId}/complete` | `completeTransportOrder` | 司机完成运输，状态改为已完成。 | 司机 | 已接入 |
| `POST` | `/api/drivers/attendance` | `markDriverAttendance` | 标记司机今天在线/离线，使用 Redis BitMap 思路记录出勤。 | 司机 | 已接入 |
| `GET` | `/api/drivers/attendance/today` | `todayDriverAttendance` | 查询司机今日出勤状态。 | 司机 | 已接入 |
| `GET` | `/api/drivers/follows` | `driverFollows` | 查询司机关注的采购方。 | 司机 | 已接入 |
| `POST` | `/api/drivers/follows` | `followPurchaser` | 司机关注一个采购方。 | 司机 | 已接入 |

### 管理员和运维接口

| 方法 | 路径 | Controller 方法 | 功能 | 角色 | 前端覆盖 |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/admin/dashboard` | `adminDashboard` | 管理员运营大盘统计。 | 管理员 | 已接入 |
| `GET` | `/api/admin/suppliers` | `adminSuppliers` | 查询供应商审核列表。 | 管理员 | 已接入 |
| `POST` | `/api/admin/suppliers/{supplierId}/approve` | `approveSupplier` | 审核通过供应商。 | 管理员 | 已接入 |
| `POST` | `/api/admin/suppliers/{supplierId}/reject` | `rejectSupplier` | 审核拒绝/禁用供应商。 | 管理员 | 已接入 |
| `GET` | `/api/admin/orders` | `adminOrders` | 查询全平台订单。 | 管理员 | 已接入 |
| `POST` | `/api/order-push/retry` | `retryOrderPushRecords` | 为缺失推送记录的订单补偿生成司机推送。 | 管理员 | 已接入 |
| `GET` | `/api/mq/dead-letters` | `deadLetterStats` | 查询 RabbitMQ 死信队列统计。 | 管理员 | 已接入 |
| `GET` | `/api/notifications` | `notifications` | 按角色查询通知中心。 | 登录用户 | 已接入 |

## 前端页面和接口覆盖

前端入口是 `web-frontend/index.html`，主要逻辑在：

| 文件 | 功能 |
| --- | --- |
| `assets/app.js` | 请求封装、登录注册、按角色加载数据、所有按钮事件和状态更新。 |
| `assets/js/state.js` | 全局前端状态。 |
| `assets/js/views.js` | 所有页面 HTML 模板。 |
| `assets/js/selectors.js` | 供应商筛选、订单集合等派生数据。 |
| `assets/js/utils.js` | 本地存储、转义、默认数量等工具。 |
| `assets/styles.css` | 页面样式。 |

角色页面：

| 角色 | 页面功能 |
| --- | --- |
| 采购方 | 供应商大厅、店铺详情、采购清单、RFQ、采购订单、验收、付款、评价、附近供应商、调度推荐。 |
| 供应商 | 供货工作台、物资管理、资质维护、RFQ 报价、供货订单确认/拒单、运输追踪、调度推荐、履约榜。 |
| 司机 | 运输大厅、推送订单、我的运输单、关注采购方、出勤状态、抢单、开始运输、上传到达节点、完成运输、运输追踪。 |
| 管理员 | 运营大盘、供应商审核、订单监控、死信统计、推送补偿、运输追踪、调度推荐。 |

前端是一个 SPA，但不是三类角色混在同一个页面里。`web-frontend/assets/js/config.js` 定义角色导航，`assets/app.js` 的 `loadRoleData` 按角色加载不同数据，`assets/js/views.js` 的 `renderRoleContent` 按角色渲染采购方、供应商、司机、管理员工作台。

运输追踪已经有前端状态 `trackingModal`，点击订单卡片中的“运输追踪”会调用 `/api/transport-orders/{orderId}/tracking`。司机端“到达节点”按钮会调用 `uploadTransportLocation`，通过浏览器 Geolocation 获取经纬度后提交 `/api/transport-orders/{orderId}/location`，提交成功后刷新司机订单和已打开的追踪弹窗。

## 运输追踪功能核对

### 已实现

后端：

- DTO：`TransportTrackingView`
- DTO：`TransportLocationReportRequest`、`TransportLocationReportView`
- Entity/Mapper：`TransportLocationReport`、`TransportLocationReportMapper`
- Controller：`BusinessDemoController.transportTracking`
- Controller：`BusinessDemoController.reportTransportLocation`
- Service：`BusinessDemoService.transportTracking`
- Service：`BusinessDemoService.reportTransportLocation`
- tracking 返回字段：`orderId`、`status`、`driverId`、`originAddress`、`originLongitude`、`originLatitude`、`destinationAddress`、`destinationLongitude`、`destinationLatitude`、`locationReports`、`timeline`
- tracking 权限：先查订单，再调用 `assertOrderVisibleToUser` 校验用户可见性。
- 位置上报权限：要求当前用户是司机，且只能上传自己承运订单的位置。
- 位置上报状态：只允许“司机已接单”和“运输中”订单上传。
- 持久化：每次上传写入 `transport_location_report`，并写入 `order_timeline`。
- Redis：同步更新 `driver:location:geo` 和 `transport:order:location:geo` 最新位置，Redis 写入失败不影响 MySQL 历史记录。
- 脚本：`scripts/smoke-test.sh` 会在有司机已接订单时调用 tracking 接口并校验路线字段。

前端：

- 状态：`web-frontend/assets/js/state.js` 有 `trackingModal`。
- 请求：`openTrackingModal` 调用 `/api/transport-orders/{orderId}/tracking`。
- 弹窗：`trackingModalTemplate` 展示路线概览、司机上传节点和履约时间线。
- 上传：`uploadTransportLocation` 调用浏览器 `navigator.geolocation.getCurrentPosition` 获取经纬度，再提交 `/api/transport-orders/{orderId}/location`。
- 刷新：上传成功后刷新角色数据，如果追踪弹窗已打开，会重新拉取 tracking 数据。

### 未实现或不夸大的边界

- 没有接入高德、百度、Mapbox 等地图 SDK。
- 没有路线规划、轨迹回放或 ETA。
- 没有后台连续定位；当前是司机手动点击“到达节点”上传。
- 浏览器定位需要用户授权，生产环境通常需要 HTTPS。
- 浏览器端经纬度存在被伪造风险，生产级系统需要设备可信校验、司机端 App 或风控规则。
- 当前页面展示路线节点和坐标，不伪装成实时地图。

### 对外表述建议

准确表述：

> 项目已经完成运输追踪接口和前端追踪弹窗。后端聚合订单状态、司机、发货地、目的地、经纬度、司机上传节点和时间线；司机端可以点击“到达节点”上传浏览器定位，经纬度历史写入 MySQL，最新位置同步 Redis GEO。当前版本没有接地图 SDK，所以展示的是路线节点、上传节点和履约时间线，不是实时地图导航。

不要说：

> 项目已经实现实时地图导航、自动后台定位、路线规划或 ETA。

## 高并发链路

### 采购方抢购

接口：`POST /api/purchase-orders/{orderId}/panic-buy`

链路：

1. Gateway 鉴权和敏感接口限流。
2. `BusinessDemoService.panicBuyOrder` 校验采购方角色和订单状态。
3. Redis Lua 做库存名额和重复抢购原子校验。
4. 成功后发送 RabbitMQ 消息。
5. `OrderClaimedConsumer` 消费普通抢购消息，Redisson 锁兜底，更新 MySQL 采购方归属和状态。

前端状态：目前没有接入抢购大厅和抢购按钮，主要由压测脚本和后端测试覆盖。

### 司机抢运输单

接口：`POST /api/transport-orders/{orderId}/claim`

链路：

1. 订单必须处于“待司机接单”。
2. Redis Lua 使用 `transport:claim:stock:{orderId}` 和 `transport:claim:driver:{orderId}:{driverId}` 做原子预占和重复提交拦截。
3. 成功后发送 `transport:{orderId}:{driverId}` 消息。
4. `OrderClaimedConsumer.handleTransportOrderClaimed` 消费消息。
5. Redisson 锁保证同一订单消费串行。
6. MySQL 条件更新要求 `status = 待司机接单` 且 `driver_id is null`，避免重复绑定。
7. 更新推送记录并写入订单时间线。

前端状态：已接入司机端“抢运输单”按钮。

## 订单生命周期

主要状态：

```text
待供应商确认
采购方已抢购
待司机接单
司机已接单
运输中
已完成
供应商已拒单
```

常见流转：

1. 采购方下单，订单进入“待供应商确认”。
2. 供应商确认，扣库存，订单进入“待司机接单”。
3. 司机抢单，订单进入“司机已接单”。
4. 司机开始运输，订单进入“运输中”。
5. 司机完成运输，订单进入“已完成”。
6. 采购方验收、付款，参与方评价。

每次关键状态变化会写入 `order_timeline`。

## Redis、RabbitMQ 和限流

Redis 用途：

- 登录 token：`login:token:{token}`
- 供应商目录缓存。
- 采购抢购 Lua 原子预占。
- 司机抢单 Lua 原子预占。
- 司机出勤 BitMap。
- 供应商履约榜 ZSet。
- 附近供应商 GEO。
- 司机/订单最新位置 GEO：`driver:location:geo`、`transport:order:location:geo`。
- Gateway RedisRateLimiter。

RabbitMQ 用途：

- `order.created`：订单创建异步落库。
- `order.claimed`：采购抢购和司机抢单异步落库。
- 死信队列统计用于管理员查看异常消息。

限流：

- OpenResty Lua 令牌桶位于 `infra/openresty/lua/token_bucket.lua`。
- Gateway RedisRateLimiter 位于 `gateway-service/src/main/resources/application.yml`。
- 高风险接口 `/api/purchase-orders/{orderId}/panic-buy` 和 `/api/transport-orders/{orderId}/claim` 使用更严格的限流配置。

## 测试与验证

测试文件：

| 文件 | 覆盖内容 |
| --- | --- |
| `AuthServiceImplTest` | 登录、注册、当前用户、token 行为。 |
| `BusinessDemoControllerTest` | 部分 controller 路由、司机抢单路径变量、调度推荐响应。 |
| `BusinessDemoServicePersistenceTest` | 采购订单、RFQ、验收付款、供应商确认、物流地点、司机位置上报、排行榜等服务逻辑。 |
| `OrderClaimedConsumerTest` | 司机抢运输单 MQ 消费幂等和条件更新。 |
| `TokenAuthGlobalFilterTest` | Gateway token 鉴权和请求头透传。 |
| `RateLimitKeyConfigTest` | 限流 key 解析。 |
| `GatewayRateLimitConfigTest` | Gateway 限流配置存在性。 |
| `web-frontend/assets/app.test.js` | 前端登录、角色页面、调度推荐、榜单、资质、验收付款、RFQ、运输追踪弹窗、司机位置上传等渲染和请求行为。 |
| `web-frontend/assets/js/selectors.test.js` | 前端状态加载、供应商选择、默认采购数量等工具逻辑。 |

常用验证命令：

```bash
mvn test
npm --prefix web-frontend test
scripts/smoke-test.sh
```

`scripts/smoke-test.sh` 需要本地服务已经启动。

## 已知缺口

| 缺口 | 当前状态 | 建议 |
| --- | --- | --- |
| 地图路线展示 | 已有无第三方依赖的路线节点、司机上传节点和时间线，没有地图。 | 后续按演示成本选择接高德/百度/Mapbox，并增加路线规划和 ETA。 |
| 连续实时定位 | 当前是司机手动点击“到达节点”上传，不是后台连续轨迹。 | 生产级可做司机端 App、定时上报、轨迹采样和异常漂移过滤。 |
| 采购方抢购前端 | 后端和压测脚本有，前端未接入抢购大厅。 | 在采购方页面接入 `/api/purchase-orders/panic-buy/hall` 和抢购按钮。 |
| 订单评价查询 | 后端有 `GET /api/orders/{orderId}/reviews`，前端主要只提交评价。 | 在时间线或详情弹窗展示历史评价。 |
| CI | 本地测试可跑，仓库未见 GitHub Actions。 | 增加后端 Maven 和前端 Node 测试 workflow。 |
| 截图/演示素材 | 文档有演示路线，缺少截图或录屏。 | 加 `docs/images/` 并在 README 展示。 |

## 后续对话使用方式

后续如果需要让 Codex 快速理解项目，可以先让它读取：

```text
docs/project-code-guide.md
docs/architecture.md
docs/api.md
docs/iteration-history.md
```

其中本文档负责回答“代码里到底做了什么、每个接口是什么、哪些地方没做完”；`architecture.md` 负责讲架构；`api.md` 负责简版接口清单；`iteration-history.md` 负责项目演进叙事。
