package com.material.auth.mq;

import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.entity.OrderTimeline;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.mapper.OrderTimelineMapper;
import com.material.auth.mapper.PurchaseOrderMapper;
import com.material.auth.service.impl.BusinessDemoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class OrderCreatedConsumer {
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final OrderTimelineMapper orderTimelineMapper;
    private final StringRedisTemplate redisTemplate;

    public OrderCreatedConsumer(PurchaseOrderMapper purchaseOrderMapper,
                                OrderTimelineMapper orderTimelineMapper,
                                StringRedisTemplate redisTemplate) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.orderTimelineMapper = orderTimelineMapper;
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = OrderRabbitConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            order = loadPendingOrder(orderId);
            purchaseOrderMapper.insert(order);
            createTimeline(order, "MQ 异步落库完成", "订单已从 Redis 临时订单写入 MySQL");
        }
        redisTemplate.delete(BusinessDemoService.PENDING_ORDER_KEY_PREFIX + orderId);
    }

    private PurchaseOrder loadPendingOrder(String orderId) {
        String key = BusinessDemoService.PENDING_ORDER_KEY_PREFIX + orderId;
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Redis 临时订单不存在，无法异步落库: " + orderId);
        }
        PurchaseOrder order = new PurchaseOrder();
        order.setId(value(fields, "id"));
        order.setPurchaserId(Long.valueOf(value(fields, "purchaserId")));
        order.setPurchaserName(value(fields, "purchaserName"));
        order.setSupplierId(Long.valueOf(value(fields, "supplierId")));
        order.setSupplierName(value(fields, "supplierName"));
        order.setMaterialId(Long.valueOf(value(fields, "materialId")));
        order.setMaterialName(value(fields, "materialName"));
        order.setCategory(value(fields, "category"));
        order.setQuantity(value(fields, "quantity"));
        order.setAmount(value(fields, "amount"));
        order.setStatus(value(fields, "status"));
        order.setSource(value(fields, "source"));
        order.setPushedTo(value(fields, "pushedTo"));
        order.setCreateTime(LocalDateTime.parse(value(fields, "createTime")));
        order.setUpdateTime(LocalDateTime.parse(value(fields, "updateTime")));
        return order;
    }

    private String value(Map<Object, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Redis 临时订单缺少字段: " + key);
        }
        return value.toString();
    }

    private void createTimeline(PurchaseOrder order, String action, String remark) {
        OrderTimeline timeline = new OrderTimeline();
        timeline.setOrderId(order.getId());
        timeline.setStatus(order.getStatus());
        timeline.setAction(action);
        timeline.setOperatorType("SYSTEM");
        timeline.setOperatorId(0L);
        timeline.setRemark(remark);
        timeline.setCreateTime(LocalDateTime.now());
        orderTimelineMapper.insert(timeline);
    }
}
