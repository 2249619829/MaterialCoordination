# 项目迭代记录

本文档用于说明项目从基础供应链协同平台逐步演进为应急物流运力调度平台的过程。时间线按功能迭代拆分，便于 GitHub 展示、答辩复盘和面试讲解；具体日期用于表达开发节奏，不绑定单个 commit 的物理提交时间。

## 迭代总览

| 迭代 | 时间 | 主题 | 主要产出 | 验证方式 |
| --- | --- | --- | --- | --- |
| V0.1 | 2026-06-01 至 2026-06-02 | 基础工程与登录鉴权 | Maven 多模块、公共模型、网关鉴权、Redis Token 登录 | `AuthServiceImplTest`、`TokenAuthGlobalFilterTest` |
| V0.2 | 2026-06-03 至 2026-06-04 | 三角色业务闭环 | 采购方、供应商、司机账号与资料模型，供应商目录，采购订单 | `BusinessDemoServicePersistenceTest` |
| V0.3 | 2026-06-05 至 2026-06-06 | 订单协同与前端工作台 | 原生前端 SPA、三角色工作台、采购清单、订单状态推进 | 前端手工验证、`app.test.js` |
| V0.4 | 2026-06-07 至 2026-06-08 | 高并发抢购与异步落库 | Redis Lua 抢购预占、RabbitMQ 消费、Redisson 锁兜底、供应商确认供货 | Java 单测、JMeter 初步压测 |
| V0.5 | 2026-06-09 至 2026-06-10 | Fleetbase 风格物流升级 | 发货地/目的地经纬度、运输订单池、司机高并发抢单、运输追踪 | `OrderClaimedConsumerTest`、运输追踪接口测试 |
| V0.6 | 2026-06-11 至 2026-06-13 | 调度推荐与履约排行榜 | 智能调度推荐、采购方/供应商/司机三方履约榜、更多前端展示 | Java 单测、前端 Node 测试 |
| V0.7 | 2026-06-14 至 2026-06-17 | 网关限流与本地可运行性 | Gateway RedisRateLimiter、OpenResty Lua 令牌桶、启动/停止/冒烟脚本 | `GatewayRateLimitConfigTest`、`scripts/smoke-test.sh` |
| V0.8 | 2026-06-18 至 2026-06-21 | 压测与性能说明 | JMeter 脚本、大数据量准备脚本、性能瓶颈说明 | `performance/run-panic-buy.sh` |
| V0.9 | 2026-06-22 至 2026-06-25 | 文档和演示包装 | README、架构文档、API 文档、启动文档、演示讲解稿、Windows 交接说明 | 文档走读、干净副本启动检查 |
| V1.0 | 2026-06-26 至 2026-06-29 | 项目展示收口 | 迭代记录、GitHub 展示清理、测试命令补齐、提交说明规范化 | `mvn test`、`npm test` |
| V1.1 | 2026-06-29 | 前端视觉升级 | 白底应急物流调度台、工业调度感信息层级、角色工作台视觉统一 | 前端 Node 测试、浏览器截图检查 |
| V1.2 | 2026-06-30 | 前端运输追踪接入 | 运输追踪弹窗设计、tracking 接口接入方案、路线节点和时间线聚合展示 | 设计走读、前端 Node 测试、`scripts/smoke-test.sh` |
| V1.3 | 2026-06-30 | 司机到达节点上报 | 浏览器定位上传、位置上报表、运输追踪司机节点、Redis GEO 最新位置缓存 | 后端 JUnit、前端 Node 测试、`scripts/smoke-test.sh` |

## V0.1 基础工程与登录鉴权

时间：2026-06-01 至 2026-06-02

目标是先搭出后端工程骨架，让后续业务不散落在单体脚本里。项目采用父级 Maven 聚合工程，拆出 `common-lib`、`gateway-service` 和 `auth-service`。公共模块负责响应体、异常、用户类型和常量；网关负责统一入口和登录态校验；业务服务负责账号登录、注册和角色相关业务。

核心设计选择：

- 登录态不使用纯 JWT，而是使用 Redis Hash 存储 `login:token:{token}`，便于服务端主动失效、续期和多端控制。
- Gateway 校验通过后向下游透传 `X-User-Id`、`X-User-Type`、`X-Username`、`X-Display-Name`，业务服务不重复解析 token。
- 账号按采购方、供应商、司机、管理员拆表建模，避免后续角色字段无限膨胀。

## V0.2 三角色业务闭环

时间：2026-06-03 至 2026-06-04

这一阶段把项目从登录 Demo 推进到供应链协同业务。围绕采购方、供应商、司机三类角色补齐资料、物资、订单、关注关系、通知中心和基础履约数据。

主要功能：

- 采购方可以浏览供应商目录、查看供应商店铺、提交采购订单。
- 供应商可以维护供应物资、查看供货订单、确认或拒绝订单。
- 司机可以关注采购方、查看推送订单和自己的运输任务。
- 订单状态从“待供应商确认”开始，逐步推进到“待司机接单”“司机已接单”“运输中”“已完成”。

这一阶段也开始引入 Cache Aside：供应商目录属于读多写少场景，读请求先查 Redis，未命中再查 MySQL 并回填缓存；供应商修改物资后删除缓存并延迟双删。

## V0.3 订单协同与前端工作台

时间：2026-06-05 至 2026-06-06

这一阶段补上可演示的前端界面。前端没有使用重框架，而是用原生 HTML/CSS/JavaScript 组织成单页应用，便于展示业务流程和减少部署复杂度。

主要功能：

- 登录页支持按角色进入不同工作台。
- 采购方页面提供供应商大厅、搜索筛选、店铺详情、采购清单和订单列表。
- 供应商页面提供供货工作台、物资管理、订单处理和资质展示。
- 司机页面提供运输大厅、推送订单、关注采购方和车辆资料。
- 登录状态迁移到 `sessionStorage`，降低不同浏览器标签页之间串号的概率。

## V0.4 高并发抢购与异步落库

时间：2026-06-07 至 2026-06-08

这一阶段把项目的后端亮点从普通 CRUD 提升到高并发链路。采购方抢购接口不直接让所有请求打 MySQL，而是先进入 Redis Lua 原子脚本做库存和重复抢购校验，成功后发送 RabbitMQ 消息异步落库。

链路设计：

1. Gateway 校验 Redis Token。
2. `auth-service` 校验订单状态和用户角色。
3. Redis Lua 原子扣减抢购名额并记录采购方抢购 key。
4. 成功请求发送 `order.claimed` 消息。
5. 消费者使用 Redisson 锁和 MySQL 条件更新兜底。

这个设计的重点是不超卖、不重复抢购，并且把高峰写入压力从同步数据库更新转为 Redis 原子预占 + MQ 异步处理。

## V0.5 Fleetbase 风格物流升级

时间：2026-06-09 至 2026-06-10

这一阶段重新包装项目定位：从“数字化供应链协同”升级为“应急物流运力调度与订单协同平台”。参考 Fleetbase 的订单运营模型，把订单、地点、司机、追踪和调度作为主线。

主要功能：

- `purchase_order` 增加发货地、目的地、经度、纬度等地点字段。
- 供应商确认供货后，订单进入待分配运输订单池。
- 司机抢运输单使用 Redis Lua 原子预占，发送 `transport:{orderId}:{driverId}` 消息。
- 消费者异步绑定司机，使用 Redisson 锁和 MySQL 条件更新保证幂等。
- 新增 `GET /api/transport-orders/{orderId}/tracking`，聚合订单状态、司机、地点和时间线。

## V0.6 调度推荐与履约排行榜

时间：2026-06-11 至 2026-06-13

这一阶段加强“物流调度平台”的解释力。系统不只是让司机抢单，还能根据司机在线状态、距发货地距离和评分生成推荐榜；同时把履约评价从供应商单榜扩展为采购方、供应商、司机三方榜。

主要功能：

- 新增智能调度推荐视图，展示推荐司机、车辆、距离、评分、推荐分和原因。
- 三类角色首页展示采购方、供应商、司机三张履约排行榜。
- 订单评价沉淀到排行榜，供应商高频榜继续同步 Redis ZSet。
- 前端补充调度推荐和排行榜展示测试。

## V0.7 网关限流与本地可运行性

时间：2026-06-14 至 2026-06-17

这一阶段重点解决“别人 clone 后能不能跑起来”和“高并发链路有没有入口层保护”。项目增加了两层限流和一组本地运维脚本。

主要功能：

- Gateway 增加 RedisRateLimiter，按用户 token、IP 或路径生成限流 key。
- `infra/openresty` 增加 Nginx Lua 令牌桶，作为入口层第一道保护。
- `scripts/start-local.sh` 启动后端、网关和前端。
- `scripts/start-openresty.sh` 启动 OpenResty 入口。
- `scripts/smoke-test.sh` 自动验证登录、前端可达、运输订单接口和路线坐标。
- Nacos 默认注册到 `127.0.0.1`，避免本机网络变化后网关拿到失效内网 IP。

## V0.8 压测与性能说明

时间：2026-06-18 至 2026-06-21

这一阶段补上后端项目展示里很重要的性能验证。项目保留 JMeter 脚本和压测数据准备脚本，明确说明这是本地单机压测，不把结果夸大成生产能力。

压测覆盖：

- 构造 1 万采购方、1000 个供应商、3000 个物资、1 万条供货关系、5 万条历史订单。
- 对采购方高并发抢购接口执行 100、500、1000 并发压测。
- 记录成功、售罄拦截、状态机拦截、平均耗时、P95、P99 和 QPS。

结论是：在抢购名额为 1 的场景下没有发生超卖；500 和 1000 并发下 P95 明显升高，说明本地单实例服务出现排队，后续可以通过多实例、连接池调优、Redis Cluster 和更细粒度限流继续优化。

## V0.9 文档和演示包装

时间：2026-06-22 至 2026-06-25

这一阶段把项目从“能跑”整理成“能讲”。文档覆盖 README、架构、API、启动、演示路线和 Windows 迁移说明。

文档产出：

- `README.md`：项目定位、技术栈、核心能力、启动入口、演示账号和关键接口。
- `docs/architecture.md`：架构图、核心流程图和面试讲法。
- `docs/api.md`：主要接口清单。
- `docs/startup.md`：本地启动、排障和脚本说明。
- `docs/demo-guide.md`：5 分钟和 15 分钟演示路线。
- `docs/windows-setup.md`：Windows / WSL2 迁移启动说明。

## V1.0 项目展示收口

时间：2026-06-26 至 2026-06-29

这一阶段围绕 GitHub 展示做收口：补齐迭代记录，明确测试命令，检查仓库结构和敏感信息，整理提交说明。

当前验证命令：

```bash
mvn test
npm --prefix web-frontend test
```

建议后续继续补充：

- GitHub Actions：自动执行后端和前端测试。
- 截图或录屏：补到 `docs/images/` 并在 README 中展示。
- GitHub Pages 或个人作品站：前端可静态部署，后端用本地/演示环境说明。
- 更细粒度提交：后续每个功能按“后端模型 / 前端展示 / 文档 / 测试”拆 commit。

## V1.1 前端视觉升级

时间：2026-06-29

这一阶段聚焦前端展示质感，不改变后端接口、不引入 React/Vue 等新框架，继续保留原生 HTML/CSS/JavaScript 的轻量实现。视觉方向确定为“白底应急物流调度台”：页面保持清爽白底，但信息组织更像应急物流运营中心，而不是普通表单后台。

设计取舍：

- 不采用黑色大屏作为主方向，避免演示时过重、过假。
- 不采用通用 SaaS 后台模板作为主方向，避免采购、供货、运力调度这些业务特征被弱化。
- 选择白底工业调度台：用冷白/浅灰背景、深色文字、细边框、紧凑数据卡、状态色和订单池结构表达“调度中心”气质。

主要改造点：

- 登录页强调应急采购、供应商供货、司机运力调度三类链路。
- 应用主界面强化顶部运行状态、角色身份、消息和退出操作的层次。
- 侧边栏、数据卡、订单列表、排行榜、供应商卡片、表单和弹窗统一为调度台视觉语言。
- 状态标签继续使用蓝绿、橙色、红色等语义色：蓝绿表示运输/调度，橙色表示待处理，红色表示异常。
- 保持 8px 左右的圆角、清晰边框和稳定尺寸，避免营销页式大卡片、装饰光斑和过度渐变。

验证方式：

- `npm --prefix web-frontend test` 验证现有前端渲染与交互测试。
- 浏览器访问 `http://localhost:5173`，检查登录页、角色工作台、桌面和移动宽度下是否有文本溢出、遮挡、空白页或控制台错误。

## V1.2 前端运输追踪接入

时间：2026-06-30

这一阶段目标是把已经完成的后端运输追踪接口接入前端，让面试和演示时不再只停留在“后端有 tracking 接口”的表述。前端仍保持原生 HTML/CSS/JavaScript，不引入地图 SDK；先做一个业务后台可解释的运输追踪弹窗，展示订单当前状态、承运司机、发货地、目的地、经纬度和状态时间线。

设计范围：

- 在订单卡片操作区新增“运输追踪”按钮，面向采购方、供应商、司机、管理员可见订单复用同一入口。
- 点击后调用 `GET /api/transport-orders/{orderId}/tracking`，把结果写入前端 `trackingModal` 状态。
- 弹窗顶部展示订单编号、当前状态、司机 ID 和物资名称。
- 弹窗主体分为两块：路线概览和履约时间线。
- 路线概览展示“发货地 -> 目的地”的节点式布局，包含地址、经度、纬度，不伪装成实时地图。
- 履约时间线复用现有时间线样式，展示后端返回的 timeline；如果没有时间线，显示空态。
- 请求失败时使用现有 toast 机制提示“运输追踪加载失败”，不让页面卡死。

不在本阶段范围内：

- 不接入高德、百度、Mapbox 等地图 SDK。
- 不做实时司机位置上报、轨迹回放、ETA 预测或路线规划。
- 不新增后端 tracking 字段，优先使用现有 `TransportTrackingView`。

实现切入点：

- `web-frontend/assets/js/state.js`：新增 `trackingModal` 状态。
- `web-frontend/assets/app.js`：新增 `openTrackingModal`、`closeTrackingModal`，绑定 `data-order-tracking` 和关闭事件。
- `web-frontend/assets/js/views.js`：新增 `trackingModalTemplate`，并在订单操作按钮中加入“运输追踪”。
- `web-frontend/assets/styles.css`：新增路线节点、坐标、追踪弹窗的紧凑样式。
- `web-frontend/assets/app.test.js`：补充 tracking 弹窗渲染、按钮出现和接口状态的测试。

验收标准：

- 订单卡片中能看到“运输追踪”按钮。
- 点击后调用 `/api/transport-orders/{orderId}/tracking`，弹窗展示状态、司机、发货地、目的地、经纬度和时间线。
- 真实环境下 `scripts/smoke-test.sh` 继续通过，确认后端 tracking 字段仍完整。
- 前端 `npm --prefix web-frontend test` 通过，覆盖按钮和弹窗渲染。
- 浏览器检查桌面和移动端弹窗，不出现文本重叠、横向溢出或控制台错误。

## V1.3 司机到达节点上报

时间：2026-06-30

这一阶段把运输追踪从“展示起终点和状态时间线”升级为“司机可主动上报到达节点”。司机在运输订单卡片点击“到达节点”后，前端通过浏览器 Geolocation 获取当前经纬度，调用后端位置上报接口；后端校验司机是否属于该订单，再将本次位置节点持久化，并同步更新 Redis GEO 中的司机/订单最新位置。

设计范围：

- 司机端在“司机已接单”和“运输中”订单上展示“到达节点”按钮。
- 前端调用浏览器 `navigator.geolocation` 获取经纬度，不接地图 SDK，不需要地图 API key。
- 新增 `POST /api/transport-orders/{orderId}/location`，接收 `longitude`、`latitude` 和 `remark`。
- 新增 `transport_location_report` 表，保存每次司机上传的位置历史，避免从时间线备注里解析坐标。
- 上传成功后写入订单时间线，action 为“司机上传到达节点”，用于履约过程展示。
- Redis GEO 只保存司机和订单最新位置，分别使用 `driver:location:geo` 和 `transport:order:location:geo`，用于后续附近查询、距离计算或实时位置扩展。
- 运输追踪弹窗新增“司机上传节点”区域，聚合展示上传时间、司机、备注、经纬度。

实现切入点：

- `auth-service/src/main/java/com/material/auth/entity/TransportLocationReport.java`：新增位置上报实体。
- `auth-service/src/main/java/com/material/auth/mapper/TransportLocationReportMapper.java`：新增 MyBatis Plus mapper。
- `auth-service/src/main/java/com/material/auth/dto/business/TransportLocationReportRequest.java`：新增上传请求 DTO。
- `auth-service/src/main/java/com/material/auth/dto/business/TransportLocationReportView.java`：新增上传节点视图 DTO。
- `auth-service/src/main/java/com/material/auth/dto/business/TransportTrackingView.java`：扩展 `locationReports` 字段。
- `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`：新增上传校验、MySQL 持久化、时间线写入和 Redis GEO 更新。
- `auth-service/src/main/java/com/material/auth/controller/BusinessDemoController.java`：新增司机位置上报接口。
- `web-frontend/assets/app.js`：新增 `uploadTransportLocation`，完成浏览器定位、接口调用、刷新追踪弹窗。
- `web-frontend/assets/js/views.js`：新增“到达节点”按钮和 tracking 弹窗上传节点展示。
- `web-frontend/assets/styles.css`：新增司机上传节点样式。
- `sql/init/01_schema.sql` 和 `sql/migrations/20260630_transport_location_report.sql`：新增位置上报表。

验收标准：

- 司机运输阶段订单可以点击“到达节点”。
- 浏览器授权定位后，前端上传当前经纬度到后端。
- 后端拒绝非承运司机上传，拒绝非运输阶段订单上传。
- 每次上传在 `transport_location_report` 留存历史记录，并在 `order_timeline` 出现履约事件。
- Redis GEO 写入失败不影响 MySQL 历史记录。
- 运输追踪弹窗能展示司机上传节点和经纬度。

## 推荐 Git 提交说明

本次整理建议使用一个聚合提交：

```text
Document project iterations and logistics platform polish
```

提交描述建议写：

```text
- add an iteration history for the emergency logistics platform evolution
- document startup, demo, performance, and architecture updates
- include OpenResty and local operation scripts for repeatable demos
- extend logistics dispatch, tracking, rankings, and rate-limit coverage
- keep unrelated algorithm practice and standalone SMS demo out of this project commit
```
