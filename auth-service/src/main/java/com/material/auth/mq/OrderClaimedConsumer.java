package com.material.auth.mq;

import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.entity.PurchaserProfile;
import com.material.auth.mapper.PurchaseOrderMapper;
import com.material.auth.mapper.PurchaserProfileMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class OrderClaimedConsumer {
    private static final String ORDER_PURCHASER_CLAIMED = "采购方已抢购";

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaserProfileMapper purchaserProfileMapper;
    private final RedissonClient redissonClient;

    public OrderClaimedConsumer(PurchaseOrderMapper purchaseOrderMapper,
                                PurchaserProfileMapper purchaserProfileMapper,
                                RedissonClient redissonClient) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaserProfileMapper = purchaserProfileMapper;
        this.redissonClient = redissonClient;
    }

    @RabbitListener(queues = OrderRabbitConfig.ORDER_CLAIMED_QUEUE)
    public void handleOrderClaimed(String message) throws InterruptedException {
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
}
