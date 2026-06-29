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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class OrderCreatedConsumer {
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final OrderTimelineMapper orderTimelineMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * 作用：创建 OrderCreatedConsumer 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - purchaseOrderMapper：采购订单数据库操作对象，类型是 PurchaseOrderMapper；方法会读取这个值继续处理。
     * - orderTimelineMapper：订单时间线数据库操作对象，类型是 OrderTimelineMapper；方法会读取这个值继续处理。
     * - redisTemplate：Redis 操作工具，类型是 StringRedisTemplate；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public OrderCreatedConsumer(PurchaseOrderMapper purchaseOrderMapper,
                                OrderTimelineMapper orderTimelineMapper,
                                StringRedisTemplate redisTemplate) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.orderTimelineMapper = orderTimelineMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 作用：消费订单创建消息，把 Redis 临时订单写入 MySQL。
     * 输入：
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    /**
     * 作用：从 Redis 中读取临时订单并还原成订单对象。
     * 输入：
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrder，也就是这个方法处理后的结果。
     */
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
        order.setOriginAddress(optionalValue(fields, "originAddress"));
        order.setOriginLongitude(optionalDecimal(fields, "originLongitude"));
        order.setOriginLatitude(optionalDecimal(fields, "originLatitude"));
        order.setDestinationAddress(optionalValue(fields, "destinationAddress"));
        order.setDestinationLongitude(optionalDecimal(fields, "destinationLongitude"));
        order.setDestinationLatitude(optionalDecimal(fields, "destinationLatitude"));
        order.setCreateTime(LocalDateTime.parse(value(fields, "createTime")));
        order.setUpdateTime(LocalDateTime.parse(value(fields, "updateTime")));
        return order;
    }

    /**
     * 作用：从 Map 中读取指定字段，并在字段缺失时报错。
     * 输入：
     * - fields：Fields，类型是 Map<Object, Object>；方法会读取这个值继续处理。
     * - key：字段名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String value(Map<Object, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Redis 临时订单缺少字段: " + key);
        }
        return value.toString();
    }

    private String optionalValue(Map<Object, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : value.toString();
    }

    private BigDecimal optionalDecimal(Map<Object, Object> fields, String key) {
        String value = optionalValue(fields, key);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    /**
     * 作用：创建一条系统操作的订单时间线记录。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * - action：操作动作，类型是 String；方法会读取这个值继续处理。
     * - remark：备注，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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
