package com.material.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.dto.business.OrderAcceptanceRequest;
import com.material.auth.dto.business.OrderPaymentRequest;
import com.material.auth.dto.business.PurchaseOrderRequest;
import com.material.auth.dto.business.PurchaseRfqRequest;
import com.material.auth.dto.business.SupplierQualificationRequest;
import com.material.auth.dto.business.SupplierQuoteRequest;
import com.material.auth.entity.PurchaseRfq;
import com.material.auth.entity.PurchaseRfqQuote;
import com.material.auth.dto.business.SupplierMaterialManageRequest;
import com.material.auth.entity.Material;
import com.material.auth.entity.OrderAcceptance;
import com.material.auth.entity.OrderPayment;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.entity.PurchaserProfile;
import com.material.auth.entity.SupplierMaterial;
import com.material.auth.entity.SupplierAccount;
import com.material.auth.entity.SupplierProfile;
import com.material.auth.mapper.DriverFollowMapper;
import com.material.auth.mapper.DriverProfileMapper;
import com.material.auth.mapper.MaterialMapper;
import com.material.auth.mapper.OrderAcceptanceMapper;
import com.material.auth.mapper.OrderPaymentMapper;
import com.material.auth.mapper.OrderPushRecordMapper;
import com.material.auth.mapper.OrderReviewMapper;
import com.material.auth.mapper.OrderTimelineMapper;
import com.material.auth.mapper.PurchaseOrderMapper;
import com.material.auth.mapper.PurchaseRfqMapper;
import com.material.auth.mapper.PurchaseRfqQuoteMapper;
import com.material.auth.mapper.PurchaserProfileMapper;
import com.material.auth.mapper.SupplierMaterialMapper;
import com.material.auth.mapper.SupplierProfileMapper;
import com.material.auth.mapper.SupplierAccountMapper;
import com.material.auth.service.geo.Coordinates;
import com.material.auth.service.geo.GeocodingService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private PurchaseRfqMapper purchaseRfqMapper;
    @Mock
    private PurchaseRfqQuoteMapper purchaseRfqQuoteMapper;
    @Mock
    private DriverFollowMapper driverFollowMapper;
    @Mock
    private OrderPushRecordMapper orderPushRecordMapper;
    @Mock
    private OrderAcceptanceMapper orderAcceptanceMapper;
    @Mock
    private OrderPaymentMapper orderPaymentMapper;
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
    @Mock
    private GeocodingService geocodingService;

    /**
     * 作用：完成 createOrderCachesPendingOrderPublishesMqAndReadsOrdersFromMapper 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void createOrderCachesPendingOrderPublishesMqAndReadsOrdersFromMapper() {
        BusinessDemoService service = service();
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

    /**
     * 作用：完成 saveSupplierMaterialRejectsAmbiguousExistingAndNewMaterialInput 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void saveSupplierMaterialRejectsAmbiguousExistingAndNewMaterialInput() {
        BusinessDemoService service = service();

        assertThatThrownBy(() -> service.saveSupplierMaterial(1L, new SupplierMaterialManageRequest(
                101L,
                "应急帐篷",
                "应急物资",
                "件",
                new BigDecimal("849.98"),
                300,
                80,
                new BigDecimal("180.00"),
                1
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不要同时选择已有物资和填写新物资");
    }

    /**
     * 作用：完成 createOrderOverloadUsesMaterialUnitForDefaultQuantity 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void createOrderOverloadUsesMaterialUnitForDefaultQuantity() {
        BusinessDemoService service = service();
        SupplierProfile supplier = supplierProfile();
        SupplierMaterial supplierMaterial = supplierMaterial();
        Material material = material();
        material.setUnit("箱");
        PurchaserProfile purchaser = purchaserProfile();
        when(supplierProfileMapper.selectOne(any())).thenReturn(supplier);
        when(supplierMaterialMapper.selectList(any())).thenReturn(List.of(supplierMaterial));
        when(materialMapper.selectById(101L)).thenReturn(material);
        when(purchaserProfileMapper.selectOne(any())).thenReturn(purchaser);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        var order = service.createPurchaseOrder(1L, 1L, 101L);

        assertThat(order.quantity()).isEqualTo("100 箱");
    }

    @Test
    void purchaserRfqQuotesAreSortedByPriceDeliveryAndSupplierRating() {
        BusinessDemoService service = service();
        PurchaseRfq rfq = rfq();
        SupplierMaterial firstMaterial = supplierMaterial();
        firstMaterial.setId(11L);
        firstMaterial.setSupplierId(1L);
        SupplierMaterial secondMaterial = supplierMaterial();
        secondMaterial.setId(12L);
        secondMaterial.setSupplierId(2L);
        SupplierProfile firstSupplier = supplierProfile();
        firstSupplier.setSupplierId(1L);
        firstSupplier.setCompanyName("A 高价供应商");
        firstSupplier.setRatingScore(new BigDecimal("4.20"));
        SupplierProfile secondSupplier = supplierProfile();
        secondSupplier.setSupplierId(2L);
        secondSupplier.setCompanyName("B 优选供应商");
        secondSupplier.setRatingScore(new BigDecimal("4.90"));
        PurchaseRfqQuote expensive = quote(1001L, 1L, 11L, "860.00", 2);
        PurchaseRfqQuote cheap = quote(1002L, 2L, 12L, "820.00", 3);
        when(purchaseRfqMapper.selectById(10L)).thenReturn(rfq);
        when(purchaseRfqQuoteMapper.selectList(any())).thenReturn(List.of(expensive, cheap));
        when(supplierMaterialMapper.selectBatchIds(any())).thenReturn(List.of(firstMaterial, secondMaterial));
        when(materialMapper.selectBatchIds(any())).thenReturn(List.of(material()));
        when(supplierProfileMapper.selectList(any())).thenReturn(List.of(firstSupplier, secondSupplier));

        var quotes = service.purchaserRfqQuotes(1L, 10L);

        assertThat(quotes).extracting("supplierName").containsExactly("B 优选供应商", "A 高价供应商");
        assertThat(quotes.get(0).unitPrice()).isEqualByComparingTo("820.00");
        assertThat(quotes.get(0).recommendScore()).isGreaterThan(quotes.get(1).recommendScore());
    }

    @Test
    void createPurchaseRfqGeocodesDeliveryAddressWhenCoordinatesAreBlank() {
        BusinessDemoService service = service();
        when(purchaserProfileMapper.selectOne(any())).thenReturn(purchaserProfile());
        when(geocodingService.resolve("北京交通大学"))
                .thenReturn(Optional.of(new Coordinates(new BigDecimal("116.348000"), new BigDecimal("39.952000"))));

        service.createPurchaseRfq(1L, new PurchaseRfqRequest(
                "瓶装饮用水",
                "食品饮水",
                "箱",
                "80",
                "北京交通大学",
                null,
                null,
                ""
        ));

        ArgumentCaptor<PurchaseRfq> rfqCaptor = ArgumentCaptor.forClass(PurchaseRfq.class);
        verify(purchaseRfqMapper).insert(rfqCaptor.capture());
        assertThat(rfqCaptor.getValue().getDeliveryAddress()).isEqualTo("北京交通大学");
        assertThat(rfqCaptor.getValue().getLongitude()).isEqualByComparingTo("116.348000");
        assertThat(rfqCaptor.getValue().getLatitude()).isEqualByComparingTo("39.952000");
    }

    @Test
    void acceptingRfqQuoteCreatesOrderAndMarksQuoteSelected() {
        BusinessDemoService service = service();
        PurchaseRfq rfq = rfq();
        PurchaseRfqQuote quote = quote(1001L, 1L, 11L, "820.00", 3);
        SupplierMaterial supplierMaterial = supplierMaterial();
        supplierMaterial.setId(11L);
        supplierMaterial.setSupplierId(1L);
        Material material = material();
        material.setUnit("箱");
        PurchaserProfile purchaser = purchaserProfile();
        SupplierProfile supplier = supplierProfile();
        when(purchaseRfqQuoteMapper.selectById(1001L)).thenReturn(quote);
        when(purchaseRfqMapper.selectById(10L)).thenReturn(rfq);
        when(supplierMaterialMapper.selectById(11L)).thenReturn(supplierMaterial);
        when(materialMapper.selectById(101L)).thenReturn(material);
        when(purchaserProfileMapper.selectOne(any())).thenReturn(purchaser);
        when(supplierProfileMapper.selectOne(any())).thenReturn(supplier);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        var order = service.acceptRfqQuote(1L, 1001L);

        assertThat(order.supplierId()).isEqualTo(1L);
        assertThat(order.materialId()).isEqualTo(101L);
        assertThat(order.quantity()).isEqualTo("80 箱");
        assertThat(order.source()).contains("询价");
        verify(purchaseRfqQuoteMapper).updateById(any(PurchaseRfqQuote.class));
        verify(purchaseRfqMapper).updateById(any(PurchaseRfq.class));
        verify(rabbitTemplate).convertAndSend(
                eq(OrderRabbitConfig.ORDER_EXCHANGE),
                eq(OrderRabbitConfig.ORDER_CREATED_ROUTING_KEY),
                eq(order.id())
        );
    }

    @Test
    void supplierQualificationUpdateMarksProfilePendingAndShowsAuditRisks() {
        BusinessDemoService service = service();
        SupplierProfile supplier = supplierProfile();
        SupplierAccount account = new SupplierAccount();
        account.setId(1L);
        account.setStatus(1);
        when(supplierProfileMapper.selectOne(any())).thenReturn(supplier);
        when(supplierProfileMapper.selectList(any())).thenReturn(List.of(supplier));
        when(supplierAccountMapper.selectById(1L)).thenReturn(account);
        when(supplierMaterialMapper.selectList(any())).thenReturn(List.of());

        var view = service.updateSupplierQualification(1L, new SupplierQualificationRequest(
                "上海可靠应急供应链有限公司",
                "张经理",
                "13800000001",
                "LIC-UPDATED-0001",
                "上海市浦东新区临港物资园",
                new BigDecimal("121.510000"),
                new BigDecimal("31.230000"),
                "https://files.example.com/license.pdf",
                "",
                "https://files.example.com/insurance.pdf"
        ));

        assertThat(view.auditStatus()).isEqualTo("PENDING");
        assertThat(view.auditStatusText()).isEqualTo("待复核");
        assertThat(view.qualificationCompletion()).isEqualTo(86);
        assertThat(view.riskTags()).contains("缺少安全生产证明", "暂无上架物资");
        assertThat(view.auditRemark()).contains("待管理员复核");
        verify(supplierProfileMapper).updateById(any(SupplierProfile.class));
        verify(geocodingService, never()).resolve(any());

        var auditView = service.adminSuppliers().get(0);
        assertThat(auditView.auditStatus()).isEqualTo("待复核");
        assertThat(auditView.qualificationCompletion()).isEqualTo(86);
        assertThat(auditView.riskTags()).contains("缺少安全生产证明", "暂无上架物资");
    }

    @Test
    void purchaserAcceptsCompletedOrderAndOrderViewShowsReceipt() {
        BusinessDemoService service = service();
        PurchaseOrder order = completedOrder();
        when(purchaseOrderMapper.selectById("PO-ACCEPT-001")).thenReturn(order);
        when(orderAcceptanceMapper.selectOne(any())).thenReturn(null).thenAnswer(invocation -> {
            OrderAcceptance acceptance = new OrderAcceptance();
            acceptance.setOrderId("PO-ACCEPT-001");
            acceptance.setPurchaserId(1L);
            acceptance.setSignerName("王主管");
            acceptance.setAcceptanceResult("ACCEPTED");
            acceptance.setProofUrl("https://files.example.com/pod.pdf");
            acceptance.setRemark("数量和外观验收通过");
            acceptance.setCreateTime(java.time.LocalDateTime.now());
            return acceptance;
        });

        var view = service.acceptPurchaseOrder(1L, "PO-ACCEPT-001", new OrderAcceptanceRequest(
                "王主管",
                "ACCEPTED",
                "https://files.example.com/pod.pdf",
                "数量和外观验收通过"
        ));

        verify(orderAcceptanceMapper).insert(any(OrderAcceptance.class));
        ArgumentCaptor<OrderPayment> paymentCaptor = ArgumentCaptor.forClass(OrderPayment.class);
        verify(orderPaymentMapper).insert(paymentCaptor.capture());
        verify(purchaseOrderMapper).updateById(any(PurchaseOrder.class));
        verify(orderTimelineMapper).insert(any(com.material.auth.entity.OrderTimeline.class));
        assertThat(view.acceptanceStatus()).isEqualTo("已验收");
        assertThat(view.acceptanceSummary()).contains("王主管", "数量和外观验收通过");
        assertThat(view.acceptanceProofUrl()).isEqualTo("https://files.example.com/pod.pdf");
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(paymentCaptor.getValue().getRemark()).contains("1小时");
        assertThat(paymentCaptor.getValue().getExpiresAt()).isAfter(paymentCaptor.getValue().getCreateTime().plusMinutes(59));
        assertThat(paymentCaptor.getValue().getExpiresAt()).isBeforeOrEqualTo(paymentCaptor.getValue().getCreateTime().plusHours(1));
    }

    @Test
    void purchaserPaysAcceptedOrderAndOrderViewShowsPaymentReceipt() {
        BusinessDemoService service = service();
        PurchaseOrder order = completedOrder();
        OrderPayment pendingPayment = pendingPayment(java.time.LocalDateTime.now().plusMinutes(30));
        when(purchaseOrderMapper.selectById("PO-ACCEPT-001")).thenReturn(order);
        when(orderAcceptanceMapper.selectOne(any())).thenReturn(acceptedReceipt());
        when(orderPaymentMapper.selectOne(any())).thenReturn(pendingPayment);

        var view = service.payPurchaseOrder(1L, "PO-ACCEPT-001", new OrderPaymentRequest(
                new BigDecimal("3184.00"),
                "BANK_TRANSFER",
                "BANK-20260608-001",
                "https://files.example.com/payment.pdf",
                "对公转账已完成"
        ));

        verify(orderPaymentMapper).updateById(any(OrderPayment.class));
        verify(orderPaymentMapper, never()).insert(any(OrderPayment.class));
        verify(purchaseOrderMapper).updateById(any(PurchaseOrder.class));
        verify(orderTimelineMapper).insert(any(com.material.auth.entity.OrderTimeline.class));
        assertThat(view.paymentStatus()).isEqualTo("已付款");
        assertThat(view.paymentSummary()).contains("¥3184.00", "对公转账", "BANK-20260608-001");
        assertThat(view.paymentProofUrl()).isEqualTo("https://files.example.com/payment.pdf");
    }

    @Test
    void purchaserPaymentRequiresAcceptedOrder() {
        BusinessDemoService service = service();
        PurchaseOrder order = completedOrder();
        when(purchaseOrderMapper.selectById("PO-ACCEPT-001")).thenReturn(order);
        when(orderAcceptanceMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.payPurchaseOrder(1L, "PO-ACCEPT-001", new OrderPaymentRequest(
                new BigDecimal("3184.00"),
                "BANK_TRANSFER",
                "BANK-20260608-001",
                "",
                ""
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("订单验收后才能付款");

        verify(orderPaymentMapper, never()).insert(any(OrderPayment.class));
    }

    @Test
    void scheduledJobMarksPendingPaymentTimeoutAfterOneHour() {
        BusinessDemoService service = service();
        OrderPayment expired = pendingPayment(java.time.LocalDateTime.now().minusMinutes(1));
        when(orderPaymentMapper.selectList(any())).thenReturn(List.of(expired));

        int updated = service.expireOverduePayments();

        assertThat(updated).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo("TIMEOUT");
        assertThat(expired.getRemark()).contains("支付超时");
        verify(orderPaymentMapper).updateById(expired);
    }

    /**
     * 作用：完成 service 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：返回 BusinessDemoService，也就是这个方法处理后的结果。
     */
    private BusinessDemoService service() {
        return new BusinessDemoService(
                supplierProfileMapper,
                supplierAccountMapper,
                supplierMaterialMapper,
                materialMapper,
                driverProfileMapper,
                purchaserProfileMapper,
                purchaseOrderMapper,
                purchaseRfqMapper,
                purchaseRfqQuoteMapper,
                driverFollowMapper,
                orderPushRecordMapper,
                orderAcceptanceMapper,
                orderPaymentMapper,
                orderReviewMapper,
                orderTimelineMapper,
                rabbitTemplate,
                amqpAdmin,
                redisTemplate,
                objectMapper,
                geocodingService
        );
    }

    private PurchaseRfq rfq() {
        PurchaseRfq rfq = new PurchaseRfq();
        rfq.setId(10L);
        rfq.setPurchaserId(1L);
        rfq.setMaterialName("瓶装饮用水");
        rfq.setCategory("食品饮水");
        rfq.setUnit("箱");
        rfq.setQuantity("80 箱");
        rfq.setDeliveryAddress("上海市浦东新区应急仓");
        rfq.setLongitude(new BigDecimal("121.470000"));
        rfq.setLatitude(new BigDecimal("31.230000"));
        rfq.setStatus("OPEN");
        return rfq;
    }

    private PurchaseRfqQuote quote(Long id, Long supplierId, Long supplierMaterialId, String unitPrice, Integer deliveryDays) {
        PurchaseRfqQuote quote = new PurchaseRfqQuote();
        quote.setId(id);
        quote.setRfqId(10L);
        quote.setSupplierId(supplierId);
        quote.setSupplierMaterialId(supplierMaterialId);
        quote.setUnitPrice(new BigDecimal(unitPrice));
        quote.setAvailableQuantity(200);
        quote.setDeliveryDays(deliveryDays);
        quote.setRemark("可按期配送");
        quote.setStatus("ACTIVE");
        return quote;
    }

    private PurchaseOrder completedOrder() {
        PurchaseOrder order = new PurchaseOrder();
        order.setId("PO-ACCEPT-001");
        order.setPurchaserId(1L);
        order.setPurchaserName("Shanghai Material Purchaser Co., Ltd.");
        order.setSupplierId(1L);
        order.setSupplierName("Shanghai Reliable Supplier Co., Ltd.");
        order.setMaterialId(101L);
        order.setMaterialName("瓶装饮用水");
        order.setCategory("食品饮水");
        order.setQuantity("80 箱");
        order.setAmount("3184.00");
        order.setStatus("已完成");
        order.setSource("询价报价已采纳");
        order.setPushedTo("订单已完成，等待采购方验收");
        order.setDriverId(8L);
        order.setCreateTime(java.time.LocalDateTime.now());
        return order;
    }

    private OrderAcceptance acceptedReceipt() {
        OrderAcceptance acceptance = new OrderAcceptance();
        acceptance.setOrderId("PO-ACCEPT-001");
        acceptance.setPurchaserId(1L);
        acceptance.setSignerName("王主管");
        acceptance.setAcceptanceResult("ACCEPTED");
        acceptance.setProofUrl("https://files.example.com/pod.pdf");
        acceptance.setRemark("数量和外观验收通过");
        acceptance.setCreateTime(java.time.LocalDateTime.now());
        return acceptance;
    }

    private OrderPayment pendingPayment(java.time.LocalDateTime expiresAt) {
        OrderPayment payment = new OrderPayment();
        payment.setOrderId("PO-ACCEPT-001");
        payment.setPurchaserId(1L);
        payment.setAmount(new BigDecimal("3184.00"));
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setPaymentReference("WAITING-PO-ACCEPT-001");
        payment.setStatus("PENDING");
        payment.setRemark("验收完成，请在1小时内完成付款");
        payment.setExpiresAt(expiresAt);
        payment.setCreateTime(expiresAt.minusHours(1));
        payment.setUpdateTime(payment.getCreateTime());
        return payment;
    }

    /**
     * 作用：完成 supplierProfile 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：返回 SupplierProfile，也就是这个方法处理后的结果。
     */
    private SupplierProfile supplierProfile() {
        SupplierProfile profile = new SupplierProfile();
        profile.setSupplierId(1L);
        profile.setCompanyName("Shanghai Reliable Supplier Co., Ltd.");
        profile.setContactName("张经理");
        profile.setAddress("上海市浦东新区临港物资园");
        profile.setRatingScore(new BigDecimal("96.8"));
        return profile;
    }

    /**
     * 作用：完成 supplierMaterial 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：返回 SupplierMaterial，也就是这个方法处理后的结果。
     */
    private SupplierMaterial supplierMaterial() {
        SupplierMaterial supplierMaterial = new SupplierMaterial();
        supplierMaterial.setSupplierId(1L);
        supplierMaterial.setMaterialId(101L);
        supplierMaterial.setSupplyPrice(new BigDecimal("3020.00"));
        supplierMaterial.setStockQuantity(420);
        supplierMaterial.setDailyCapacity(120);
        return supplierMaterial;
    }

    /**
     * 作用：完成 material 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Material，也就是这个方法处理后的结果。
     */
    private Material material() {
        Material material = new Material();
        material.setId(101L);
        material.setMaterialName("HRB400E 抗震钢筋");
        material.setCategory("钢材");
        material.setUnit("吨");
        return material;
    }

    /**
     * 作用：完成 purchaserProfile 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：返回 PurchaserProfile，也就是这个方法处理后的结果。
     */
    private PurchaserProfile purchaserProfile() {
        PurchaserProfile profile = new PurchaserProfile();
        profile.setPurchaserId(1L);
        profile.setCompanyName("Shanghai Material Purchaser Co., Ltd.");
        return profile;
    }
}
