# JMeter 性能压测报告

## 压测目标

本轮压测针对采购方高并发抢购链路：

```http
POST /api/purchase-orders/{orderId}/panic-buy
```

该链路用于模拟自然灾害场景下多个采购方同时抢购稀缺物资或物流专线名额。系统先通过 Redis Lua 脚本完成原子占位，再通过 RabbitMQ 异步落库，MySQL 订单状态作为最终业务兜底。

## 压测环境

- 压测工具：Apache JMeter 5.6.3
- 压测方式：命令行非 GUI 模式
- 被测环境：本地单机 Gateway + Auth Service
- 中间件：本地 MySQL、Redis、RabbitMQ、Nacos
- 压测订单：`PO-PERF-PANIC-0001`
- 抢购名额：`1`
- 账号策略：自动准备独立采购方账号，避免同一账号重复抢购影响结果

## 数据规模

压测前通过脚本构造了基础业务数据，避免只在空库中验证接口：

| 数据类型 | 数量 |
| --- | ---: |
| 采购方账号 | 11003 |
| 供应商账号 | 1004 |
| 物资 | 3009 |
| 供应商供货关系 | 10010 |
| 历史采购订单 | 50010 |

其中 `bulk_` / `BULK-MAT-` / `PO-BULK-` 前缀为压测背景数据：

| 压测背景数据 | 数量 |
| --- | ---: |
| `bulk_purchaser_%` | 10000 |
| `bulk_supplier_%` | 1000 |
| `BULK-MAT-%` | 3000 |
| bulk 供应商供货关系 | 10000 |
| `PO-BULK-%` | 50000 |

## 执行方式

```bash
cd "/Users/didi/Desktop/MaterialCoordination"

./performance/run-seed-large-dataset.sh

THREADS=100 RAMP_UP=2 STOCK=1 ./performance/run-panic-buy.sh
THREADS=500 RAMP_UP=5 STOCK=1 ./performance/run-panic-buy.sh
THREADS=1000 RAMP_UP=10 STOCK=1 ./performance/run-panic-buy.sh
```

结果文件位于：

```text
performance/results/<run_id>/summary.txt
performance/results/<run_id>/result.jtl
performance/results/<run_id>/html/index.html
```

## 结果口径

- `BUSINESS_SUCCESS`：抢购成功，Redis 占位成功并发送 MQ。
- `SOLD_OUT`：Redis Lua 判断名额已抢完。
- `NOT_BUYING`：首个请求成功后，MQ 消费者已将订单状态更新，后续请求被 MySQL 状态机拦截。
- `DUPLICATE_BUYER`：同一采购方重复抢购，被 Redis buyer key 拦截。

`SOLD_OUT` 和 `NOT_BUYING` 都是业务层面的正常拦截，不代表系统异常。

## 压测结果

| 并发线程 | Ramp Up | 请求数 | 抢购成功 | Redis 售罄拦截 | 状态机拦截 | 平均耗时 | P95 | P99 | QPS |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 100 | 2s | 100 | 1 | 39 | 60 | 127.22ms | 439ms | 481ms | 55.83 |
| 500 | 5s | 500 | 1 | 10 | 489 | 1061.72ms | 2538ms | 3149ms | 67.14 |
| 1000 | 10s | 1000 | 1 | 14 | 985 | 2670.74ms | 5364ms | 5914ms | 66.32 |

## 结论

1. 没有发生超卖。三轮压测中，抢购成功数均为 1，符合抢购名额为 1 的预期。
2. Redis Lua 负责第一层原子占位，能够避免并发请求同时扣减成功。
3. RabbitMQ 消费者会快速更新 MySQL 订单状态，后续请求会被订单状态机拦截，因此高并发下 `NOT_BUYING` 数量较多。
4. 在 5 万历史订单背景下，抢购链路仍没有出现超卖；500 并发和 1000 并发下 P95 明显升高，说明当前本地单实例 Gateway/Auth Service 已出现排队，后续可以通过服务多实例、连接池调优、Redis Cluster 和限流策略继续优化。

## 面试讲法

可以这样讲：

> 我使用 JMeter 对采购方高并发抢购接口做了压测。压测前先构造了 1 万采购方、1000 个供应商、3000 个物资、1 万条供货关系和 5 万条历史订单，再通过脚本批量准备独立采购方账号，并重置一条抢购名额为 1 的订单。请求进入系统后，先经过网关鉴权，再到业务服务执行 Redis Lua 脚本，Lua 同时判断库存和一企一单，成功后发送 RabbitMQ 消息异步落库。压测结果显示，在 100、500、1000 并发下最终都只有 1 个采购方抢购成功，没有发生超卖；后续请求会被 Redis 售罄判断或 MySQL 订单状态机拦截。随着并发升高，P95 延迟明显上升，说明本地单实例服务存在排队，后续可以通过服务横向扩容、Redis Cluster 和网关限流继续优化。

注意不要把这组数据说成生产环境能力。它是本地单机压测，用来验证抢购链路的正确性和发现瓶颈。
