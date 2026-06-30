# 接口文档

统一网关地址：`http://localhost:8080`

除登录和注册外，请求头统一携带：

```http
Authorization: Bearer {token}
```

## 登录注册

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 登录 | POST | `/auth/login` |
| 注册 | POST | `/auth/register` |
| 当前用户 | GET | `/auth/me` |
| 退出登录 | DELETE | `/auth/logout` |

## 采购方

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 供应商目录 | GET | `/api/suppliers/catalog` |
| 供应商店铺详情 | GET | `/api/suppliers/{supplierId}/store` |
| 附近供应商 | GET | `/api/suppliers/nearby?longitude=121.47&latitude=31.23&radiusKm=500` |
| 创建采购订单 | POST | `/api/purchase-orders` |
| 采购清单批量提交 | POST | `/api/purchase-orders/cart/checkout` |
| 我的采购订单 | GET | `/api/purchase-orders/mine` |
| 采购方抢购 | POST | `/api/purchase-orders/{orderId}/panic-buy` |

采购清单批量提交示例：

```json
{
  "supplierId": 1,
  "remark": "采购方通过购物车/询价单批量提交",
  "items": [
    {
      "materialId": 101,
      "quantity": "100 吨"
    }
  ]
}
```

## 供应商

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 供应物资列表 | GET | `/api/supplier/materials` |
| 新增供应物资 | POST | `/api/supplier/materials` |
| 更新供应物资 | PUT | `/api/supplier/materials/{id}` |
| 下架供应物资 | POST | `/api/supplier/materials/{id}/offline` |
| 我的供货订单 | GET | `/api/supplier/orders` |
| 确认供货并扣减库存 | POST | `/api/supplier/orders/{orderId}/confirm` |
| 拒绝供货 | POST | `/api/supplier/orders/{orderId}/reject` |
| MQ 死信统计 | GET | `/api/mq/dead-letters` |
| 补偿推送记录 | POST | `/api/order-push/retry` |

## 司机

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 运输大厅 | GET | `/api/transport-orders/hall` |
| 我的运输订单 | GET | `/api/transport-orders/mine` |
| 推送订单 | GET | `/api/transport-orders/push` |
| 标记推送已读 | POST | `/api/transport-orders/push/{orderId}/read` |
| 抢运输单 | POST | `/api/transport-orders/{orderId}/claim` |
| 开始运输 | POST | `/api/transport-orders/{orderId}/start` |
| 上传到达节点 | POST | `/api/transport-orders/{orderId}/location` |
| 完成运输 | POST | `/api/transport-orders/{orderId}/complete` |
| 运输追踪 | GET | `/api/transport-orders/{orderId}/tracking` |
| 司机出勤 | POST | `/api/drivers/attendance?online=true` |
| 今日出勤 | GET | `/api/drivers/attendance/today` |
| 关注采购方列表 | GET | `/api/drivers/follows` |
| 关注采购方 | POST | `/api/drivers/follows` |

上传到达节点示例：

```json
{
  "longitude": 121.473701,
  "latitude": 31.230416,
  "remark": "到达运输节点"
}
```

说明：

- 该接口只允许承运司机调用。
- 订单必须处于“司机已接单”或“运输中”。
- 每次上传会写入 `transport_location_report`，并追加订单时间线。
- Redis GEO 只保存司机和订单最新位置，历史记录以 MySQL 为准。

## 订单协同

| 功能 | 方法 | 路径 |
| --- | --- | --- |
| 订单时间线 | GET | `/api/orders/{orderId}/timeline` |
| 智能调度推荐 | GET | `/api/orders/{orderId}/dispatch-recommendations` |
| 提交履约评价 | POST | `/api/orders/{orderId}/reviews` |
| 查询履约评价 | GET | `/api/orders/{orderId}/reviews` |
| 三方履约排行榜 | GET | `/api/rankings/fulfillment` |
| 消息通知中心 | GET | `/api/notifications` |

订单状态主链路：

```text
待供应商确认 -> 待司机接单 -> 司机已接单 -> 运输中 -> 已完成 -> 三方评价
```

运输订单包含发货地和目的地定位字段：

```json
{
  "originAddress": "上海市浦东新区临港物资园",
  "originLongitude": 121.510000,
  "originLatitude": 31.230000,
  "destinationAddress": "上海市徐汇区应急采购中心",
  "destinationLongitude": 121.430000,
  "destinationLatitude": 31.180000
}
```

运输追踪响应会在起终点基础上聚合司机上传节点和履约时间线：

```json
{
  "orderId": "PO-BULK-00025736",
  "status": "运输中",
  "driverId": 1,
  "originAddress": "上海市浦东新区临港物资园",
  "originLongitude": 121.510000,
  "originLatitude": 31.230000,
  "destinationAddress": "上海市徐汇区应急采购中心",
  "destinationLongitude": 121.430000,
  "destinationLatitude": 31.180000,
  "locationReports": [
    {
      "id": 1,
      "orderId": "PO-BULK-00025736",
      "driverId": 1,
      "longitude": 121.473701,
      "latitude": 31.230416,
      "remark": "到达运输节点",
      "createdAt": "2026-06-30 15:30:00"
    }
  ],
  "timeline": []
}
```
