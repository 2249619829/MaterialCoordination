package com.material.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.material.auth.dto.business.AdminDashboardView;
import com.material.auth.dto.business.AdminSupplierAuditView;
import com.material.auth.dto.business.DeadLetterStatsView;
import com.material.auth.dto.business.DriverAttendanceView;
import com.material.auth.dto.business.DriverFollowView;
import com.material.auth.dto.business.MaterialOptionView;
import com.material.auth.dto.business.NearbySupplierView;
import com.material.auth.dto.business.NotificationView;
import com.material.auth.dto.business.OrderReviewRequest;
import com.material.auth.dto.business.OrderReviewView;
import com.material.auth.dto.business.OrderTimelineView;
import com.material.auth.dto.business.PurchaseCartCheckoutRequest;
import com.material.auth.dto.business.PurchaseCartItemRequest;
import com.material.auth.dto.business.PurchaseOrderRequest;
import com.material.auth.dto.business.PurchaseOrderView;
import com.material.auth.dto.business.SupplierCatalogView;
import com.material.auth.dto.business.SupplierMaterialManageRequest;
import com.material.auth.dto.business.SupplierMaterialManageView;
import com.material.auth.dto.business.SupplierMaterialView;
import com.material.auth.dto.business.SupplierRankingView;
import com.material.auth.dto.business.SupplierStoreView;
import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.entity.DriverFollow;
import com.material.auth.entity.DriverProfile;
import com.material.auth.entity.Material;
import com.material.auth.entity.OrderPushRecord;
import com.material.auth.entity.OrderReview;
import com.material.auth.entity.OrderTimeline;
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
import com.material.auth.entity.SupplierAccount;
import com.material.common.enums.AccountStatus;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BusinessDemoService {
    private static final Logger log = LoggerFactory.getLogger(BusinessDemoService.class);
    private static final String ORDER_WAITING_SUPPLIER_CONFIRM = "待供应商确认";
    private static final String ORDER_WAITING_DRIVER = "待司机接单";
    private static final String ORDER_CLAIMED = "司机已接单";
    private static final String ORDER_TRANSPORTING = "运输中";
    private static final String ORDER_COMPLETED = "已完成";
    private static final String ORDER_SUPPLIER_REJECTED = "供应商已拒单";
    private static final String ORDER_PANIC_BUYING = "待抢购";
    private static final String ORDER_PURCHASER_CLAIMED = "采购方已抢购";
    private static final String DRIVER_FOLLOW_PURCHASER = "DRIVER_FOLLOW_PURCHASER";
    private static final String PURCHASER_FOLLOW_DRIVER = "PURCHASER_FOLLOW_DRIVER";
    public static final String PENDING_ORDER_KEY_PREFIX = "order:pending:";
    private static final String SUPPLIER_CATALOG_CACHE_KEY = "cache:supplier:catalog:v1";
    private static final String EMPTY_CACHE_VALUE = "[]";
    private static final String SUPPLIER_RANKING_KEY = "ranking:supplier:fulfillment";
    private static final String SUPPLIER_GEO_KEY = "geo:supplier";
    private static final String DRIVER_ATTENDANCE_KEY_PREFIX = "attendance:driver:";
    private static final String TARGET_SUPPLIER = "SUPPLIER";
    private static final String TARGET_PURCHASER = "PURCHASER";
    private static final String TARGET_DRIVER = "DRIVER";
    private static final String TARGET_ADMIN = "ADMIN";
    private static final String PANIC_STOCK_KEY_PREFIX = "panic:stock:";
    private static final String PANIC_BUYER_KEY_PREFIX = "panic:buyer:";
    private static final String PUSH_STATUS_READ = "READ";
    private static final String PUSH_STATUS_CLAIMED = "CLAIMED";
    private static final Duration PENDING_ORDER_TTL = Duration.ofMinutes(30);
    private static final Duration EMPTY_CATALOG_TTL = Duration.ofSeconds(60);
    private static final DefaultRedisScript<Long> PANIC_BUY_SCRIPT = new DefaultRedisScript<>("""
            local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
            if stock <= 0 then
                return 1
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 2
            end
            redis.call('DECR', KEYS[1])
            redis.call('SET', KEYS[2], ARGV[1], 'EX', ARGV[2])
            return 0
            """, Long.class);
    private static final DateTimeFormatter ORDER_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter VIEW_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SupplierProfileMapper supplierProfileMapper;
    private final SupplierAccountMapper supplierAccountMapper;
    private final SupplierMaterialMapper supplierMaterialMapper;
    private final MaterialMapper materialMapper;
    private final DriverProfileMapper driverProfileMapper;
    private final PurchaserProfileMapper purchaserProfileMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final DriverFollowMapper driverFollowMapper;
    private final OrderPushRecordMapper orderPushRecordMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final OrderTimelineMapper orderTimelineMapper;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicInteger orderSequence = new AtomicInteger(1001);

    public BusinessDemoService(SupplierProfileMapper supplierProfileMapper,
                               SupplierAccountMapper supplierAccountMapper,
                               SupplierMaterialMapper supplierMaterialMapper,
                               MaterialMapper materialMapper,
                               DriverProfileMapper driverProfileMapper,
                               PurchaserProfileMapper purchaserProfileMapper,
                               PurchaseOrderMapper purchaseOrderMapper,
                               DriverFollowMapper driverFollowMapper,
                               OrderPushRecordMapper orderPushRecordMapper,
                               OrderReviewMapper orderReviewMapper,
                               OrderTimelineMapper orderTimelineMapper,
                               RabbitTemplate rabbitTemplate,
                               AmqpAdmin amqpAdmin,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper) {
        this.supplierProfileMapper = supplierProfileMapper;
        this.supplierAccountMapper = supplierAccountMapper;
        this.supplierMaterialMapper = supplierMaterialMapper;
        this.materialMapper = materialMapper;
        this.driverProfileMapper = driverProfileMapper;
        this.purchaserProfileMapper = purchaserProfileMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.driverFollowMapper = driverFollowMapper;
        this.orderPushRecordMapper = orderPushRecordMapper;
        this.orderReviewMapper = orderReviewMapper;
        this.orderTimelineMapper = orderTimelineMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SupplierCatalogView> supplierCatalog() {
        List<SupplierCatalogView> cachedCatalog = readSupplierCatalogCache();
        if (cachedCatalog != null) {
            return cachedCatalog;
        }
        List<SupplierCatalogView> catalog = supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .orderByDesc(SupplierProfile::getRatingScore))
                .stream()
                .map(this::toSupplierCatalogView)
                .toList();
        writeSupplierCatalogCache(catalog);
        return catalog;
    }

    public SupplierStoreView supplierStore(Long supplierId) {
        SupplierProfile supplier = findSupplier(supplierId);
        SupplierCatalogView catalogView = toSupplierCatalogView(supplier);
        List<PurchaseOrder> recentOrders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getSupplierId, supplierId)
                .orderByDesc(PurchaseOrder::getCreateTime)
                .last("LIMIT 5"));
        List<OrderReview> recentReviews = orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getTargetType, TARGET_SUPPLIER)
                .eq(OrderReview::getTargetId, supplierId)
                .orderByDesc(OrderReview::getCreateTime)
                .last("LIMIT 5"));
        Long totalOrders = purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getSupplierId, supplierId));
        return new SupplierStoreView(
                catalogView,
                recentOrders.stream().map(this::toPurchaseOrderView).toList(),
                recentReviews.stream().map(this::toOrderReviewView).toList(),
                totalOrders.intValue(),
                catalogView.materials().size(),
                "可供 " + catalogView.materials().size() + " 类物资，平台履约评分 " + catalogView.rating() + " 分"
        );
    }

    public List<MaterialOptionView> materialOptions() {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .eq(Material::getStatus, 1)
                        .orderByAsc(Material::getCategory)
                        .orderByAsc(Material::getMaterialName))
                .stream()
                .map(material -> new MaterialOptionView(
                        material.getId(),
                        material.getMaterialCode(),
                        material.getMaterialName(),
                        material.getCategory(),
                        material.getUnit()
                ))
                .toList();
    }

    public List<SupplierMaterialManageView> supplierMaterials(Long supplierId) {
        return supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                        .eq(SupplierMaterial::getSupplierId, supplierId)
                        .orderByDesc(SupplierMaterial::getStatus)
                        .orderByDesc(SupplierMaterial::getUpdateTime))
                .stream()
                .map(this::toSupplierMaterialManageView)
                .toList();
    }

    public SupplierMaterialManageView saveSupplierMaterial(Long supplierId, SupplierMaterialManageRequest request) {
        if (request.materialId() == null) {
            throw new IllegalArgumentException("请选择物资");
        }
        Material material = findMaterial(request.materialId());
        SupplierMaterial supplierMaterial = supplierMaterialMapper.selectOne(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplierId)
                .eq(SupplierMaterial::getMaterialId, material.getId()));
        LocalDateTime now = LocalDateTime.now();
        if (supplierMaterial == null) {
            supplierMaterial = new SupplierMaterial();
            supplierMaterial.setSupplierId(supplierId);
            supplierMaterial.setMaterialId(material.getId());
            supplierMaterial.setCreateTime(now);
        }
        supplierMaterial.setSupplyPrice(nonNegativeMoney(request.supplyPrice(), "供货价格"));
        supplierMaterial.setStockQuantity(nonNegativeInt(request.stockQuantity(), "库存"));
        supplierMaterial.setDailyCapacity(nonNegativeInt(request.dailyCapacity(), "日产能"));
        supplierMaterial.setDeliveryRadiusKm(nonNegativeMoney(request.deliveryRadiusKm(), "配送半径"));
        supplierMaterial.setStatus(request.status() == null ? 1 : request.status());
        supplierMaterial.setUpdateTime(now);
        if (supplierMaterial.getId() == null) {
            supplierMaterialMapper.insert(supplierMaterial);
        } else {
            supplierMaterialMapper.updateById(supplierMaterial);
        }
        invalidateSupplierCatalogCacheWithDelay();
        return toSupplierMaterialManageView(supplierMaterial);
    }

    public SupplierMaterialManageView updateSupplierMaterial(Long supplierId, Long supplierMaterialId, SupplierMaterialManageRequest request) {
        SupplierMaterial supplierMaterial = findOwnedSupplierMaterial(supplierId, supplierMaterialId);
        supplierMaterial.setSupplyPrice(nonNegativeMoney(request.supplyPrice(), "供货价格"));
        supplierMaterial.setStockQuantity(nonNegativeInt(request.stockQuantity(), "库存"));
        supplierMaterial.setDailyCapacity(nonNegativeInt(request.dailyCapacity(), "日产能"));
        supplierMaterial.setDeliveryRadiusKm(nonNegativeMoney(request.deliveryRadiusKm(), "配送半径"));
        supplierMaterial.setStatus(request.status() == null ? supplierMaterial.getStatus() : request.status());
        supplierMaterial.setUpdateTime(LocalDateTime.now());
        supplierMaterialMapper.updateById(supplierMaterial);
        invalidateSupplierCatalogCacheWithDelay();
        return toSupplierMaterialManageView(supplierMaterial);
    }

    public SupplierMaterialManageView offlineSupplierMaterial(Long supplierId, Long supplierMaterialId) {
        SupplierMaterial supplierMaterial = findOwnedSupplierMaterial(supplierId, supplierMaterialId);
        supplierMaterial.setStatus(0);
        supplierMaterial.setUpdateTime(LocalDateTime.now());
        supplierMaterialMapper.updateById(supplierMaterial);
        invalidateSupplierCatalogCacheWithDelay();
        return toSupplierMaterialManageView(supplierMaterial);
    }

    public OrderReviewView createOrderReview(Long reviewerId, String reviewerType, String orderId, OrderReviewRequest request) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        assertOrderParticipant(order, reviewerId, reviewerType);
        String targetType = normalizeTargetType(request.targetType());
        Long targetId = request.targetId();
        validateReviewTarget(order, targetType, targetId);
        Integer score = request.score();
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException("评分必须在 1 到 5 分之间");
        }
        if (orderReviewMapper.selectCount(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getOrderId, orderId)
                .eq(OrderReview::getReviewerType, reviewerType)
                .eq(OrderReview::getReviewerId, reviewerId)
                .eq(OrderReview::getTargetType, targetType)
                .eq(OrderReview::getTargetId, targetId)) > 0) {
            throw new IllegalStateException("该订单已评价过这个对象");
        }
        LocalDateTime now = LocalDateTime.now();
        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setReviewerType(reviewerType);
        review.setReviewerId(reviewerId);
        review.setTargetType(targetType);
        review.setTargetId(targetId);
        review.setScore(score);
        review.setContent(StringUtils.hasText(request.content()) ? request.content().trim() : "履约评价正常");
        review.setCreateTime(now);
        review.setUpdateTime(now);
        orderReviewMapper.insert(review);
        addTimeline(orderId, order.getStatus(), "提交履约评价", reviewerType, reviewerId,
                "评价 " + targetType + "，评分 " + score + " 分");
        if (TARGET_SUPPLIER.equals(targetType)) {
            refreshSupplierRating(targetId);
        }
        return toOrderReviewView(review);
    }

    public List<OrderReviewView> orderReviews(Long userId, String userType, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        assertOrderVisibleToUser(order, userId, userType);
        return orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                        .eq(OrderReview::getOrderId, orderId)
                        .orderByDesc(OrderReview::getCreateTime))
                .stream()
                .map(this::toOrderReviewView)
                .toList();
    }

    public List<OrderTimelineView> orderTimeline(Long userId, String userType, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        assertOrderVisibleToUser(order, userId, userType);
        return orderTimelineMapper.selectList(new LambdaQueryWrapper<OrderTimeline>()
                        .eq(OrderTimeline::getOrderId, orderId)
                        .orderByAsc(OrderTimeline::getCreateTime))
                .stream()
                .map(this::toOrderTimelineView)
                .toList();
    }

    public List<SupplierRankingView> supplierRanking() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(SUPPLIER_RANKING_KEY, 0, 9);
        if (tuples == null || tuples.isEmpty()) {
            return rebuildSupplierRankingFromDb();
        }
        List<Long> supplierIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .map(Long::valueOf)
                .toList();
        Map<Long, SupplierProfile> profiles = supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .in(SupplierProfile::getSupplierId, supplierIds))
                .stream()
                .collect(Collectors.toMap(SupplierProfile::getSupplierId, Function.identity()));
        List<SupplierRankingView> ranking = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long supplierId = Long.valueOf(tuple.getValue());
            SupplierProfile profile = profiles.get(supplierId);
            if (profile != null) {
                ranking.add(toSupplierRankingView(profile, rank++));
            }
        }
        return ranking;
    }

    public List<NearbySupplierView> nearbySuppliers(Double longitude, Double latitude, Double radiusKm) {
        if (longitude == null || latitude == null) {
            throw new IllegalArgumentException("经纬度不能为空");
        }
        rebuildSupplierGeoFromDb();
        double radius = radiusKm == null ? 50D : Math.max(radiusKm, 1D);
        var results = redisTemplate.opsForGeo().radius(
                SUPPLIER_GEO_KEY,
                new Circle(new Point(longitude, latitude), new Distance(radius, Metrics.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending().limit(10)
        );
        if (results == null) {
            return List.of();
        }
        List<Long> supplierIds = results.getContent().stream()
                .map(result -> Long.valueOf(result.getContent().getName()))
                .toList();
        if (supplierIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SupplierProfile> profiles = supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .in(SupplierProfile::getSupplierId, supplierIds))
                .stream()
                .collect(Collectors.toMap(SupplierProfile::getSupplierId, Function.identity()));
        return results.getContent().stream()
                .map(result -> {
                    Long supplierId = Long.valueOf(result.getContent().getName());
                    SupplierProfile profile = profiles.get(supplierId);
                    if (profile == null) {
                        return null;
                    }
                    return new NearbySupplierView(
                            supplierId,
                            profile.getCompanyName(),
                            profile.getAddress(),
                            profile.getRatingScore().stripTrailingZeros().toPlainString(),
                            result.getDistance().getValue()
                    );
                })
                .filter(item -> item != null)
                .toList();
    }

    public DriverAttendanceView markDriverAttendance(Long driverId, boolean online) {
        DriverProfile driver = findDriver(driverId);
        redisTemplate.opsForValue().setBit(driverAttendanceKey(LocalDate.now()), driverId, online);
        driver.setAttendanceStatus(online ? 1 : 0);
        driver.setUpdateTime(LocalDateTime.now());
        driverProfileMapper.updateById(driver);
        return new DriverAttendanceView(driverId, LocalDate.now().toString(), online);
    }

    public DriverAttendanceView todayDriverAttendance(Long driverId) {
        Boolean online = redisTemplate.opsForValue().getBit(driverAttendanceKey(LocalDate.now()), driverId);
        if (online == null) {
            DriverProfile driver = findDriver(driverId);
            online = Integer.valueOf(1).equals(driver.getAttendanceStatus());
        }
        return new DriverAttendanceView(driverId, LocalDate.now().toString(), Boolean.TRUE.equals(online));
    }

    public PurchaseOrderView markPushRead(Long driverId, String orderId) {
        OrderPushRecord record = findDriverPushRecord(driverId, orderId);
        if (!PUSH_STATUS_CLAIMED.equals(record.getStatus())) {
            record.setStatus(PUSH_STATUS_READ);
            record.setUpdateTime(LocalDateTime.now());
            orderPushRecordMapper.updateById(record);
        }
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return toPurchaseOrderView(order, record.getStatus());
    }

    public Integer retryOrderPushRecords() {
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getStatus, ORDER_WAITING_DRIVER));
        int createdCount = 0;
        for (PurchaseOrder order : orders) {
            Set<Long> driverIds = relatedDriverIds(order.getPurchaserId());
            for (Long driverId : driverIds) {
                if (createPushRecord(order, driverId)) {
                    createdCount++;
                }
            }
        }
        return createdCount;
    }

    public List<DeadLetterStatsView> deadLetterStats() {
        return List.of(
                queueStats(OrderRabbitConfig.ORDER_CREATED_DEAD_LETTER_QUEUE),
                queueStats(OrderRabbitConfig.ORDER_CLAIMED_DEAD_LETTER_QUEUE)
        );
    }

    public AdminDashboardView adminDashboard() {
        long deadLetterCount = deadLetterStats().stream()
                .mapToLong(DeadLetterStatsView::messages)
                .sum();
        return new AdminDashboardView(
                supplierProfileMapper.selectCount(null),
                purchaserProfileMapper.selectCount(null),
                driverProfileMapper.selectCount(null),
                purchaseOrderMapper.selectCount(null),
                orderCountByStatus(ORDER_WAITING_SUPPLIER_CONFIRM),
                orderCountByStatus(ORDER_WAITING_DRIVER),
                orderCountByStatus(ORDER_TRANSPORTING),
                orderCountByStatus(ORDER_COMPLETED),
                orderCountByStatus(ORDER_SUPPLIER_REJECTED) + deadLetterCount,
                orderPushRecordMapper.selectCount(new LambdaQueryWrapper<OrderPushRecord>()
                        .eq(OrderPushRecord::getStatus, "PENDING")),
                deadLetterCount
        );
    }

    public List<AdminSupplierAuditView> adminSuppliers() {
        return supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .orderByAsc(SupplierProfile::getSupplierId))
                .stream()
                .map(this::toAdminSupplierAuditView)
                .toList();
    }

    public AdminSupplierAuditView approveSupplier(Long supplierId) {
        SupplierAccount account = findSupplierAccount(supplierId);
        account.setStatus(AccountStatus.ENABLED.getCode());
        account.setUpdateTime(LocalDateTime.now());
        supplierAccountMapper.updateById(account);
        invalidateSupplierCatalogCacheWithDelay();
        log.info("business_event event=supplier_approved supplierId={}", supplierId);
        return toAdminSupplierAuditView(findSupplier(supplierId));
    }

    public AdminSupplierAuditView rejectSupplier(Long supplierId) {
        SupplierAccount account = findSupplierAccount(supplierId);
        account.setStatus(AccountStatus.DISABLED.getCode());
        account.setUpdateTime(LocalDateTime.now());
        supplierAccountMapper.updateById(account);
        invalidateSupplierCatalogCacheWithDelay();
        log.info("business_event event=supplier_rejected supplierId={}", supplierId);
        return toAdminSupplierAuditView(findSupplier(supplierId));
    }

    public List<PurchaseOrderView> adminOrders() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .orderByDesc(PurchaseOrder::getUpdateTime)
                        .last("LIMIT 50"))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    private List<SupplierCatalogView> readSupplierCatalogCache() {
        String cacheValue = redisTemplate.opsForValue().get(SUPPLIER_CATALOG_CACHE_KEY);
        if (!StringUtils.hasText(cacheValue)) {
            return null;
        }
        try {
            return objectMapper.readValue(cacheValue, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY);
            return null;
        }
    }

    private void writeSupplierCatalogCache(List<SupplierCatalogView> catalog) {
        try {
            String cacheValue = catalog.isEmpty() ? EMPTY_CACHE_VALUE : objectMapper.writeValueAsString(catalog);
            Duration ttl = catalog.isEmpty() ? EMPTY_CATALOG_TTL : supplierCatalogTtl();
            redisTemplate.opsForValue().set(SUPPLIER_CATALOG_CACHE_KEY, cacheValue, ttl);
        } catch (JsonProcessingException ignored) {
            redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY);
        }
    }

    private Duration supplierCatalogTtl() {
        return Duration.ofMinutes(5).plusSeconds(ThreadLocalRandom.current().nextInt(0, 180));
    }

    public List<PurchaseOrderView> purchaserOrders(Long purchaserId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getPurchaserId, purchaserId)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    public List<PurchaseOrderView> supplierOrders(Long supplierId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getSupplierId, supplierId)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    public List<PurchaseOrderView> transportHall() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, ORDER_WAITING_DRIVER)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    public List<PurchaseOrderView> panicBuyHall() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, ORDER_PANIC_BUYING)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    public List<PurchaseOrderView> driverPushOrders(Long driverId) {
        List<OrderPushRecord> pushRecords = orderPushRecordMapper.selectList(new LambdaQueryWrapper<OrderPushRecord>()
                .eq(OrderPushRecord::getDriverId, driverId)
                .orderByDesc(OrderPushRecord::getCreateTime));
        if (pushRecords.isEmpty()) {
            return List.of();
        }
        Map<String, String> pushStatusByOrderId = pushRecords.stream()
                .collect(Collectors.toMap(OrderPushRecord::getOrderId, OrderPushRecord::getStatus, (left, right) -> left));
        List<String> orderIds = new ArrayList<>(pushStatusByOrderId.keySet());
        return purchaseOrderMapper.selectBatchIds(orderIds)
                .stream()
                .filter(order -> ORDER_WAITING_DRIVER.equals(order.getStatus()))
                .sorted((left, right) -> right.getCreateTime().compareTo(left.getCreateTime()))
                .map(order -> toPurchaseOrderView(order, pushStatusByOrderId.get(order.getId())))
                .toList();
    }

    public List<PurchaseOrderView> driverOrders(Long driverId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getDriverId, driverId)
                        .in(PurchaseOrder::getStatus, ORDER_CLAIMED, ORDER_TRANSPORTING, ORDER_COMPLETED)
                        .orderByDesc(PurchaseOrder::getUpdateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    public List<DriverFollowView> driverFollows(Long driverId) {
        return purchaserProfileMapper.selectList(new LambdaQueryWrapper<PurchaserProfile>()
                        .orderByAsc(PurchaserProfile::getPurchaserId))
                .stream()
                .map(profile -> new DriverFollowView(
                        profile.getPurchaserId(),
                        profile.getCompanyName(),
                        existsFollow(driverId, profile.getPurchaserId(), DRIVER_FOLLOW_PURCHASER),
                        existsFollow(driverId, profile.getPurchaserId(), PURCHASER_FOLLOW_DRIVER)
                ))
                .toList();
    }

    public DriverFollowView followPurchaser(Long driverId, Long purchaserId) {
        if (!existsFollow(driverId, purchaserId, DRIVER_FOLLOW_PURCHASER)) {
            DriverFollow follow = new DriverFollow();
            follow.setDriverId(driverId);
            follow.setPurchaserId(purchaserId);
            follow.setFollowType(DRIVER_FOLLOW_PURCHASER);
            driverFollowMapper.insert(follow);
        }
        PurchaserProfile purchaser = findPurchaser(purchaserId);
        return new DriverFollowView(
                purchaserId,
                purchaser.getCompanyName(),
                true,
                existsFollow(driverId, purchaserId, PURCHASER_FOLLOW_DRIVER)
        );
    }

    public List<NotificationView> notifications(Long userId, String userType) {
        if (TARGET_PURCHASER.equals(userType)) {
            return purchaserNotifications(userId);
        }
        if (TARGET_SUPPLIER.equals(userType)) {
            return supplierNotifications(userId);
        }
        if (TARGET_DRIVER.equals(userType)) {
            return driverNotifications(userId);
        }
        if (TARGET_ADMIN.equals(userType)) {
            return adminNotifications();
        }
        return List.of();
    }

    public PurchaseOrderView createPurchaseOrder(Long purchaserId, Long supplierId, Long materialId) {
        return createPurchaseOrder(purchaserId, new PurchaseOrderRequest(supplierId, materialId, "100 吨", "线上沟通后确认"));
    }

    public PurchaseOrderView createPurchaseOrder(Long purchaserId, PurchaseOrderRequest request) {
        PurchaseOrder order = buildPurchaseOrder(purchaserId, request);
        publishPendingOrder(order);
        log.info("business_event event=order_created orderId={} purchaserId={} supplierId={} materialId={}",
                order.getId(), purchaserId, order.getSupplierId(), order.getMaterialId());
        return toPurchaseOrderView(order);
    }

    private PurchaseOrder buildPurchaseOrder(Long purchaserId, PurchaseOrderRequest request) {
        SupplierProfile supplier = findSupplier(request.supplierId());
        SupplierMaterial supplierMaterial = findSupplierMaterial(supplier.getSupplierId(), request.materialId());
        Material material = findMaterial(supplierMaterial.getMaterialId());
        PurchaserProfile purchaser = findPurchaser(purchaserId);
        String quantity = StringUtils.hasText(request.quantity()) ? request.quantity().trim() : "100 " + material.getUnit();
        PurchaseOrder order = new PurchaseOrder();
        order.setId(newOrderId());
        order.setPurchaserId(purchaserId);
        order.setPurchaserName(purchaser.getCompanyName());
        order.setSupplierId(supplier.getSupplierId());
        order.setSupplierName(supplier.getCompanyName());
        order.setMaterialId(material.getId());
        order.setMaterialName(material.getMaterialName());
        order.setCategory(material.getCategory());
        order.setQuantity(quantity);
        order.setAmount(estimateAmount(supplierMaterial.getSupplyPrice(), quantity));
        order.setStatus(ORDER_WAITING_SUPPLIER_CONFIRM);
        order.setSource("采购方提交采购清单，等待供应商确认");
        order.setPushedTo("供应商确认后推送给关注关系司机");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(order.getCreateTime());
        return order;
    }

    private void publishPendingOrder(PurchaseOrder order) {
        cachePendingOrder(order);
        rabbitTemplate.convertAndSend(
                OrderRabbitConfig.ORDER_EXCHANGE,
                OrderRabbitConfig.ORDER_CREATED_ROUTING_KEY,
                order.getId()
        );
        addTimeline(order.getId(), order.getStatus(), "采购方提交采购订单", TARGET_PURCHASER, order.getPurchaserId(),
                "订单已写入 Redis 临时订单并发送 RabbitMQ，等待异步落库");
    }

    public List<PurchaseOrderView> checkoutPurchaseCart(Long purchaserId, PurchaseCartCheckoutRequest request) {
        if (request.supplierId() == null) {
            throw new IllegalArgumentException("请选择供应商");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("采购清单不能为空");
        }
        if (request.items().size() > 20) {
            throw new IllegalArgumentException("单次采购清单不能超过 20 项");
        }
        List<PurchaseOrder> orders = new ArrayList<>();
        for (PurchaseCartItemRequest item : request.items()) {
            if (item.materialId() == null) {
                throw new IllegalArgumentException("采购清单存在未选择物资");
            }
            String quantity = StringUtils.hasText(item.quantity()) ? item.quantity().trim() : null;
            orders.add(buildPurchaseOrder(
                    purchaserId,
                    new PurchaseOrderRequest(
                            request.supplierId(),
                            item.materialId(),
                            quantity,
                            StringUtils.hasText(request.remark()) ? request.remark().trim() : "采购清单批量提交"
                    )
            ));
        }
        for (PurchaseOrder order : orders) {
            publishPendingOrder(order);
        }
        log.info("business_event event=cart_checkout purchaserId={} supplierId={} orderCount={}",
                purchaserId, request.supplierId(), orders.size());
        return orders.stream().map(this::toPurchaseOrderView).toList();
    }

    @Transactional
    public PurchaseOrderView confirmSupplierOrder(Long supplierId, String orderId) {
        PurchaseOrder order = findOrderForSupplier(supplierId, orderId);
        if (!ORDER_WAITING_SUPPLIER_CONFIRM.equals(order.getStatus())) {
            throw new IllegalStateException("订单当前状态不能确认供货");
        }
        int quantity = parseOrderQuantity(order.getQuantity());
        int stockRows = supplierMaterialMapper.update(null, new LambdaUpdateWrapper<SupplierMaterial>()
                .setSql("stock_quantity = stock_quantity - " + quantity)
                .set(SupplierMaterial::getUpdateTime, LocalDateTime.now())
                .eq(SupplierMaterial::getSupplierId, supplierId)
                .eq(SupplierMaterial::getMaterialId, order.getMaterialId())
                .eq(SupplierMaterial::getStatus, 1)
                .ge(SupplierMaterial::getStockQuantity, quantity));
        if (stockRows <= 0) {
            throw new IllegalStateException("库存不足，无法确认供货");
        }
        PurchaseOrder update = new PurchaseOrder();
        update.setStatus(ORDER_WAITING_DRIVER);
        update.setPushedTo("供应商已确认，订单进入运输大厅并推送给关注关系司机");
        update.setUpdateTime(LocalDateTime.now());
        int orderRows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getSupplierId, supplierId)
                .eq(PurchaseOrder::getStatus, ORDER_WAITING_SUPPLIER_CONFIRM));
        if (orderRows <= 0) {
            throw new IllegalStateException("订单状态已变化，请刷新后重试");
        }
        order.setStatus(ORDER_WAITING_DRIVER);
        order.setPushedTo(update.getPushedTo());
        order.setUpdateTime(update.getUpdateTime());
        Set<Long> driverIds = relatedDriverIds(order.getPurchaserId());
        for (Long driverId : driverIds) {
            createPushRecord(order, driverId);
        }
        addTimeline(orderId, ORDER_WAITING_DRIVER, "供应商确认供货并扣减库存", TARGET_SUPPLIER, supplierId,
                "扣减库存 " + quantity + " " + orderUnit(order.getQuantity()) + "，生成司机推送记录 " + driverIds.size() + " 条");
        invalidateSupplierCatalogCacheWithDelay();
        log.info("business_event event=supplier_confirmed_order orderId={} supplierId={} quantity={} pushCount={}",
                orderId, supplierId, quantity, driverIds.size());
        return toPurchaseOrderView(purchaseOrderMapper.selectById(orderId));
    }

    @Transactional
    public PurchaseOrderView rejectSupplierOrder(Long supplierId, String orderId) {
        PurchaseOrder order = findOrderForSupplier(supplierId, orderId);
        if (!ORDER_WAITING_SUPPLIER_CONFIRM.equals(order.getStatus())) {
            throw new IllegalStateException("订单当前状态不能拒单");
        }
        PurchaseOrder update = new PurchaseOrder();
        update.setStatus(ORDER_SUPPLIER_REJECTED);
        update.setPushedTo("供应商拒绝供货，采购方需要重新选择供应商或物资");
        update.setUpdateTime(LocalDateTime.now());
        int rows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getSupplierId, supplierId)
                .eq(PurchaseOrder::getStatus, ORDER_WAITING_SUPPLIER_CONFIRM));
        if (rows <= 0) {
            throw new IllegalStateException("订单状态已变化，请刷新后重试");
        }
        addTimeline(orderId, ORDER_SUPPLIER_REJECTED, "供应商拒绝供货", TARGET_SUPPLIER, supplierId,
                "采购方可重新选择供应商或调整采购清单");
        log.info("business_event event=supplier_rejected_order orderId={} supplierId={}", orderId, supplierId);
        return toPurchaseOrderView(purchaseOrderMapper.selectById(orderId));
    }

    @Transactional
    public PurchaseOrderView claimTransportOrder(Long driverId, String orderId) {
        findDriver(driverId);
        PurchaseOrder claimedOrder = new PurchaseOrder();
        claimedOrder.setStatus(ORDER_CLAIMED);
        claimedOrder.setDriverId(driverId);
        claimedOrder.setPushedTo("司机 " + driverId + " 已抢单");
        claimedOrder.setUpdateTime(LocalDateTime.now());
        int affectedRows = purchaseOrderMapper.update(claimedOrder, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getStatus, ORDER_WAITING_DRIVER)
                .isNull(PurchaseOrder::getDriverId));
        PurchaseOrder existingOrder = purchaseOrderMapper.selectById(orderId);
        if (existingOrder == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (affectedRows <= 0) {
            if (ORDER_CLAIMED.equals(existingOrder.getStatus()) && driverId.equals(existingOrder.getDriverId())) {
                return toPurchaseOrderView(existingOrder, PUSH_STATUS_CLAIMED);
            }
            throw new IllegalStateException("订单已被抢或当前状态不可抢");
        }
        OrderPushRecord pushRecord = orderPushRecordMapper.selectOne(new LambdaQueryWrapper<OrderPushRecord>()
                .eq(OrderPushRecord::getDriverId, driverId)
                .eq(OrderPushRecord::getOrderId, orderId));
        if (pushRecord != null) {
            pushRecord.setStatus(PUSH_STATUS_CLAIMED);
            pushRecord.setUpdateTime(LocalDateTime.now());
            orderPushRecordMapper.updateById(pushRecord);
        }
        addTimeline(orderId, ORDER_CLAIMED, "司机抢运输单", TARGET_DRIVER, driverId, "司机已接单，等待发车运输");
        existingOrder = purchaseOrderMapper.selectById(orderId);
        log.info("business_event event=driver_claimed_order orderId={} driverId={}", orderId, driverId);
        return toPurchaseOrderView(existingOrder);
    }

    @Transactional
    public PurchaseOrderView startTransportOrder(Long driverId, String orderId) {
        findDriver(driverId);
        PurchaseOrder update = new PurchaseOrder();
        update.setStatus(ORDER_TRANSPORTING);
        update.setPushedTo("司机 " + driverId + " 已发车运输");
        update.setUpdateTime(LocalDateTime.now());
        int rows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getDriverId, driverId)
                .eq(PurchaseOrder::getStatus, ORDER_CLAIMED));
        if (rows <= 0) {
            throw new IllegalStateException("订单当前状态不能开始运输");
        }
        addTimeline(orderId, ORDER_TRANSPORTING, "司机开始运输", TARGET_DRIVER, driverId, "订单进入在途状态");
        return toPurchaseOrderView(purchaseOrderMapper.selectById(orderId));
    }

    @Transactional
    public PurchaseOrderView completeTransportOrder(Long driverId, String orderId) {
        findDriver(driverId);
        PurchaseOrder update = new PurchaseOrder();
        update.setStatus(ORDER_COMPLETED);
        update.setPushedTo("订单已完成，等待三方履约评价");
        update.setUpdateTime(LocalDateTime.now());
        int rows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getDriverId, driverId)
                .eq(PurchaseOrder::getStatus, ORDER_TRANSPORTING));
        if (rows <= 0) {
            throw new IllegalStateException("订单当前状态不能完成运输");
        }
        addTimeline(orderId, ORDER_COMPLETED, "司机完成运输", TARGET_DRIVER, driverId, "订单闭环完成，可发起三方评价");
        log.info("business_event event=transport_completed orderId={} driverId={}", orderId, driverId);
        return toPurchaseOrderView(purchaseOrderMapper.selectById(orderId));
    }

    public PurchaseOrderView panicBuyOrder(Long purchaserId, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("抢购资源不存在");
        }
        if (!ORDER_PANIC_BUYING.equals(order.getStatus())) {
            throw new IllegalStateException("资源不在抢购状态");
        }
        initPanicStockIfNecessary(orderId);
        Long result = redisTemplate.execute(
                PANIC_BUY_SCRIPT,
                List.of(PANIC_STOCK_KEY_PREFIX + orderId, PANIC_BUYER_KEY_PREFIX + orderId + ":" + purchaserId),
                String.valueOf(purchaserId),
                String.valueOf(Duration.ofHours(2).toSeconds())
        );
        if (Long.valueOf(1L).equals(result)) {
            throw new IllegalStateException("资源名额已抢完");
        }
        if (Long.valueOf(2L).equals(result)) {
            throw new IllegalStateException("同一采购方不能重复抢购");
        }
        rabbitTemplate.convertAndSend(
                OrderRabbitConfig.ORDER_EXCHANGE,
                OrderRabbitConfig.ORDER_CLAIMED_ROUTING_KEY,
                orderId + ":" + purchaserId
        );
        log.info("business_event event=panic_buy_reserved orderId={} purchaserId={}", orderId, purchaserId);
        order.setStatus("抢购处理中");
        order.setPushedTo("Redis 原子占位成功，MQ 异步落库中");
        return toPurchaseOrderView(order);
    }

    private SupplierCatalogView toSupplierCatalogView(SupplierProfile supplier) {
        List<SupplierMaterial> supplierMaterials = supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplier.getSupplierId())
                .eq(SupplierMaterial::getStatus, 1));
        Map<Long, Material> materialsById = materialMapper.selectBatchIds(
                        supplierMaterials.stream().map(SupplierMaterial::getMaterialId).toList())
                .stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        List<SupplierMaterialView> materials = supplierMaterials.stream()
                .map(supplierMaterial -> toSupplierMaterialView(supplierMaterial, materialsById.get(supplierMaterial.getMaterialId())))
                .toList();
        return new SupplierCatalogView(
                supplier.getSupplierId(),
                supplier.getCompanyName(),
                supplier.getContactName(),
                "华东",
                supplier.getAddress(),
                supplier.getRatingScore().stripTrailingZeros().toPlainString(),
                List.of("营业执照", "供应商资质编号 " + supplier.getLicenseNo(), "平台履约评分 " + supplier.getRatingScore()),
                materials
        );
    }

    private SupplierMaterialView toSupplierMaterialView(SupplierMaterial supplierMaterial, Material material) {
        return new SupplierMaterialView(
                material.getId(),
                material.getMaterialName(),
                material.getCategory(),
                material.getUnit(),
                supplierMaterial.getSupplyPrice().stripTrailingZeros().toPlainString(),
                supplierMaterial.getStockQuantity() + " " + material.getUnit(),
                "日供 " + supplierMaterial.getDailyCapacity() + " " + material.getUnit()
        );
    }

    private SupplierMaterialManageView toSupplierMaterialManageView(SupplierMaterial supplierMaterial) {
        Material material = findMaterial(supplierMaterial.getMaterialId());
        return new SupplierMaterialManageView(
                supplierMaterial.getId(),
                material.getId(),
                material.getMaterialName(),
                material.getCategory(),
                material.getUnit(),
                supplierMaterial.getSupplyPrice(),
                supplierMaterial.getStockQuantity(),
                supplierMaterial.getDailyCapacity(),
                supplierMaterial.getDeliveryRadiusKm(),
                supplierMaterial.getStatus()
        );
    }

    private PurchaseOrderView toPurchaseOrderView(PurchaseOrder order) {
        return toPurchaseOrderView(order, null);
    }

    private PurchaseOrderView toPurchaseOrderView(PurchaseOrder order, String pushStatus) {
        return new PurchaseOrderView(
                order.getId(),
                order.getPurchaserId(),
                order.getPurchaserName(),
                order.getSupplierId(),
                order.getSupplierName(),
                order.getMaterialId(),
                order.getMaterialName(),
                order.getCategory(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus(),
                order.getSource(),
                order.getPushedTo(),
                order.getDriverId(),
                pushStatus,
                order.getCreateTime().format(VIEW_TIME_FORMATTER)
        );
    }

    private OrderTimelineView toOrderTimelineView(OrderTimeline timeline) {
        return new OrderTimelineView(
                timeline.getId(),
                timeline.getOrderId(),
                timeline.getStatus(),
                timeline.getAction(),
                timeline.getOperatorType(),
                timeline.getOperatorId(),
                timeline.getRemark(),
                timeline.getCreateTime().format(VIEW_TIME_FORMATTER)
        );
    }

    private OrderReviewView toOrderReviewView(OrderReview review) {
        return new OrderReviewView(
                review.getId(),
                review.getOrderId(),
                review.getReviewerType(),
                review.getReviewerId(),
                review.getTargetType(),
                review.getTargetId(),
                review.getScore(),
                review.getContent(),
                review.getCreateTime().format(VIEW_TIME_FORMATTER)
        );
    }

    private SupplierRankingView toSupplierRankingView(SupplierProfile profile, int rank) {
        return new SupplierRankingView(
                profile.getSupplierId(),
                profile.getCompanyName(),
                profile.getRatingScore().stripTrailingZeros().toPlainString(),
                rank
        );
    }

    private SupplierProfile findSupplier(Long supplierId) {
        SupplierProfile supplier = supplierProfileMapper.selectOne(new LambdaQueryWrapper<SupplierProfile>()
                .eq(SupplierProfile::getSupplierId, supplierId));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        return supplier;
    }

    private SupplierAccount findSupplierAccount(Long supplierId) {
        SupplierAccount account = supplierAccountMapper.selectById(supplierId);
        if (account == null) {
            throw new IllegalArgumentException("供应商账号不存在");
        }
        return account;
    }

    private Long orderCountByStatus(String status) {
        return purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getStatus, status));
    }

    private AdminSupplierAuditView toAdminSupplierAuditView(SupplierProfile supplier) {
        SupplierAccount account = supplierAccountMapper.selectById(supplier.getSupplierId());
        List<SupplierMaterial> materials = supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplier.getSupplierId()));
        long stockQuantity = materials.stream()
                .map(SupplierMaterial::getStockQuantity)
                .filter(stock -> stock != null)
                .mapToLong(Integer::longValue)
                .sum();
        Integer status = account == null ? AccountStatus.DISABLED.getCode() : account.getStatus();
        return new AdminSupplierAuditView(
                supplier.getSupplierId(),
                supplier.getCompanyName(),
                supplier.getContactName(),
                supplier.getContactPhone(),
                supplier.getLicenseNo(),
                supplier.getAddress(),
                supplier.getRatingScore().stripTrailingZeros().toPlainString(),
                status,
                AccountStatus.ENABLED.getCode() == status ? "已通过" : "待处理/已驳回",
                (long) materials.size(),
                stockQuantity
        );
    }

    private SupplierMaterial findSupplierMaterial(Long supplierId, Long materialId) {
        return supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                        .eq(SupplierMaterial::getSupplierId, supplierId)
                        .eq(SupplierMaterial::getMaterialId, materialId)
                        .eq(SupplierMaterial::getStatus, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("供应商未上架该物资"));
    }

    private SupplierMaterial findOwnedSupplierMaterial(Long supplierId, Long supplierMaterialId) {
        SupplierMaterial supplierMaterial = supplierMaterialMapper.selectById(supplierMaterialId);
        if (supplierMaterial == null || !supplierId.equals(supplierMaterial.getSupplierId())) {
            throw new IllegalArgumentException("供应物资不存在");
        }
        return supplierMaterial;
    }

    private PurchaseOrder findOrderForSupplier(Long supplierId, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null || !supplierId.equals(order.getSupplierId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private Material findMaterial(Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new IllegalArgumentException("物资不存在");
        }
        return material;
    }

    private PurchaserProfile findPurchaser(Long purchaserId) {
        PurchaserProfile purchaser = purchaserProfileMapper.selectOne(new LambdaQueryWrapper<PurchaserProfile>()
                .eq(PurchaserProfile::getPurchaserId, purchaserId));
        if (purchaser == null) {
            throw new IllegalArgumentException("采购方不存在");
        }
        return purchaser;
    }

    private DriverProfile findDriver(Long driverId) {
        DriverProfile driver = driverProfileMapper.selectOne(new LambdaQueryWrapper<DriverProfile>()
                .eq(DriverProfile::getDriverId, driverId));
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在");
        }
        return driver;
    }

    private List<NotificationView> purchaserNotifications(Long purchaserId) {
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getPurchaserId, purchaserId)
                .orderByDesc(PurchaseOrder::getUpdateTime)
                .last("LIMIT 8"));
        List<NotificationView> notifications = new ArrayList<>();
        for (PurchaseOrder order : orders) {
            notifications.add(notification(
                    "purchaser-order-" + order.getId(),
                    "采购订单状态更新",
                    order.getMaterialName() + " 当前状态：" + order.getStatus(),
                    "ORDER",
                    order.getStatus(),
                    order.getUpdateTime()
            ));
        }
        return notifications;
    }

    private List<NotificationView> supplierNotifications(Long supplierId) {
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getSupplierId, supplierId)
                .orderByDesc(PurchaseOrder::getCreateTime)
                .last("LIMIT 6"));
        List<NotificationView> notifications = new ArrayList<>();
        for (PurchaseOrder order : orders) {
            notifications.add(notification(
                    "supplier-order-" + order.getId(),
                    "收到采购需求",
                    order.getPurchaserName() + " 采购 " + order.getMaterialName() + "，数量 " + order.getQuantity(),
                    "ORDER",
                    order.getStatus(),
                    order.getCreateTime()
            ));
        }
        try {
            for (DeadLetterStatsView deadLetter : deadLetterStats()) {
                if (deadLetter.messages() > 0) {
                    notifications.add(notification(
                            "mq-" + deadLetter.queueName(),
                            "MQ 死信队列有积压",
                            deadLetter.queueName() + " 当前积压 " + deadLetter.messages() + " 条消息",
                            "MQ",
                            "WARN",
                            LocalDateTime.now()
                    ));
                }
            }
        } catch (RuntimeException ignored) {
            // 通知中心不能因为 MQ 观测失败影响供应商主页面加载。
        }
        return notifications;
    }

    private List<NotificationView> driverNotifications(Long driverId) {
        List<OrderPushRecord> pushRecords = orderPushRecordMapper.selectList(new LambdaQueryWrapper<OrderPushRecord>()
                .eq(OrderPushRecord::getDriverId, driverId)
                .orderByDesc(OrderPushRecord::getCreateTime)
                .last("LIMIT 8"));
        if (pushRecords.isEmpty()) {
            Long hallCount = purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getStatus, ORDER_WAITING_DRIVER));
            if (hallCount <= 0) {
                return List.of();
            }
            return List.of(notification(
                    "driver-hall",
                    "平台大厅有可抢运输单",
                    "当前有 " + hallCount + " 条订单等待司机接单",
                    "HALL",
                    "PENDING",
                    LocalDateTime.now()
            ));
        }
        List<String> orderIds = pushRecords.stream().map(OrderPushRecord::getOrderId).toList();
        Map<String, PurchaseOrder> ordersById = purchaseOrderMapper.selectBatchIds(orderIds)
                .stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, Function.identity()));
        List<NotificationView> notifications = new ArrayList<>();
        for (OrderPushRecord pushRecord : pushRecords) {
            PurchaseOrder order = ordersById.get(pushRecord.getOrderId());
            if (order == null) {
                continue;
            }
            notifications.add(notification(
                    "driver-push-" + pushRecord.getId(),
                    "收到运输订单推送",
                    order.getPurchaserName() + " -> " + order.getSupplierName() + "，" + order.getMaterialName(),
                    "PUSH",
                    pushRecord.getStatus(),
                    pushRecord.getCreateTime()
            ));
        }
        return notifications;
    }

    private List<NotificationView> adminNotifications() {
        List<NotificationView> notifications = new ArrayList<>();
        AdminDashboardView dashboard = adminDashboard();
        if (dashboard.abnormalCount() > 0) {
            notifications.add(notification(
                    "admin-abnormal",
                    "运营异常待处理",
                    "当前异常订单/死信合计 " + dashboard.abnormalCount() + " 条",
                    "ADMIN",
                    "WARN",
                    LocalDateTime.now()
            ));
        }
        if (dashboard.waitingSupplierConfirmCount() > 0) {
            notifications.add(notification(
                    "admin-waiting-supplier",
                    "供应商确认积压",
                    "有 " + dashboard.waitingSupplierConfirmCount() + " 条订单等待供应商确认",
                    "ORDER",
                    "PENDING",
                    LocalDateTime.now()
            ));
        }
        return notifications;
    }

    private NotificationView notification(String id,
                                          String title,
                                          String content,
                                          String type,
                                          String status,
                                          LocalDateTime createTime) {
        LocalDateTime time = createTime == null ? LocalDateTime.now() : createTime;
        return new NotificationView(id, title, content, type, status, time.format(VIEW_TIME_FORMATTER));
    }

    private OrderPushRecord findDriverPushRecord(Long driverId, String orderId) {
        OrderPushRecord record = orderPushRecordMapper.selectOne(new LambdaQueryWrapper<OrderPushRecord>()
                .eq(OrderPushRecord::getDriverId, driverId)
                .eq(OrderPushRecord::getOrderId, orderId));
        if (record == null) {
            throw new IllegalArgumentException("推送记录不存在");
        }
        return record;
    }

    private boolean existsFollow(Long driverId, Long purchaserId, String followType) {
        return driverFollowMapper.selectCount(new LambdaQueryWrapper<DriverFollow>()
                .eq(DriverFollow::getDriverId, driverId)
                .eq(DriverFollow::getPurchaserId, purchaserId)
                .eq(DriverFollow::getFollowType, followType)) > 0;
    }

    private Set<Long> relatedDriverIds(Long purchaserId) {
        List<DriverFollow> follows = driverFollowMapper.selectList(new LambdaQueryWrapper<DriverFollow>()
                .eq(DriverFollow::getPurchaserId, purchaserId)
                .in(DriverFollow::getFollowType, DRIVER_FOLLOW_PURCHASER, PURCHASER_FOLLOW_DRIVER));
        return follows.stream()
                .map(DriverFollow::getDriverId)
                .collect(Collectors.toSet());
    }

    private boolean createPushRecord(PurchaseOrder order, Long driverId) {
        if (orderPushRecordMapper.selectCount(new LambdaQueryWrapper<OrderPushRecord>()
                .eq(OrderPushRecord::getOrderId, order.getId())
                .eq(OrderPushRecord::getDriverId, driverId)) > 0) {
            return false;
        }
        OrderPushRecord record = new OrderPushRecord();
        record.setOrderId(order.getId());
        record.setDriverId(driverId);
        record.setPurchaserId(order.getPurchaserId());
        record.setPushType("COMPENSATION");
        record.setStatus("PENDING");
        record.setRetryCount(1);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(record.getCreateTime());
        try {
            orderPushRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }

    private void addTimeline(String orderId,
                             String status,
                             String action,
                             String operatorType,
                             Long operatorId,
                             String remark) {
        OrderTimeline timeline = new OrderTimeline();
        timeline.setOrderId(orderId);
        timeline.setStatus(status);
        timeline.setAction(action);
        timeline.setOperatorType(operatorType);
        timeline.setOperatorId(operatorId);
        timeline.setRemark(remark);
        timeline.setCreateTime(LocalDateTime.now());
        orderTimelineMapper.insert(timeline);
    }

    private int parseOrderQuantity(String quantity) {
        String numericQuantity = quantity == null ? "" : quantity.replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(numericQuantity)) {
            throw new IllegalArgumentException("订单数量格式不正确");
        }
        return Math.max(1, new BigDecimal(numericQuantity).setScale(0, RoundingMode.CEILING).intValue());
    }

    private String orderUnit(String quantity) {
        String unit = quantity == null ? "" : quantity.replaceAll("[0-9.\\s]", "");
        return StringUtils.hasText(unit) ? unit : "单位";
    }

    private void rebuildSupplierGeoFromDb() {
        List<SupplierProfile> suppliers = supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>());
        for (SupplierProfile supplier : suppliers) {
            if (supplier.getLongitude() != null && supplier.getLatitude() != null) {
                redisTemplate.opsForGeo().add(
                        SUPPLIER_GEO_KEY,
                        new Point(supplier.getLongitude().doubleValue(), supplier.getLatitude().doubleValue()),
                        String.valueOf(supplier.getSupplierId())
                );
            }
        }
    }

    private String driverAttendanceKey(LocalDate date) {
        return DRIVER_ATTENDANCE_KEY_PREFIX + date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private DeadLetterStatsView queueStats(String queueName) {
        java.util.Properties properties = amqpAdmin.getQueueProperties(queueName);
        if (properties == null) {
            return new DeadLetterStatsView(queueName, 0, 0);
        }
        return new DeadLetterStatsView(
                queueName,
                (Integer) properties.getOrDefault("QUEUE_MESSAGE_COUNT", 0),
                (Integer) properties.getOrDefault("QUEUE_CONSUMER_COUNT", 0)
        );
    }

    private void assertOrderParticipant(PurchaseOrder order, Long userId, String userType) {
        if (TARGET_ADMIN.equals(userType)) {
            return;
        }
        if (TARGET_PURCHASER.equals(userType) && userId.equals(order.getPurchaserId())) {
            return;
        }
        if (TARGET_SUPPLIER.equals(userType) && userId.equals(order.getSupplierId())) {
            return;
        }
        if (TARGET_DRIVER.equals(userType) && userId.equals(order.getDriverId())) {
            return;
        }
        throw new IllegalStateException("只能评价自己参与的订单");
    }

    private void assertOrderVisibleToUser(PurchaseOrder order, Long userId, String userType) {
        if (TARGET_PURCHASER.equals(userType) && userId.equals(order.getPurchaserId())) {
            return;
        }
        if (TARGET_SUPPLIER.equals(userType) && userId.equals(order.getSupplierId())) {
            return;
        }
        if (TARGET_DRIVER.equals(userType) && userId.equals(order.getDriverId())) {
            return;
        }
        throw new IllegalStateException("只能查看自己参与订单的评价");
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            throw new IllegalArgumentException("请选择评价对象类型");
        }
        String normalized = targetType.trim().toUpperCase();
        if (!List.of(TARGET_SUPPLIER, TARGET_PURCHASER, TARGET_DRIVER).contains(normalized)) {
            throw new IllegalArgumentException("评价对象类型不支持");
        }
        return normalized;
    }

    private void validateReviewTarget(PurchaseOrder order, String targetType, Long targetId) {
        if (targetId == null) {
            throw new IllegalArgumentException("请选择评价对象");
        }
        if (TARGET_SUPPLIER.equals(targetType) && targetId.equals(order.getSupplierId())) {
            return;
        }
        if (TARGET_PURCHASER.equals(targetType) && targetId.equals(order.getPurchaserId())) {
            return;
        }
        if (TARGET_DRIVER.equals(targetType) && targetId.equals(order.getDriverId())) {
            return;
        }
        throw new IllegalArgumentException("评价对象不属于该订单");
    }

    private void refreshSupplierRating(Long supplierId) {
        List<OrderReview> reviews = orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .eq(OrderReview::getTargetType, TARGET_SUPPLIER)
                .eq(OrderReview::getTargetId, supplierId));
        if (reviews.isEmpty()) {
            return;
        }
        BigDecimal totalScore = reviews.stream()
                .map(review -> BigDecimal.valueOf(review.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageScore = totalScore.divide(BigDecimal.valueOf(reviews.size()), 2, RoundingMode.HALF_UP);
        SupplierProfile supplier = findSupplier(supplierId);
        supplier.setRatingScore(averageScore);
        supplier.setUpdateTime(LocalDateTime.now());
        supplierProfileMapper.updateById(supplier);
        redisTemplate.opsForZSet().add(SUPPLIER_RANKING_KEY, String.valueOf(supplierId), averageScore.doubleValue());
        invalidateSupplierCatalogCacheWithDelay();
    }

    private List<SupplierRankingView> rebuildSupplierRankingFromDb() {
        List<SupplierProfile> suppliers = supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .orderByDesc(SupplierProfile::getRatingScore))
                .stream()
                .sorted(Comparator.comparing(SupplierProfile::getRatingScore).reversed())
                .toList();
        for (SupplierProfile supplier : suppliers) {
            redisTemplate.opsForZSet().add(
                    SUPPLIER_RANKING_KEY,
                    String.valueOf(supplier.getSupplierId()),
                    supplier.getRatingScore().doubleValue()
            );
        }
        List<SupplierRankingView> ranking = new ArrayList<>();
        for (int index = 0; index < suppliers.size(); index++) {
            ranking.add(toSupplierRankingView(suppliers.get(index), index + 1));
        }
        return ranking;
    }

    private BigDecimal nonNegativeMoney(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于 0");
        }
        return value;
    }

    private Integer nonNegativeInt(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于 0");
        }
        return value;
    }

    private void invalidateSupplierCatalogCacheWithDelay() {
        redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY);
        CompletableFuture.delayedExecutor(700, TimeUnit.MILLISECONDS)
                .execute(() -> redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY));
    }

    private String estimateAmount(BigDecimal price, String quantity) {
        String numericQuantity = quantity.replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(numericQuantity)) {
            return "待议价";
        }
        BigDecimal amount = price.multiply(new BigDecimal(numericQuantity));
        return "¥ " + amount.stripTrailingZeros().toPlainString();
    }

    private String newOrderId() {
        return "PO-" + LocalDateTime.now().format(ORDER_ID_FORMATTER) + "-" + orderSequence.getAndIncrement();
    }

    private void initPanicStockIfNecessary(String orderId) {
        String stockKey = PANIC_STOCK_KEY_PREFIX + orderId;
        Boolean initialized = redisTemplate.opsForValue().setIfAbsent(stockKey, "1", Duration.ofHours(2));
        if (Boolean.TRUE.equals(initialized)) {
            redisTemplate.opsForValue().set(stockKey, "1", Duration.ofHours(2));
        }
    }

    private void cachePendingOrder(PurchaseOrder order) {
        String key = PENDING_ORDER_KEY_PREFIX + order.getId();
        Map<String, String> fields = new HashMap<>();
        fields.put("id", order.getId());
        fields.put("purchaserId", String.valueOf(order.getPurchaserId()));
        fields.put("purchaserName", order.getPurchaserName());
        fields.put("supplierId", String.valueOf(order.getSupplierId()));
        fields.put("supplierName", order.getSupplierName());
        fields.put("materialId", String.valueOf(order.getMaterialId()));
        fields.put("materialName", order.getMaterialName());
        fields.put("category", order.getCategory());
        fields.put("quantity", order.getQuantity());
        fields.put("amount", order.getAmount());
        fields.put("status", order.getStatus());
        fields.put("source", order.getSource());
        fields.put("pushedTo", order.getPushedTo());
        fields.put("createTime", order.getCreateTime().toString());
        fields.put("updateTime", order.getUpdateTime().toString());
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, PENDING_ORDER_TTL);
    }

}
