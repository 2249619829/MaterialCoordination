package com.material.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.dto.business.PurchaseOrderRequest;
import com.material.auth.entity.Material;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.entity.PurchaserProfile;
import com.material.auth.entity.SupplierMaterial;
import com.material.auth.entity.SupplierProfile;
import com.material.auth.mapper.DriverFollowMapper;
import com.material.auth.mapper.DriverProfileMapper;
import com.material.auth.mapper.MaterialMapper;
import com.material.auth.mapper.OrderPushRecordMapper;
import com.material.auth.mapper.OrderReviewMapper;
import com.material.auth.mapper.OrderTimelineMapper;
import com.material.auth.mapper.PurchaseOrderMapper;
import com.material.auth.mapper.PurchaserProfileMapper;
import com.material.auth.mapper.SupplierMaterialMapper;
import com.material.auth.mapper.SupplierProfileMapper;
import com.material.auth.mapper.SupplierAccountMapper;
import com.material.auth.service.impl.BusinessDemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessDemoServicePersistenceTest {
    @Mock
    private SupplierProfileMapper supplierProfileMapper;
    @Mock
    private SupplierAccountMapper supplierAccountMapper;
    @Mock
    private SupplierMaterialMapper supplierMaterialMapper;
    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private DriverProfileMapper driverProfileMapper;
    @Mock
    private PurchaserProfileMapper purchaserProfileMapper;
    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private DriverFollowMapper driverFollowMapper;
    @Mock
    private OrderPushRecordMapper orderPushRecordMapper;
    @Mock
    private OrderReviewMapper orderReviewMapper;
    @Mock
    private OrderTimelineMapper orderTimelineMapper;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private AmqpAdmin amqpAdmin;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void createOrderCachesPendingOrderPublishesMqAndReadsOrdersFromMapper() {
        BusinessDemoService service = new BusinessDemoService(
                supplierProfileMapper,
                supplierAccountMapper,
                supplierMaterialMapper,
                materialMapper,
                driverProfileMapper,
                purchaserProfileMapper,
                purchaseOrderMapper,
                driverFollowMapper,
                orderPushRecordMapper,
                orderReviewMapper,
                orderTimelineMapper,
                rabbitTemplate,
                amqpAdmin,
                redisTemplate,
                objectMapper
        );
        SupplierProfile supplier = supplierProfile();
        SupplierMaterial supplierMaterial = supplierMaterial();
        Material material = material();
        PurchaserProfile purchaser = purchaserProfile();
        when(supplierProfileMapper.selectOne(any())).thenReturn(supplier);
        when(supplierMaterialMapper.selectList(any())).thenReturn(List.of(supplierMaterial));
        when(materialMapper.selectById(101L)).thenReturn(material);
        when(purchaserProfileMapper.selectOne(any())).thenReturn(purchaser);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        var order = service.createPurchaseOrder(1L, new PurchaseOrderRequest(1L, 101L, "100 吨", "测试落库"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> fieldsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(startsWith(BusinessDemoService.PENDING_ORDER_KEY_PREFIX), fieldsCaptor.capture());
        verify(redisTemplate).expire(startsWith(BusinessDemoService.PENDING_ORDER_KEY_PREFIX), any(Duration.class));
        verify(rabbitTemplate).convertAndSend(
                eq(OrderRabbitConfig.ORDER_EXCHANGE),
                eq(OrderRabbitConfig.ORDER_CREATED_ROUTING_KEY),
                eq(order.id())
        );
        verify(purchaseOrderMapper, never()).insert(any(PurchaseOrder.class));
        assertThat(fieldsCaptor.getValue()).containsEntry("id", order.id());
        assertThat(fieldsCaptor.getValue()).containsEntry("status", "待供应商确认");

        PurchaseOrder persistedOrder = new PurchaseOrder();
        persistedOrder.setId(order.id());
        persistedOrder.setPurchaserId(1L);
        persistedOrder.setPurchaserName(purchaser.getCompanyName());
        persistedOrder.setSupplierId(1L);
        persistedOrder.setSupplierName(supplier.getCompanyName());
        persistedOrder.setMaterialId(101L);
        persistedOrder.setMaterialName(material.getMaterialName());
        persistedOrder.setCategory(material.getCategory());
        persistedOrder.setQuantity("100 吨");
        persistedOrder.setAmount(order.amount());
        persistedOrder.setStatus("待供应商确认");
        persistedOrder.setSource(order.source());
        persistedOrder.setPushedTo(order.pushedTo());
        persistedOrder.setCreateTime(java.time.LocalDateTime.now());
        when(purchaseOrderMapper.selectList(any())).thenReturn(List.of(persistedOrder));

        assertThat(service.purchaserOrders(1L)).extracting("id").containsExactly(order.id());
    }

    private SupplierProfile supplierProfile() {
        SupplierProfile profile = new SupplierProfile();
        profile.setSupplierId(1L);
        profile.setCompanyName("Shanghai Reliable Supplier Co., Ltd.");
        profile.setContactName("张经理");
        profile.setAddress("上海市浦东新区临港物资园");
        profile.setRatingScore(new BigDecimal("96.8"));
        return profile;
    }

    private SupplierMaterial supplierMaterial() {
        SupplierMaterial supplierMaterial = new SupplierMaterial();
        supplierMaterial.setSupplierId(1L);
        supplierMaterial.setMaterialId(101L);
        supplierMaterial.setSupplyPrice(new BigDecimal("3020.00"));
        supplierMaterial.setStockQuantity(420);
        supplierMaterial.setDailyCapacity(120);
        return supplierMaterial;
    }

    private Material material() {
        Material material = new Material();
        material.setId(101L);
        material.setMaterialName("HRB400E 抗震钢筋");
        material.setCategory("钢材");
        material.setUnit("吨");
        return material;
    }

    private PurchaserProfile purchaserProfile() {
        PurchaserProfile profile = new PurchaserProfile();
        profile.setPurchaserId(1L);
        profile.setCompanyName("Shanghai Material Purchaser Co., Ltd.");
        return profile;
    }
}
