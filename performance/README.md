# 性能压测

## JMeter 抢购压测

默认压测采购方高并发抢购链路：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
THREADS=100 RAMP_UP=2 STOCK=1 ./performance/run-panic-buy.sh
```

参数说明：

- `THREADS`：并发线程数。
- `RAMP_UP`：线程启动时间，单位秒。
- `LOOPS`：每个线程执行次数。
- `STOCK`：Redis 抢购名额。`1` 用于防超卖场景，`100/500` 用于吞吐场景。
- `ORDER_ID`：压测订单编号，默认 `PO-PERF-PANIC-0001`。

脚本默认用 `redis-cli -c` 连接 Redis Cluster 的 `127.0.0.1:6379` seed 节点，并扫描 `6379-6384` 清理同一订单的 hash-tag 抢购 key。

结果输出：

- `performance/results/<run_id>/result.jtl`：JMeter 原始结果。
- `performance/results/<run_id>/html/index.html`：JMeter HTML 报告。
- `performance/results/<run_id>/summary.txt`：业务维度摘要。

## 大数据量准备

默认生成 1 万采购方、1000 个供应商、3000 个物资、1 万条供货关系、5 万条历史订单：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
./performance/run-seed-large-dataset.sh
```

可通过环境变量调整规模：

```bash
PURCHASERS=10000 SUPPLIERS=1000 MATERIALS=3000 SUPPLIER_MATERIALS=10000 ORDERS=50000 ./performance/run-seed-large-dataset.sh
```

生成数据使用 `bulk_`、`BULK-MAT-`、`PO-BULK-` 前缀，可重复执行。
