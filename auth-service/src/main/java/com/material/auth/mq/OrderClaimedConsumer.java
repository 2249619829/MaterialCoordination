package com.material.auth.mq;

import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.entity.OrderPushRecord;
import com.material.auth.entity.OrderTimeline;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.entity.PurchaserProfile;
import com.material.auth.mapper.OrderPushRecordMapper;
import com.material.auth.mapper.OrderTimelineMapper;
import com.material.auth.mapper.PurchaseOrderMapper;
import com.material.auth.mapper.PurchaserProfileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class OrderClaimedConsumer {
    private static final String ORDER_PURCHASER_CLAIMED = "采购方已抢购";
    private static final String ORDER_WAITING_DRIVER = "待司机接单";
    private static final String ORDER_CLAIMED = "司机已接单";
    private static final String PUSH_STATUS_CLAIMED = "CLAIMED";

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaserProfileMapper purchaserProfileMapper;
    private final OrderPushRecordMapper orderPushRecordMapper;
    private final OrderTimelineMapper orderTimelineMapper;
    private final RedissonClient redissonClient;

    /**
     * 作用：创建 OrderClaimedConsumer 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - purchaseOrderMapper：采购订单数据库操作对象，类型是 PurchaseOrderMapper；方法会读取这个值继续处理。
     * - purchaserProfileMapper：采购方资料数据库操作对象，类型是 PurchaserProfileMapper；方法会读取这个值继续处理。
     * - redissonClient：Redisson 客户端，类型是 RedissonClient；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public OrderClaimedConsumer(PurchaseOrderMapper purchaseOrderMapper,
                                PurchaserProfileMapper purchaserProfileMapper,
                                OrderPushRecordMapper orderPushRecordMapper,
                                OrderTimelineMapper orderTimelineMapper,
                                RedissonClient redissonClient) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaserProfileMapper = purchaserProfileMapper;
        this.orderPushRecordMapper = orderPushRecordMapper;
        this.orderTimelineMapper = orderTimelineMapper;
        this.redissonClient = redissonClient;
    }

    /**
     * 作用：消费抢购成功消息，把抢购结果写入 MySQL。
     * 输入：
     * - message：消息内容，通常来自 RabbitMQ 或错误提示。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @RabbitListener(queues = OrderRabbitConfig.ORDER_CLAIMED_QUEUE)
    public void handleOrderClaimed(String message) throws InterruptedException {
        if (message.startsWith("transport:")) {
            handleTransportOrderClaimed(message);
            return;
        }
        String[] parts = message.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("抢购消息格式错误: " + message);
        }
        String orderId = parts[0];
        Long purchaserId = Long.valueOf(parts[1]);
        RLock lock = redissonClient.getLock("lock:order:claim:" + orderId);
        boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (!locked) {
            throw new IllegalStateException("获取订单抢购落库锁失败: " + orderId);
        }
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
            if (order == null) {
                throw new IllegalArgumentException("抢购订单不存在: " + orderId);
            }
            if (ORDER_PURCHASER_CLAIMED.equals(order.getStatus())) {
                return;
            }
            PurchaserProfile purchaser = purchaserProfileMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurchaserProfile>()
                            .eq(PurchaserProfile::getPurchaserId, purchaserId)
            );
            if (purchaser == null) {
                throw new IllegalArgumentException("采购方不存在: " + purchaserId);
            }
            order.setPurchaserId(purchaserId);
            order.setPurchaserName(purchaser.getCompanyName());
            order.setStatus(ORDER_PURCHASER_CLAIMED);
            order.setPushedTo("采购方 " + purchaserId + " 已抢购成功，等待供应商确认");
            order.setUpdateTime(LocalDateTime.now());
            purchaseOrderMapper.updateById(order);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void handleTransportOrderClaimed(String message) throws InterruptedException {
        String[] parts = message.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("运输抢单消息格式错误: " + message);
        }
        String orderId = parts[1];
        Long driverId = Long.valueOf(parts[2]);
        RLock lock = redissonClient.getLock("lock:transport:claim:" + orderId);
        boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (!locked) {
            throw new IllegalStateException("获取运输抢单落库锁失败: " + orderId);
        }
        try {
            PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
            if (order == null) {
                throw new IllegalArgumentException("运输订单不存在: " + orderId);
            }
            if (ORDER_CLAIMED.equals(order.getStatus()) && driverId.equals(order.getDriverId())) {
                return;
            }
            PurchaseOrder update = new PurchaseOrder();
            update.setStatus(ORDER_CLAIMED);
            update.setDriverId(driverId);
            update.setPushedTo("司机 " + driverId + " 已抢单");
            update.setUpdateTime(LocalDateTime.now());
            int rows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getId, orderId)
                    .eq(PurchaseOrder::getStatus, ORDER_WAITING_DRIVER)
                    .isNull(PurchaseOrder::getDriverId));
            if (rows <= 0) {
                return;
            }
            OrderPushRecord pushRecord = orderPushRecordMapper.selectOne(new LambdaQueryWrapper<OrderPushRecord>()
                    .eq(OrderPushRecord::getDriverId, driverId)
                    .eq(OrderPushRecord::getOrderId, orderId));
            if (pushRecord != null) {
                pushRecord.setStatus(PUSH_STATUS_CLAIMED);
                pushRecord.setUpdateTime(LocalDateTime.now());
                orderPushRecordMapper.updateById(pushRecord);
            }
            createTimeline(orderId, driverId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void createTimeline(String orderId, Long driverId) {
        OrderTimeline timeline = new OrderTimeline();
        timeline.setOrderId(orderId);
        timeline.setStatus(ORDER_CLAIMED);
        timeline.setAction("司机抢运输单异步落库");
        timeline.setOperatorType("DRIVER");
        timeline.setOperatorId(driverId);
        timeline.setRemark("RabbitMQ 消费成功，MySQL 已绑定承运司机");
        timeline.setCreateTime(LocalDateTime.now());
        orderTimelineMapper.insert(timeline);
    }
}
