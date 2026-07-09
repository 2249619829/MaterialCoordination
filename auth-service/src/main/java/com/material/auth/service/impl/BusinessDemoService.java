package com.material.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.material.auth.dto.business.AdminDashboardView;
import com.material.auth.dto.business.AdminSupplierAuditView;
import com.material.auth.dto.business.DeadLetterStatsView;
import com.material.auth.dto.business.DispatchRecommendationView;
import com.material.auth.dto.business.DriverAttendanceView;
import com.material.auth.dto.business.DriverFollowView;
import com.material.auth.dto.business.FulfillmentRankingsView;
import com.material.auth.dto.business.MaterialOptionView;
import com.material.auth.dto.business.NearbySupplierView;
import com.material.auth.dto.business.NotificationView;
import com.material.auth.dto.business.OrderAcceptanceRequest;
import com.material.auth.dto.business.OrderPaymentRequest;
import com.material.auth.dto.business.OrderReviewRequest;
import com.material.auth.dto.business.OrderReviewView;
import com.material.auth.dto.business.OrderTimelineView;
import com.material.auth.dto.business.PurchaseCartCheckoutRequest;
import com.material.auth.dto.business.PurchaseCartItemRequest;
import com.material.auth.dto.business.PurchaseOrderRequest;
import com.material.auth.dto.business.PurchaseOrderView;
import com.material.auth.dto.business.PurchaseRfqRequest;
import com.material.auth.dto.business.PurchaseRfqView;
import com.material.auth.dto.business.RfqQuoteView;
import com.material.auth.dto.business.SupplierQuoteRequest;
import com.material.auth.dto.business.SupplierCatalogView;
import com.material.auth.dto.business.SupplierMaterialManageRequest;
import com.material.auth.dto.business.SupplierMaterialManageView;
import com.material.auth.dto.business.SupplierMaterialView;
import com.material.auth.dto.business.SupplierQualificationRequest;
import com.material.auth.dto.business.SupplierQualificationView;
import com.material.auth.dto.business.SupplierRankingView;
import com.material.auth.dto.business.SupplierStoreView;
import com.material.auth.dto.business.TransportLocationReportRequest;
import com.material.auth.dto.business.TransportLocationReportView;
import com.material.auth.dto.business.TransportTrackingView;
import com.material.auth.config.OrderRabbitConfig;
import com.material.auth.entity.DriverFollow;
import com.material.auth.entity.DriverProfile;
import com.material.auth.entity.Material;
import com.material.auth.entity.OrderAcceptance;
import com.material.auth.entity.OrderPayment;
import com.material.auth.entity.OrderPushRecord;
import com.material.auth.entity.OrderReview;
import com.material.auth.entity.OrderTimeline;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.entity.PurchaseRfq;
import com.material.auth.entity.PurchaseRfqQuote;
import com.material.auth.entity.PurchaserProfile;
import com.material.auth.entity.SupplierMaterial;
import com.material.auth.entity.SupplierProfile;
import com.material.auth.entity.TransportLocationReport;
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
import com.material.auth.entity.SupplierAccount;
import com.material.auth.mapper.TransportLocationReportMapper;
import com.material.auth.service.geo.Coordinates;
import com.material.auth.service.geo.GeocodingService;
import com.material.auth.service.impl.support.DispatchRecommendationSupport;
import com.material.auth.service.impl.support.FulfillmentRankingSupport;
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
import org.springframework.scheduling.annotation.Scheduled;

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

import static com.material.auth.service.impl.support.OrderLifecycleSupport.*;

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
    private static final Set<String> ORDER_SUPPLIER_ACTIONABLE_STATUSES = Set.of(
            ORDER_WAITING_SUPPLIER_CONFIRM,
            ORDER_PURCHASER_CLAIMED
    );
    private static final String RFQ_OPEN = "OPEN";
    private static final String RFQ_AWARDED = "AWARDED";
    private static final String QUOTE_ACTIVE = "ACTIVE";
    private static final String QUOTE_SELECTED = "SELECTED";
    private static final String AUDIT_PENDING = "PENDING";
    private static final String AUDIT_APPROVED = "APPROVED";
    private static final String AUDIT_REJECTED = "REJECTED";
    private static final String DRIVER_FOLLOW_PURCHASER = "DRIVER_FOLLOW_PURCHASER";
    private static final String PURCHASER_FOLLOW_DRIVER = "PURCHASER_FOLLOW_DRIVER";
    public static final String PENDING_ORDER_KEY_PREFIX = "order:pending:";
    private static final String SUPPLIER_CATALOG_CACHE_KEY = "cache:supplier:catalog:v1";
    private static final String EMPTY_CACHE_VALUE = "[]";
    private static final String SUPPLIER_RANKING_KEY = "ranking:supplier:fulfillment";
    private static final String SUPPLIER_GEO_KEY = "geo:supplier";
    private static final String DRIVER_LOCATION_GEO_KEY = "driver:location:geo";
    private static final String TRANSPORT_ORDER_LOCATION_GEO_KEY = "transport:order:location:geo";
    private static final String DRIVER_ATTENDANCE_KEY_PREFIX = "attendance:driver:";
    private static final String TARGET_SUPPLIER = "SUPPLIER";
    private static final String TARGET_PURCHASER = "PURCHASER";
    private static final String TARGET_DRIVER = "DRIVER";
    private static final String TARGET_ADMIN = "ADMIN";
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
    private static final DefaultRedisScript<Long> TRANSPORT_CLAIM_SCRIPT = new DefaultRedisScript<>("""
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
    private final PurchaseRfqMapper purchaseRfqMapper;
    private final PurchaseRfqQuoteMapper purchaseRfqQuoteMapper;
    private final DriverFollowMapper driverFollowMapper;
    private final OrderPushRecordMapper orderPushRecordMapper;
    private final OrderAcceptanceMapper orderAcceptanceMapper;
    private final OrderPaymentMapper orderPaymentMapper;
    private final OrderReviewMapper orderReviewMapper;
    private final OrderTimelineMapper orderTimelineMapper;
    private final TransportLocationReportMapper transportLocationReportMapper;
    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GeocodingService geocodingService;
    private final AtomicInteger orderSequence = new AtomicInteger(1001);

    /**
     * 作用：创建 BusinessDemoService 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - supplierProfileMapper：供应商资料数据库操作对象，类型是 SupplierProfileMapper；方法会读取这个值继续处理。
     * - supplierAccountMapper：供应商账号数据库操作对象，类型是 SupplierAccountMapper；方法会读取这个值继续处理。
     * - supplierMaterialMapper：供应商物资数据库操作对象，类型是 SupplierMaterialMapper；方法会读取这个值继续处理。
     * - materialMapper：物资数据库操作对象，类型是 MaterialMapper；方法会读取这个值继续处理。
     * - driverProfileMapper：司机资料数据库操作对象，类型是 DriverProfileMapper；方法会读取这个值继续处理。
     * - purchaserProfileMapper：采购方资料数据库操作对象，类型是 PurchaserProfileMapper；方法会读取这个值继续处理。
     * - purchaseOrderMapper：采购订单数据库操作对象，类型是 PurchaseOrderMapper；方法会读取这个值继续处理。
     * - driverFollowMapper：司机关注关系数据库操作对象，类型是 DriverFollowMapper；方法会读取这个值继续处理。
     * - orderPushRecordMapper：订单推送记录数据库操作对象，类型是 OrderPushRecordMapper；方法会读取这个值继续处理。
     * - orderReviewMapper：订单评价数据库操作对象，类型是 OrderReviewMapper；方法会读取这个值继续处理。
     * - orderTimelineMapper：订单时间线数据库操作对象，类型是 OrderTimelineMapper；方法会读取这个值继续处理。
     * - rabbitTemplate：RabbitMQ 消息发送工具，类型是 RabbitTemplate；方法会读取这个值继续处理。
     * - amqpAdmin：RabbitMQ 管理工具，类型是 AmqpAdmin；方法会读取这个值继续处理。
     * - redisTemplate：Redis 操作工具，类型是 StringRedisTemplate；方法会读取这个值继续处理。
     * - objectMapper：JSON 转换工具，类型是 ObjectMapper；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public BusinessDemoService(SupplierProfileMapper supplierProfileMapper,
                               SupplierAccountMapper supplierAccountMapper,
                               SupplierMaterialMapper supplierMaterialMapper,
                               MaterialMapper materialMapper,
                               DriverProfileMapper driverProfileMapper,
                               PurchaserProfileMapper purchaserProfileMapper,
                               PurchaseOrderMapper purchaseOrderMapper,
                               PurchaseRfqMapper purchaseRfqMapper,
                               PurchaseRfqQuoteMapper purchaseRfqQuoteMapper,
                               DriverFollowMapper driverFollowMapper,
                               OrderPushRecordMapper orderPushRecordMapper,
                               OrderAcceptanceMapper orderAcceptanceMapper,
                               OrderPaymentMapper orderPaymentMapper,
                               OrderReviewMapper orderReviewMapper,
                               OrderTimelineMapper orderTimelineMapper,
                               TransportLocationReportMapper transportLocationReportMapper,
                               RabbitTemplate rabbitTemplate,
                               AmqpAdmin amqpAdmin,
                               StringRedisTemplate redisTemplate,
                               ObjectMapper objectMapper,
                               GeocodingService geocodingService) {
        this.supplierProfileMapper = supplierProfileMapper;
        this.supplierAccountMapper = supplierAccountMapper;
        this.supplierMaterialMapper = supplierMaterialMapper;
        this.materialMapper = materialMapper;
        this.driverProfileMapper = driverProfileMapper;
        this.purchaserProfileMapper = purchaserProfileMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseRfqMapper = purchaseRfqMapper;
        this.purchaseRfqQuoteMapper = purchaseRfqQuoteMapper;
        this.driverFollowMapper = driverFollowMapper;
        this.orderPushRecordMapper = orderPushRecordMapper;
        this.orderAcceptanceMapper = orderAcceptanceMapper;
        this.orderPaymentMapper = orderPaymentMapper;
        this.orderReviewMapper = orderReviewMapper;
        this.orderTimelineMapper = orderTimelineMapper;
        this.transportLocationReportMapper = transportLocationReportMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.geocodingService = geocodingService;
    }

    /**
     * 作用：查询采购方能看到的供应商目录。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<SupplierCatalogView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：查询某个供应商的店铺详情和物资列表。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 SupplierStoreView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：查询页面下拉框可选择的物资基础数据。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<MaterialOptionView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    public SupplierQualificationView supplierQualification(Long supplierId) {
        SupplierProfile supplier = findSupplier(supplierId);
        List<SupplierMaterial> materials = nullSafe(supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplierId)));
        return toSupplierQualificationView(supplier, materials);
    }

    @Transactional
    public SupplierQualificationView updateSupplierQualification(Long supplierId, SupplierQualificationRequest request) {
        SupplierProfile supplier = findSupplier(supplierId);
        supplier.setCompanyName(requiredText(request.companyName(), "企业名称"));
        supplier.setContactName(requiredText(request.contactName(), "联系人"));
        supplier.setContactPhone(requiredText(request.contactPhone(), "联系电话"));
        supplier.setLicenseNo(requiredText(request.licenseNo(), "营业执照编号"));
        String address = requiredText(request.address(), "经营地址");
        boolean addressChangedWithOldCoordinates = !address.equals(supplier.getAddress())
                && sameCoordinate(request.longitude(), supplier.getLongitude())
                && sameCoordinate(request.latitude(), supplier.getLatitude());
        Coordinates coordinates = addressChangedWithOldCoordinates
                ? resolveRequiredCoordinates(address, null, null)
                : resolveRequiredCoordinates(address, request.longitude(), request.latitude());
        supplier.setAddress(address);
        supplier.setLongitude(coordinates.longitude());
        supplier.setLatitude(coordinates.latitude());
        supplier.setBusinessLicenseUrl(optionalText(request.businessLicenseUrl()));
        supplier.setSafetyCertUrl(optionalText(request.safetyCertUrl()));
        supplier.setInsuranceCertUrl(optionalText(request.insuranceCertUrl()));
        supplier.setAuditStatus(AUDIT_PENDING);
        supplier.setAuditRemark("供应商资料已更新，待管理员复核");
        supplier.setUpdateTime(LocalDateTime.now());
        supplierProfileMapper.updateById(supplier);
        invalidateSupplierCatalogCacheWithDelay();
        log.info("business_event event=supplier_qualification_updated supplierId={}", supplierId);
        List<SupplierMaterial> materials = nullSafe(supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplierId)));
        return toSupplierQualificationView(supplier, materials);
    }

    /**
     * 作用：查询某个供应商自己维护的供应物资。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<SupplierMaterialManageView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<SupplierMaterialManageView> supplierMaterials(Long supplierId) {
        return supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                        .eq(SupplierMaterial::getSupplierId, supplierId)
                        .orderByDesc(SupplierMaterial::getStatus)
                        .orderByDesc(SupplierMaterial::getUpdateTime))
                .stream()
                .map(this::toSupplierMaterialManageView)
                .toList();
    }

    /**
     * 作用：新增一条供应商可供应的物资。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 SupplierMaterialManageView，这是给前端页面展示用的数据对象。
     */
    public SupplierMaterialManageView saveSupplierMaterial(Long supplierId, SupplierMaterialManageRequest request) {
        Material material = resolveMaterialForUpsert(request);
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

    /**
     * 作用：在保存供应商物资前，确定要使用哪条物资基础信息。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Material，也就是这个方法处理后的结果。
     */
    private Material resolveMaterialForUpsert(SupplierMaterialManageRequest request) {
        boolean hasMaterialId = request.materialId() != null;
        boolean hasNewMaterial = StringUtils.hasText(request.materialName());
        if (hasMaterialId && hasNewMaterial) {
            throw new IllegalArgumentException("不要同时选择已有物资和填写新物资");
        }
        if (hasNewMaterial) {
            return createOrReuseMaterial(
                    request.materialName().trim(),
                    requiredText(request.category(), "物资分类"),
                    requiredText(request.unit(), "计量单位")
            );
        }
        if (!hasMaterialId) {
            throw new IllegalArgumentException("请选择已有物资，或填写新物资名称");
        }
        return findMaterial(request.materialId());
    }

    /**
     * 作用：如果物资已存在就复用，不存在就创建一条新物资。
     * 输入：
     * - materialName：物资名称，类型是 String；方法会读取这个值继续处理。
     * - category：物资分类，类型是 String；方法会读取这个值继续处理。
     * - unit：计量单位，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Material，也就是这个方法处理后的结果。
     */
    private Material createOrReuseMaterial(String materialName, String category, String unit) {
        Material existing = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .eq(Material::getMaterialName, materialName)
                        .eq(Material::getCategory, category)
                        .eq(Material::getUnit, unit)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (existing.getStatus() == null || existing.getStatus() != 1) {
                existing.setStatus(1);
                existing.setUpdateTime(LocalDateTime.now());
                materialMapper.updateById(existing);
            }
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        Material material = new Material();
        material.setMaterialCode(nextMaterialCode(category));
        material.setMaterialName(materialName);
        material.setCategory(category);
        material.setUnit(unit);
        material.setDescription("供应商自定义新增物资");
        material.setStatus(1);
        material.setCreateTime(now);
        material.setUpdateTime(now);
        materialMapper.insert(material);
        return material;
    }

    /**
     * 作用：检查文本是否为空，并返回去掉前后空格后的文本。
     * 输入：
     * - value：数值，类型是 String；方法会读取这个值继续处理。
     * - label：字段名称，用来在报错时提示用户。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String requiredText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("请填写" + label);
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private BigDecimal requiredCoordinate(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException("请上传" + label);
        }
        return value;
    }

    private Coordinates resolveRequiredCoordinates(String address, BigDecimal longitude, BigDecimal latitude) {
        if (longitude != null || latitude != null) {
            if (longitude == null || latitude == null) {
                throw new IllegalArgumentException("经纬度需要成对填写");
            }
            validateCoordinateRange(longitude, latitude);
            return new Coordinates(longitude, latitude);
        }
        return geocodingService.resolve(address)
                .orElseThrow(() -> new IllegalArgumentException("未能根据地址获取经纬度，请手动填写"));
    }

    private void validateCoordinateRange(BigDecimal longitude, BigDecimal latitude) {
        if (longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0
                || latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("经纬度超出有效范围");
        }
    }

    private boolean sameCoordinate(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    /**
     * 作用：根据物资分类生成一个新的物资编码。
     * 输入：
     * - category：物资分类，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String nextMaterialCode(String category) {
        String normalized = category.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        if (!StringUtils.hasText(normalized) || normalized.replace("-", "").isBlank()) {
            normalized = "CUSTOM";
        }
        normalized = normalized.replaceAll("^-+|-+$", "");
        return "MAT-" + normalized + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    /**
     * 作用：修改一条供应商物资信息。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - supplierMaterialId：供应商物资记录编号，类型是 Long；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 SupplierMaterialManageView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：把一条供应商物资设置为下架状态。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - supplierMaterialId：供应商物资记录编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 SupplierMaterialManageView，这是给前端页面展示用的数据对象。
     */
    public SupplierMaterialManageView offlineSupplierMaterial(Long supplierId, Long supplierMaterialId) {
        SupplierMaterial supplierMaterial = findOwnedSupplierMaterial(supplierId, supplierMaterialId);
        supplierMaterial.setStatus(0);
        supplierMaterial.setUpdateTime(LocalDateTime.now());
        supplierMaterialMapper.updateById(supplierMaterial);
        invalidateSupplierCatalogCacheWithDelay();
        return toSupplierMaterialManageView(supplierMaterial);
    }

    /**
     * 作用：创建订单评价，并在评价供应商时刷新供应商评分。
     * 输入：
     * - reviewerId：评价人编号，类型是 Long；方法会读取这个值继续处理。
     * - reviewerType：评价人类型，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 OrderReviewView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：查询一个订单下的所有评价。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 List<OrderReviewView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：查询一个订单的状态变化记录。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 List<OrderTimelineView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    public TransportTrackingView transportTracking(Long userId, String userType, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        assertOrderVisibleToUser(order, userId, userType);
        List<TransportLocationReportView> locationReports = transportLocationReportMapper.selectList(
                        new LambdaQueryWrapper<TransportLocationReport>()
                                .eq(TransportLocationReport::getOrderId, orderId)
                                .orderByAsc(TransportLocationReport::getCreateTime))
                .stream()
                .map(this::toTransportLocationReportView)
                .toList();
        List<OrderTimelineView> timeline = orderTimelineMapper.selectList(new LambdaQueryWrapper<OrderTimeline>()
                        .eq(OrderTimeline::getOrderId, orderId)
                        .orderByAsc(OrderTimeline::getCreateTime))
                .stream()
                .map(this::toOrderTimelineView)
                .toList();
        return new TransportTrackingView(
                order.getId(),
                order.getStatus(),
                order.getDriverId(),
                order.getOriginAddress(),
                order.getOriginLongitude(),
                order.getOriginLatitude(),
                order.getDestinationAddress(),
                order.getDestinationLongitude(),
                order.getDestinationLatitude(),
                locationReports,
                timeline
        );
    }

    @Transactional
    public TransportLocationReportView reportTransportLocation(Long driverId,
                                                               String orderId,
                                                               TransportLocationReportRequest request) {
        findDriver(driverId);
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!driverId.equals(order.getDriverId())) {
            throw new IllegalStateException("只能上传自己承运订单的位置");
        }
        if (!ORDER_CLAIMED.equals(order.getStatus()) && !ORDER_TRANSPORTING.equals(order.getStatus())) {
            throw new IllegalStateException("当前订单状态不能上传运输位置");
        }
        BigDecimal longitude = requiredCoordinate(request.longitude(), "经度");
        BigDecimal latitude = requiredCoordinate(request.latitude(), "纬度");
        validateCoordinateRange(longitude, latitude);
        String remark = StringUtils.hasText(request.remark()) ? request.remark().trim() : "到达运输节点";

        TransportLocationReport report = new TransportLocationReport();
        report.setOrderId(orderId);
        report.setDriverId(driverId);
        report.setLongitude(longitude);
        report.setLatitude(latitude);
        report.setRemark(remark);
        report.setCreateTime(LocalDateTime.now());
        transportLocationReportMapper.insert(report);

        addTimeline(orderId, order.getStatus(), "司机上传到达节点", TARGET_DRIVER, driverId,
                remark + "，经度 " + longitude + "，纬度 " + latitude);
        writeLatestTransportGeo(driverId, orderId, longitude, latitude);
        log.info("business_event event=driver_reported_location orderId={} driverId={} longitude={} latitude={}",
                orderId, driverId, longitude, latitude);
        return toTransportLocationReportView(report);
    }

    /**
     * 作用：查询供应商履约评分排行榜。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<SupplierRankingView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<SupplierRankingView> supplierRanking() {
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        if (zSetOperations == null) {
            return rebuildSupplierRankingFromDb();
        }
        Set<ZSetOperations.TypedTuple<String>> tuples = zSetOperations.reverseRangeWithScores(SUPPLIER_RANKING_KEY, 0, 9);
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

    public FulfillmentRankingsView fulfillmentRankings() {
        List<OrderReview> reviews = nullSafe(orderReviewMapper.selectList(new LambdaQueryWrapper<OrderReview>()
                .in(OrderReview::getTargetType, TARGET_PURCHASER, TARGET_SUPPLIER, TARGET_DRIVER)));
        List<PurchaserProfile> purchasers = nullSafe(purchaserProfileMapper.selectList(new LambdaQueryWrapper<PurchaserProfile>()));
        List<DriverProfile> drivers = nullSafe(driverProfileMapper.selectList(new LambdaQueryWrapper<DriverProfile>()
                .orderByDesc(DriverProfile::getRatingScore)));
        return FulfillmentRankingSupport.create(
                reviews,
                purchasers,
                supplierRanking(),
                drivers
        );
    }

    /**
     * 作用：按照经纬度查找附近供应商。
     * 输入：
     * - longitude：经度，类型是 Double；方法会读取这个值继续处理。
     * - latitude：纬度，类型是 Double；方法会读取这个值继续处理。
     * - radiusKm：搜索半径，单位是公里；方法用它限制附近供应商的范围。
     * 输出：返回 List<NearbySupplierView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：记录司机今天是否出勤。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - online：是否在线或出勤，true 表示在线，false 表示离线。
     * 输出：返回 DriverAttendanceView，这是给前端页面展示用的数据对象。
     */
    public DriverAttendanceView markDriverAttendance(Long driverId, boolean online) {
        DriverProfile driver = findDriver(driverId);
        redisTemplate.opsForValue().setBit(driverAttendanceKey(LocalDate.now()), driverId, online);
        driver.setAttendanceStatus(online ? 1 : 0);
        driver.setUpdateTime(LocalDateTime.now());
        driverProfileMapper.updateById(driver);
        return new DriverAttendanceView(driverId, LocalDate.now().toString(), online);
    }

    /**
     * 作用：查询司机今天是否出勤。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 DriverAttendanceView，这是给前端页面展示用的数据对象。
     */
    public DriverAttendanceView todayDriverAttendance(Long driverId) {
        Boolean online = redisTemplate.opsForValue().getBit(driverAttendanceKey(LocalDate.now()), driverId);
        if (online == null) {
            DriverProfile driver = findDriver(driverId);
            online = Integer.valueOf(1).equals(driver.getAttendanceStatus());
        }
        return new DriverAttendanceView(driverId, LocalDate.now().toString(), Boolean.TRUE.equals(online));
    }

    /**
     * 作用：把司机收到的订单推送标记为已读。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：为缺失推送记录的订单重新生成司机推送记录。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Integer，表示方法算出的数量、编号或顺序值。
     */
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

    /**
     * 作用：查询 RabbitMQ 死信队列中的消息数量。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<DeadLetterStatsView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<DeadLetterStatsView> deadLetterStats() {
        return List.of(
                queueStats(OrderRabbitConfig.ORDER_CREATED_DEAD_LETTER_QUEUE),
                queueStats(OrderRabbitConfig.ORDER_CLAIMED_DEAD_LETTER_QUEUE)
        );
    }

    /**
     * 作用：查询管理员首页需要展示的统计数据。
     * 输入：
     * - 无输入参数。
     * 输出：返回 AdminDashboardView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：查询管理员审核供应商时使用的列表。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<AdminSupplierAuditView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<AdminSupplierAuditView> adminSuppliers() {
        return supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .orderByAsc(SupplierProfile::getSupplierId))
                .stream()
                .map(this::toAdminSupplierAuditView)
                .toList();
    }

    /**
     * 作用：把供应商账号改为审核通过状态。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 AdminSupplierAuditView，这是给前端页面展示用的数据对象。
     */
    public AdminSupplierAuditView approveSupplier(Long supplierId) {
        SupplierAccount account = findSupplierAccount(supplierId);
        account.setStatus(AccountStatus.ENABLED.getCode());
        account.setUpdateTime(LocalDateTime.now());
        supplierAccountMapper.updateById(account);
        SupplierProfile supplier = findSupplier(supplierId);
        supplier.setAuditStatus(AUDIT_APPROVED);
        supplier.setAuditRemark("资质审核通过，可参与采购协同");
        supplier.setUpdateTime(account.getUpdateTime());
        supplierProfileMapper.updateById(supplier);
        invalidateSupplierCatalogCacheWithDelay();
        log.info("business_event event=supplier_approved supplierId={}", supplierId);
        return toAdminSupplierAuditView(supplier);
    }

    /**
     * 作用：把供应商账号改为审核拒绝或禁用状态。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 AdminSupplierAuditView，这是给前端页面展示用的数据对象。
     */
    public AdminSupplierAuditView rejectSupplier(Long supplierId) {
        SupplierAccount account = findSupplierAccount(supplierId);
        account.setStatus(AccountStatus.DISABLED.getCode());
        account.setUpdateTime(LocalDateTime.now());
        supplierAccountMapper.updateById(account);
        SupplierProfile supplier = findSupplier(supplierId);
        supplier.setAuditStatus(AUDIT_REJECTED);
        supplier.setAuditRemark("资质审核未通过，请补充或修正企业资料");
        supplier.setUpdateTime(account.getUpdateTime());
        supplierProfileMapper.updateById(supplier);
        invalidateSupplierCatalogCacheWithDelay();
        log.info("business_event event=supplier_rejected supplierId={}", supplierId);
        return toAdminSupplierAuditView(supplier);
    }

    /**
     * 作用：查询管理员能看到的全部订单。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<PurchaseOrderView> adminOrders() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .orderByDesc(PurchaseOrder::getUpdateTime)
                        .last("LIMIT 50"))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 作用：从 Redis 读取供应商目录缓存。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<SupplierCatalogView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：把供应商目录写入 Redis 缓存。
     * 输入：
     * - catalog：供应商目录列表，类型是 List<SupplierCatalogView>；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void writeSupplierCatalogCache(List<SupplierCatalogView> catalog) {
        try {
            String cacheValue = catalog.isEmpty() ? EMPTY_CACHE_VALUE : objectMapper.writeValueAsString(catalog);
            Duration ttl = catalog.isEmpty() ? EMPTY_CATALOG_TTL : supplierCatalogTtl();
            redisTemplate.opsForValue().set(SUPPLIER_CATALOG_CACHE_KEY, cacheValue, ttl);
        } catch (JsonProcessingException ignored) {
            redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY);
        }
    }

    /**
     * 作用：生成供应商目录缓存的过期时间。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Duration，也就是这个方法处理后的结果。
     */
    private Duration supplierCatalogTtl() {
        return Duration.ofMinutes(5).plusSeconds(ThreadLocalRandom.current().nextInt(0, 180));
    }

    /**
     * 作用：查询采购方自己的采购订单。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<PurchaseOrderView> purchaserOrders(Long purchaserId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getPurchaserId, purchaserId)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    public PurchaseRfqView createPurchaseRfq(Long purchaserId, PurchaseRfqRequest request) {
        findPurchaser(purchaserId);
        LocalDateTime now = LocalDateTime.now();
        PurchaseRfq rfq = new PurchaseRfq();
        rfq.setPurchaserId(purchaserId);
        rfq.setMaterialName(requiredText(request.materialName(), "物资名称"));
        rfq.setCategory(requiredText(request.category(), "物资分类"));
        rfq.setUnit(requiredText(request.unit(), "计量单位"));
        rfq.setQuantity(requiredText(request.quantity(), "采购数量"));
        String deliveryAddress = requiredText(request.deliveryAddress(), "收货地址");
        Coordinates coordinates = resolveRequiredCoordinates(deliveryAddress, request.longitude(), request.latitude());
        rfq.setDeliveryAddress(deliveryAddress);
        rfq.setLongitude(coordinates.longitude());
        rfq.setLatitude(coordinates.latitude());
        rfq.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : "");
        rfq.setStatus(RFQ_OPEN);
        rfq.setCreateTime(now);
        rfq.setUpdateTime(now);
        purchaseRfqMapper.insert(rfq);
        return toPurchaseRfqView(rfq);
    }

    public List<PurchaseRfqView> purchaserRfqs(Long purchaserId) {
        return purchaseRfqMapper.selectList(new LambdaQueryWrapper<PurchaseRfq>()
                        .eq(PurchaseRfq::getPurchaserId, purchaserId)
                        .orderByDesc(PurchaseRfq::getCreateTime))
                .stream()
                .map(this::toPurchaseRfqView)
                .toList();
    }

    public List<PurchaseRfqView> openRfqsForSupplier(Long supplierId) {
        findSupplier(supplierId);
        return purchaseRfqMapper.selectList(new LambdaQueryWrapper<PurchaseRfq>()
                        .eq(PurchaseRfq::getStatus, RFQ_OPEN)
                        .orderByDesc(PurchaseRfq::getCreateTime))
                .stream()
                .map(this::toPurchaseRfqView)
                .toList();
    }

    public RfqQuoteView quoteRfq(Long supplierId, SupplierQuoteRequest request) {
        PurchaseRfq rfq = findRfq(request.rfqId());
        if (!RFQ_OPEN.equals(rfq.getStatus())) {
            throw new IllegalStateException("询价单已结束，不能继续报价");
        }
        SupplierMaterial supplierMaterial = findOwnedSupplierMaterial(supplierId, request.supplierMaterialId());
        LocalDateTime now = LocalDateTime.now();
        PurchaseRfqQuote quote = purchaseRfqQuoteMapper.selectOne(new LambdaQueryWrapper<PurchaseRfqQuote>()
                .eq(PurchaseRfqQuote::getRfqId, rfq.getId())
                .eq(PurchaseRfqQuote::getSupplierId, supplierId));
        if (quote == null) {
            quote = new PurchaseRfqQuote();
            quote.setRfqId(rfq.getId());
            quote.setSupplierId(supplierId);
            quote.setCreateTime(now);
        }
        quote.setSupplierMaterialId(supplierMaterial.getId());
        quote.setUnitPrice(nonNegativeMoney(request.unitPrice(), "报价单价"));
        quote.setAvailableQuantity(nonNegativeInt(request.availableQuantity(), "可供数量"));
        quote.setDeliveryDays(nonNegativeInt(request.deliveryDays(), "交付天数"));
        quote.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : "");
        quote.setStatus(QUOTE_ACTIVE);
        quote.setUpdateTime(now);
        if (quote.getId() == null) {
            purchaseRfqQuoteMapper.insert(quote);
        } else {
            purchaseRfqQuoteMapper.updateById(quote);
        }
        return toRfqQuoteViews(List.of(quote)).get(0);
    }

    public List<RfqQuoteView> supplierRfqQuotes(Long supplierId) {
        return toRfqQuoteViews(purchaseRfqQuoteMapper.selectList(new LambdaQueryWrapper<PurchaseRfqQuote>()
                .eq(PurchaseRfqQuote::getSupplierId, supplierId)
                .orderByDesc(PurchaseRfqQuote::getUpdateTime)));
    }

    public List<RfqQuoteView> purchaserRfqQuotes(Long purchaserId, Long rfqId) {
        PurchaseRfq rfq = findRfq(rfqId);
        if (!purchaserId.equals(rfq.getPurchaserId())) {
            throw new IllegalArgumentException("只能查看自己的询价报价");
        }
        return toRfqQuoteViews(purchaseRfqQuoteMapper.selectList(new LambdaQueryWrapper<PurchaseRfqQuote>()
                .eq(PurchaseRfqQuote::getRfqId, rfqId)));
    }

    @Transactional
    public PurchaseOrderView acceptRfqQuote(Long purchaserId, Long quoteId) {
        PurchaseRfqQuote quote = purchaseRfqQuoteMapper.selectById(quoteId);
        if (quote == null) {
            throw new IllegalArgumentException("报价不存在");
        }
        PurchaseRfq rfq = findRfq(quote.getRfqId());
        if (!purchaserId.equals(rfq.getPurchaserId())) {
            throw new IllegalArgumentException("只能采纳自己的询价报价");
        }
        if (!RFQ_OPEN.equals(rfq.getStatus())) {
            throw new IllegalStateException("询价单已结束");
        }
        SupplierMaterial supplierMaterial = supplierMaterialMapper.selectById(quote.getSupplierMaterialId());
        if (supplierMaterial == null || !quote.getSupplierId().equals(supplierMaterial.getSupplierId())) {
            throw new IllegalArgumentException("报价关联的供应物资不存在");
        }
        Material material = findMaterial(supplierMaterial.getMaterialId());
        SupplierProfile supplier = findSupplier(quote.getSupplierId());
        PurchaserProfile purchaser = findPurchaser(purchaserId);
        PurchaseOrder order = buildPurchaseOrderFromRfq(rfq, quote, supplier, purchaser, material);
        publishPendingOrder(order);

        quote.setStatus(QUOTE_SELECTED);
        quote.setUpdateTime(LocalDateTime.now());
        purchaseRfqQuoteMapper.updateById(quote);
        rfq.setStatus(RFQ_AWARDED);
        rfq.setSelectedQuoteId(quote.getId());
        rfq.setUpdateTime(quote.getUpdateTime());
        purchaseRfqMapper.updateById(rfq);
        log.info("business_event event=rfq_quote_accepted rfqId={} quoteId={} orderId={}",
                rfq.getId(), quote.getId(), order.getId());
        return toPurchaseOrderView(order);
    }

    /**
     * 作用：查询供应商自己的供货订单。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<PurchaseOrderView> supplierOrders(Long supplierId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getSupplierId, supplierId)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 作用：查询等待司机接单的运输大厅订单。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<PurchaseOrderView> transportHall() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, ORDER_WAITING_DRIVER)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 作用：为等待司机接单的订单推荐合适运力。
     * 输入：
     * - userId：当前用户编号，用于判断订单可见性。
     * - userType：当前用户角色，用于判断订单可见性。
     * - orderId：订单编号，方法会基于订单发货地计算司机距离。
     * 输出：返回 List<DispatchRecommendationView>，按在线、距离、评分综合排序。
     */
    public List<DispatchRecommendationView> dispatchRecommendations(Long userId, String userType, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        assertDispatchRecommendationVisible(order, userId, userType);
        if (!ORDER_WAITING_DRIVER.equals(order.getStatus())) {
            throw new IllegalStateException("订单尚未进入待司机接单状态");
        }
        if (order.getOriginLongitude() == null || order.getOriginLatitude() == null) {
            throw new IllegalStateException("订单缺少发货地经纬度，无法推荐司机");
        }
        return DispatchRecommendationSupport.rank(
                order,
                nullSafe(driverProfileMapper.selectList(new LambdaQueryWrapper<DriverProfile>())),
                5
        );
    }

    /**
     * 作用：查询正在抢购中的订单资源。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<PurchaseOrderView> panicBuyHall() {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, ORDER_PANIC_BUYING)
                        .orderByDesc(PurchaseOrder::getCreateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 作用：查询推送给某个司机的订单。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：查询司机已经接下的运输订单。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    public List<PurchaseOrderView> driverOrders(Long driverId) {
        return purchaseOrderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getDriverId, driverId)
                        .in(PurchaseOrder::getStatus, ORDER_CLAIMED, ORDER_TRANSPORTING, ORDER_COMPLETED)
                        .orderByDesc(PurchaseOrder::getUpdateTime))
                .stream()
                .map(this::toPurchaseOrderView)
                .toList();
    }

    /**
     * 作用：查询司机关注了哪些采购方。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<DriverFollowView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：让司机关注一个采购方。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 DriverFollowView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：根据角色查询通知中心消息。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 List<NotificationView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：创建一笔采购订单。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - materialId：物资编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    public PurchaseOrderView createPurchaseOrder(Long purchaserId, Long supplierId, Long materialId) {
        return createPurchaseOrder(purchaserId, new PurchaseOrderRequest(supplierId, materialId, null, "线上沟通后确认"));
    }

    /**
     * 作用：创建一笔采购订单。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    public PurchaseOrderView createPurchaseOrder(Long purchaserId, PurchaseOrderRequest request) {
        PurchaseOrder order = buildPurchaseOrder(purchaserId, request);
        publishPendingOrder(order);
        log.info("business_event event=order_created orderId={} purchaserId={} supplierId={} materialId={}",
                order.getId(), purchaserId, order.getSupplierId(), order.getMaterialId());
        return toPurchaseOrderView(order);
    }

    /**
     * 作用：把采购请求组装成订单实体。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 PurchaseOrder，也就是这个方法处理后的结果。
     */
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
        applyOrderPlaces(order, supplier, purchaser);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(order.getCreateTime());
        return order;
    }

    private PurchaseOrder buildPurchaseOrderFromRfq(PurchaseRfq rfq,
                                                    PurchaseRfqQuote quote,
                                                    SupplierProfile supplier,
                                                    PurchaserProfile purchaser,
                                                    Material material) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(newOrderId());
        order.setPurchaserId(rfq.getPurchaserId());
        order.setPurchaserName(purchaser.getCompanyName());
        order.setSupplierId(supplier.getSupplierId());
        order.setSupplierName(supplier.getCompanyName());
        order.setMaterialId(material.getId());
        order.setMaterialName(material.getMaterialName());
        order.setCategory(material.getCategory());
        order.setQuantity(rfq.getQuantity());
        order.setAmount(estimateAmount(quote.getUnitPrice(), rfq.getQuantity()));
        order.setStatus(ORDER_WAITING_SUPPLIER_CONFIRM);
        order.setSource("采购方采纳询价 RFQ-" + rfq.getId() + " 报价，等待供应商确认");
        order.setPushedTo("询价报价已采纳，供应商确认后推送给关注关系司机");
        applyOrderPlaces(order, supplier, purchaser);
        if (StringUtils.hasText(rfq.getDeliveryAddress())) {
            order.setDestinationAddress(rfq.getDeliveryAddress());
            order.setDestinationLongitude(rfq.getLongitude());
            order.setDestinationLatitude(rfq.getLatitude());
        }
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(order.getCreateTime());
        return order;
    }

    private void applyOrderPlaces(PurchaseOrder order, SupplierProfile supplier, PurchaserProfile purchaser) {
        order.setOriginAddress(supplier.getAddress());
        order.setOriginLongitude(supplier.getLongitude());
        order.setOriginLatitude(supplier.getLatitude());
        order.setDestinationAddress(purchaser.getAddress());
        order.setDestinationLongitude(purchaser.getLongitude());
        order.setDestinationLatitude(purchaser.getLatitude());
    }

    /**
     * 作用：先把订单放进 Redis 临时保存，再发送 RabbitMQ 消息。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    /**
     * 作用：把采购清单里的多项物资批量生成订单。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 List<PurchaseOrderView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：供应商确认供货，并扣减库存。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    @Transactional
    public PurchaseOrderView confirmSupplierOrder(Long supplierId, String orderId) {
        PurchaseOrder order = findOrderForSupplier(supplierId, orderId);
        if (!ORDER_SUPPLIER_ACTIONABLE_STATUSES.contains(order.getStatus())) {
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
                .in(PurchaseOrder::getStatus, ORDER_SUPPLIER_ACTIONABLE_STATUSES));
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

    /**
     * 作用：供应商拒绝供货订单。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    @Transactional
    public PurchaseOrderView rejectSupplierOrder(Long supplierId, String orderId) {
        PurchaseOrder order = findOrderForSupplier(supplierId, orderId);
        if (!ORDER_SUPPLIER_ACTIONABLE_STATUSES.contains(order.getStatus())) {
            throw new IllegalStateException("订单当前状态不能拒单");
        }
        PurchaseOrder update = new PurchaseOrder();
        update.setStatus(ORDER_SUPPLIER_REJECTED);
        update.setPushedTo("供应商拒绝供货，采购方需要重新选择供应商或物资");
        update.setUpdateTime(LocalDateTime.now());
        int rows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getSupplierId, supplierId)
                .in(PurchaseOrder::getStatus, ORDER_SUPPLIER_ACTIONABLE_STATUSES));
        if (rows <= 0) {
            throw new IllegalStateException("订单状态已变化，请刷新后重试");
        }
        addTimeline(orderId, ORDER_SUPPLIER_REJECTED, "供应商拒绝供货", TARGET_SUPPLIER, supplierId,
                "采购方可重新选择供应商或调整采购清单");
        log.info("business_event event=supplier_rejected_order orderId={} supplierId={}", orderId, supplierId);
        return toPurchaseOrderView(purchaseOrderMapper.selectById(orderId));
    }

    /**
     * 作用：司机抢下一个待接单的运输订单。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    @Transactional
    public PurchaseOrderView claimTransportOrder(Long driverId, String orderId) {
        findDriver(driverId);
        PurchaseOrder existingOrder = purchaseOrderMapper.selectById(orderId);
        if (existingOrder == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (ORDER_CLAIMED.equals(existingOrder.getStatus()) && driverId.equals(existingOrder.getDriverId())) {
            return toPurchaseOrderView(existingOrder, PUSH_STATUS_CLAIMED);
        }
        if (!ORDER_WAITING_DRIVER.equals(existingOrder.getStatus()) || existingOrder.getDriverId() != null) {
            throw new IllegalStateException("订单已被抢或当前状态不可抢");
        }

        initTransportClaimStockIfNecessary(orderId);
        String stockKey = transportClaimStockKey(orderId);
        String driverKey = transportClaimDriverKey(orderId, driverId);
        Long result = redisTemplate.execute(
                TRANSPORT_CLAIM_SCRIPT,
                List.of(stockKey, driverKey),
                String.valueOf(driverId),
                String.valueOf(Duration.ofHours(2).toSeconds())
        );
        if (result == null) {
            throw new IllegalStateException("Redis 抢单结果为空，请稍后重试");
        }
        if (result == 1L) {
            throw new IllegalStateException("订单已被抢或当前状态不可抢");
        }
        if (result == 2L) {
            throw new IllegalStateException("司机已提交过该订单抢单请求，请稍后查看结果");
        }

        rabbitTemplate.convertAndSend(
                OrderRabbitConfig.ORDER_EXCHANGE,
                OrderRabbitConfig.ORDER_CLAIMED_ROUTING_KEY,
                "transport:" + orderId + ":" + driverId
        );
        addTimeline(orderId, ORDER_CLAIMED, "司机抢运输单预占成功", TARGET_DRIVER, driverId,
                "Redis Lua 已完成运力名额预占，RabbitMQ 异步绑定司机");
        existingOrder.setStatus(ORDER_CLAIMED);
        existingOrder.setDriverId(driverId);
        existingOrder.setPushedTo("司机 " + driverId + " 已抢单，等待异步落库确认");
        log.info("business_event event=driver_claimed_order orderId={} driverId={}", orderId, driverId);
        return toPurchaseOrderView(existingOrder, PUSH_STATUS_CLAIMED);
    }

    /**
     * 作用：司机把订单状态改为运输中。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：司机把订单状态改为已完成。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    @Transactional
    public PurchaseOrderView completeTransportOrder(Long driverId, String orderId) {
        findDriver(driverId);
        PurchaseOrder update = new PurchaseOrder();
        update.setStatus(ORDER_COMPLETED);
        update.setPushedTo("订单已完成，等待采购方验收签收");
        update.setUpdateTime(LocalDateTime.now());
        int rows = purchaseOrderMapper.update(update, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, orderId)
                .eq(PurchaseOrder::getDriverId, driverId)
                .eq(PurchaseOrder::getStatus, ORDER_TRANSPORTING));
        if (rows <= 0) {
            throw new IllegalStateException("订单当前状态不能完成运输");
        }
        addTimeline(orderId, ORDER_COMPLETED, "司机完成运输", TARGET_DRIVER, driverId, "订单到达采购方，等待验收签收");
        log.info("business_event event=transport_completed orderId={} driverId={}", orderId, driverId);
        return toPurchaseOrderView(purchaseOrderMapper.selectById(orderId));
    }

    @Transactional
    public PurchaseOrderView acceptPurchaseOrder(Long purchaserId, String orderId, OrderAcceptanceRequest request) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!purchaserId.equals(order.getPurchaserId())) {
            throw new IllegalArgumentException("只能验收自己的采购订单");
        }
        if (!ORDER_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("只有已完成运输的订单才能验收");
        }
        OrderAcceptance existing = orderAcceptanceMapper.selectOne(new LambdaQueryWrapper<OrderAcceptance>()
                .eq(OrderAcceptance::getOrderId, orderId));
        if (existing != null) {
            throw new IllegalStateException("订单已经验收，不能重复提交");
        }
        String result = normalizeAcceptanceResult(request.acceptanceResult());
        OrderAcceptance acceptance = new OrderAcceptance();
        acceptance.setOrderId(orderId);
        acceptance.setPurchaserId(purchaserId);
        acceptance.setSignerName(requiredText(request.signerName(), "签收人"));
        acceptance.setAcceptanceResult(result);
        acceptance.setProofUrl(optionalText(request.proofUrl()));
        acceptance.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : acceptanceStatusText(result));
        acceptance.setCreateTime(LocalDateTime.now());
        acceptance.setUpdateTime(acceptance.getCreateTime());
        orderAcceptanceMapper.insert(acceptance);
        orderPaymentMapper.insert(pendingPaymentFor(order, purchaserId, acceptance.getCreateTime()));

        order.setPushedTo(acceptanceStatusText(result) + "：" + acceptance.getSignerName() + "，" + acceptance.getRemark());
        order.setUpdateTime(acceptance.getCreateTime());
        purchaseOrderMapper.updateById(order);
        addTimeline(orderId, order.getStatus(), "采购方验收签收", TARGET_PURCHASER, purchaserId, order.getPushedTo());
        log.info("business_event event=order_accepted orderId={} purchaserId={} result={}", orderId, purchaserId, result);
        return toPurchaseOrderView(order);
    }

    @Transactional
    public PurchaseOrderView payPurchaseOrder(Long purchaserId, String orderId, OrderPaymentRequest request) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!purchaserId.equals(order.getPurchaserId())) {
            throw new IllegalArgumentException("只能为自己的采购订单付款");
        }
        if (!ORDER_COMPLETED.equals(order.getStatus())) {
            throw new IllegalStateException("只有已完成订单才能付款");
        }
        OrderAcceptance acceptance = orderAcceptanceMapper.selectOne(new LambdaQueryWrapper<OrderAcceptance>()
                .eq(OrderAcceptance::getOrderId, orderId));
        if (acceptance == null) {
            throw new IllegalStateException("订单验收后才能付款");
        }
        OrderPayment existing = orderPaymentMapper.selectOne(new LambdaQueryWrapper<OrderPayment>()
                .eq(OrderPayment::getOrderId, orderId));
        if (existing == null) {
            existing = pendingPaymentFor(order, purchaserId, LocalDateTime.now());
            existing.setAmount(requirePositiveAmount(request.amount()));
            existing.setStatus(PAYMENT_PAID);
            existing.setPaymentMethod(normalizePaymentMethod(request.paymentMethod()));
            existing.setPaymentReference(requiredText(request.paymentReference(), "付款流水号"));
            existing.setProofUrl(optionalText(request.proofUrl()));
            existing.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : "付款凭证已登记");
            existing.setPaidTime(LocalDateTime.now());
            existing.setUpdateTime(existing.getPaidTime());
            orderPaymentMapper.insert(existing);
            order.setPushedTo("采购方已付款：" + paymentMethodText(existing.getPaymentMethod()) + "，流水号 " + existing.getPaymentReference());
            order.setUpdateTime(existing.getPaidTime());
            purchaseOrderMapper.updateById(order);
            addTimeline(orderId, order.getStatus(), "采购方付款登记", TARGET_PURCHASER, purchaserId, paymentSummary(existing));
            log.info("business_event event=order_paid orderId={} purchaserId={} method={} amount={}", orderId, purchaserId, existing.getPaymentMethod(), existing.getAmount());
            return toPurchaseOrderView(order);
        }
        if (PAYMENT_PAID.equals(existing.getStatus())) {
            throw new IllegalStateException("订单已经付款，不能重复提交");
        }
        if (PAYMENT_TIMEOUT.equals(existing.getStatus()) || paymentExpired(existing, LocalDateTime.now())) {
            existing.setStatus(PAYMENT_TIMEOUT);
            existing.setRemark("支付超时：超过1小时未完成付款");
            existing.setUpdateTime(LocalDateTime.now());
            orderPaymentMapper.updateById(existing);
            throw new IllegalStateException("支付已超时，请联系管理员重新开启付款");
        }
        BigDecimal amount = requirePositiveAmount(request.amount());
        String method = normalizePaymentMethod(request.paymentMethod());
        String reference = requiredText(request.paymentReference(), "付款流水号");

        existing.setAmount(amount);
        existing.setPaymentMethod(method);
        existing.setPaymentReference(reference);
        existing.setProofUrl(optionalText(request.proofUrl()));
        existing.setStatus(PAYMENT_PAID);
        existing.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : "付款凭证已登记");
        existing.setPaidTime(LocalDateTime.now());
        existing.setUpdateTime(existing.getPaidTime());
        orderPaymentMapper.updateById(existing);

        order.setPushedTo("采购方已付款：" + paymentMethodText(method) + "，流水号 " + reference);
        order.setUpdateTime(existing.getPaidTime());
        purchaseOrderMapper.updateById(order);
        addTimeline(orderId, order.getStatus(), "采购方付款登记", TARGET_PURCHASER, purchaserId, paymentSummary(existing));
        log.info("business_event event=order_paid orderId={} purchaserId={} method={} amount={}", orderId, purchaserId, method, amount);
        return toPurchaseOrderView(order);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public int expireOverduePayments() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderPayment> expiredPayments = orderPaymentMapper.selectList(new LambdaQueryWrapper<OrderPayment>()
                .eq(OrderPayment::getStatus, PAYMENT_PENDING)
                .le(OrderPayment::getExpiresAt, now));
        for (OrderPayment payment : nullSafe(expiredPayments)) {
            payment.setStatus(PAYMENT_TIMEOUT);
            payment.setRemark("支付超时：超过1小时未完成付款");
            payment.setUpdateTime(now);
            orderPaymentMapper.updateById(payment);
            log.info("business_event event=payment_timeout orderId={} purchaserId={}", payment.getOrderId(), payment.getPurchaserId());
        }
        return expiredPayments == null ? 0 : expiredPayments.size();
    }

    /**
     * 作用：采购方抢购一个处于抢购状态的订单资源。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
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
                List.of(panicStockKey(orderId), panicBuyerKey(orderId, purchaserId)),
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

    /**
     * 作用：把供应商资料转换成前端目录展示对象。
     * 输入：
     * - supplier：供应商资料，类型是 SupplierProfile；方法会读取这个值继续处理。
     * 输出：返回 SupplierCatalogView，这是给前端页面展示用的数据对象。
     */
    private SupplierCatalogView toSupplierCatalogView(SupplierProfile supplier) {
        List<SupplierMaterial> supplierMaterials = supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplier.getSupplierId())
                .eq(SupplierMaterial::getStatus, 1));
        List<Long> materialIds = supplierMaterials.stream().map(SupplierMaterial::getMaterialId).toList();
        Map<Long, Material> materialsById = materialIds.isEmpty()
                ? Map.of()
                : materialMapper.selectBatchIds(materialIds)
                        .stream()
                        .collect(Collectors.toMap(Material::getId, Function.identity()));
        List<SupplierMaterialView> materials = supplierMaterials.stream()
                .filter(supplierMaterial -> materialsById.containsKey(supplierMaterial.getMaterialId()))
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

    /**
     * 作用：把供应商物资转换成前端展示对象。
     * 输入：
     * - supplierMaterial：供应商物资记录，类型是 SupplierMaterial；方法会读取这个值继续处理。
     * - material：物资资料，类型是 Material；方法会读取这个值继续处理。
     * 输出：返回 SupplierMaterialView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：把供应商物资转换成管理页面展示对象。
     * 输入：
     * - supplierMaterial：供应商物资记录，类型是 SupplierMaterial；方法会读取这个值继续处理。
     * 输出：返回 SupplierMaterialManageView，这是给前端页面展示用的数据对象。
     */
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

    private PurchaseRfqView toPurchaseRfqView(PurchaseRfq rfq) {
        PurchaserProfile purchaser = findPurchaser(rfq.getPurchaserId());
        List<RfqQuoteView> quotes = toRfqQuoteViews(purchaseRfqQuoteMapper.selectList(new LambdaQueryWrapper<PurchaseRfqQuote>()
                .eq(PurchaseRfqQuote::getRfqId, rfq.getId())));
        return new PurchaseRfqView(
                rfq.getId(),
                rfq.getPurchaserId(),
                purchaser.getCompanyName(),
                rfq.getMaterialName(),
                rfq.getCategory(),
                rfq.getUnit(),
                rfq.getQuantity(),
                rfq.getDeliveryAddress(),
                rfq.getLongitude(),
                rfq.getLatitude(),
                rfq.getStatus(),
                quotes.size(),
                quotes.isEmpty() ? null : quotes.get(0),
                rfq.getRemark(),
                rfq.getCreateTime() == null ? "" : rfq.getCreateTime().format(VIEW_TIME_FORMATTER)
        );
    }

    private List<RfqQuoteView> toRfqQuoteViews(List<PurchaseRfqQuote> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return List.of();
        }
        List<Long> supplierMaterialIds = quotes.stream()
                .map(PurchaseRfqQuote::getSupplierMaterialId)
                .distinct()
                .toList();
        Map<Long, SupplierMaterial> supplierMaterialsById = nullSafe(supplierMaterialMapper.selectBatchIds(supplierMaterialIds))
                .stream()
                .collect(Collectors.toMap(SupplierMaterial::getId, Function.identity()));
        List<Long> materialIds = supplierMaterialsById.values().stream()
                .map(SupplierMaterial::getMaterialId)
                .distinct()
                .toList();
        Map<Long, Material> materialsById = materialIds.isEmpty()
                ? Map.of()
                : nullSafe(materialMapper.selectBatchIds(materialIds)).stream()
                        .collect(Collectors.toMap(Material::getId, Function.identity()));
        List<Long> supplierIds = quotes.stream()
                .map(PurchaseRfqQuote::getSupplierId)
                .distinct()
                .toList();
        Map<Long, SupplierProfile> suppliersById = supplierIds.isEmpty()
                ? Map.of()
                : nullSafe(supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .in(SupplierProfile::getSupplierId, supplierIds))).stream()
                        .collect(Collectors.toMap(SupplierProfile::getSupplierId, Function.identity()));

        return quotes.stream()
                .map(quote -> toRfqQuoteView(
                        quote,
                        supplierMaterialsById.get(quote.getSupplierMaterialId()),
                        materialsById,
                        suppliersById.get(quote.getSupplierId())))
                .sorted(Comparator
                        .comparing(RfqQuoteView::unitPrice)
                        .thenComparing(RfqQuoteView::deliveryDays)
                        .thenComparing(RfqQuoteView::recommendScore, Comparator.reverseOrder()))
                .toList();
    }

    private RfqQuoteView toRfqQuoteView(PurchaseRfqQuote quote,
                                        SupplierMaterial supplierMaterial,
                                        Map<Long, Material> materialsById,
                                        SupplierProfile supplier) {
        Material material = supplierMaterial == null ? null : materialsById.get(supplierMaterial.getMaterialId());
        BigDecimal recommendScore = quoteRecommendScore(quote, supplierMaterial, supplier);
        return new RfqQuoteView(
                quote.getId(),
                quote.getRfqId(),
                quote.getSupplierId(),
                supplier == null ? "未知供应商" : supplier.getCompanyName(),
                quote.getSupplierMaterialId(),
                material == null ? null : material.getId(),
                material == null ? "未知物资" : material.getMaterialName(),
                material == null ? "" : material.getCategory(),
                material == null ? "" : material.getUnit(),
                quote.getUnitPrice(),
                quote.getAvailableQuantity(),
                quote.getDeliveryDays(),
                quote.getRemark(),
                recommendScore,
                quote.getStatus(),
                quote.getCreateTime() == null ? "" : quote.getCreateTime().format(VIEW_TIME_FORMATTER)
        );
    }

    private BigDecimal quoteRecommendScore(PurchaseRfqQuote quote, SupplierMaterial supplierMaterial, SupplierProfile supplier) {
        BigDecimal price = quote.getUnitPrice() == null ? BigDecimal.ZERO : quote.getUnitPrice();
        BigDecimal priceScore = price.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : new BigDecimal("100000").divide(price.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        BigDecimal ratingScore = supplier == null || supplier.getRatingScore() == null
                ? BigDecimal.ZERO
                : supplier.getRatingScore().multiply(new BigDecimal("10"));
        BigDecimal stockScore = supplierMaterial == null || supplierMaterial.getStockQuantity() == null
                ? BigDecimal.ZERO
                : new BigDecimal(supplierMaterial.getStockQuantity()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
        BigDecimal deliveryPenalty = new BigDecimal(quote.getDeliveryDays() == null ? 0 : quote.getDeliveryDays())
                .multiply(new BigDecimal("2"));
        return priceScore.add(ratingScore).add(stockScore).subtract(deliveryPenalty).setScale(2, RoundingMode.HALF_UP);
    }

    private <T> List<T> nullSafe(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * 作用：把订单实体转换成前端展示对象。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    private PurchaseOrderView toPurchaseOrderView(PurchaseOrder order) {
        return toPurchaseOrderView(order, null);
    }

    /**
     * 作用：把订单实体转换成前端展示对象。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * - pushStatus：推送状态，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrderView，这是给前端页面展示用的数据对象。
     */
    private PurchaseOrderView toPurchaseOrderView(PurchaseOrder order, String pushStatus) {
        OrderAcceptance acceptance = orderAcceptanceMapper.selectOne(new LambdaQueryWrapper<OrderAcceptance>()
                .eq(OrderAcceptance::getOrderId, order.getId()));
        OrderPayment payment = orderPaymentMapper.selectOne(new LambdaQueryWrapper<OrderPayment>()
                .eq(OrderPayment::getOrderId, order.getId()));
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
                order.getCreateTime().format(VIEW_TIME_FORMATTER),
                acceptance == null ? "待验收" : acceptanceStatusText(acceptance.getAcceptanceResult()),
                acceptance == null ? "运输完成后由采购方验收签收" : acceptanceSummary(acceptance),
                acceptance == null ? "" : acceptance.getProofUrl(),
                payment == null ? "待付款" : paymentStatusText(payment.getStatus()),
                payment == null ? "验收完成后由采购方登记付款凭证" : paymentSummary(payment),
                payment == null || payment.getExpiresAt() == null ? "" : payment.getExpiresAt().format(VIEW_TIME_FORMATTER),
                payment == null ? "" : payment.getProofUrl(),
                order.getOriginAddress(),
                order.getOriginLongitude(),
                order.getOriginLatitude(),
                order.getDestinationAddress(),
                order.getDestinationLongitude(),
                order.getDestinationLatitude()
        );
    }

    /**
     * 作用：把订单时间线实体转换成前端展示对象。
     * 输入：
     * - timeline：订单时间线记录，类型是 OrderTimeline；方法会读取这个值继续处理。
     * 输出：返回 OrderTimelineView，这是给前端页面展示用的数据对象。
     */
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

    private TransportLocationReportView toTransportLocationReportView(TransportLocationReport report) {
        return new TransportLocationReportView(
                report.getId(),
                report.getOrderId(),
                report.getDriverId(),
                report.getLongitude(),
                report.getLatitude(),
                report.getRemark(),
                report.getCreateTime().format(VIEW_TIME_FORMATTER)
        );
    }

    /**
     * 作用：把评价实体转换成前端展示对象。
     * 输入：
     * - review：订单评价记录，类型是 OrderReview；方法会读取这个值继续处理。
     * 输出：返回 OrderReviewView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：把供应商资料转换成排行榜展示对象。
     * 输入：
     * - profile：资料对象，包含页面展示用的用户或企业信息。
     * - rank：排行榜名次，从 1 开始。
     * 输出：返回 SupplierRankingView，这是给前端页面展示用的数据对象。
     */
    private SupplierRankingView toSupplierRankingView(SupplierProfile profile, int rank) {
        return new SupplierRankingView(
                profile.getSupplierId(),
                profile.getCompanyName(),
                profile.getRatingScore().stripTrailingZeros().toPlainString(),
                rank
        );
    }

    /**
     * 作用：按照供应商编号查询供应商资料，不存在就报错。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 SupplierProfile，也就是这个方法处理后的结果。
     */
    private SupplierProfile findSupplier(Long supplierId) {
        SupplierProfile supplier = supplierProfileMapper.selectOne(new LambdaQueryWrapper<SupplierProfile>()
                .eq(SupplierProfile::getSupplierId, supplierId));
        if (supplier == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        return supplier;
    }

    /**
     * 作用：按照供应商编号查询供应商账号，不存在就报错。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 SupplierAccount，也就是这个方法处理后的结果。
     */
    private SupplierAccount findSupplierAccount(Long supplierId) {
        SupplierAccount account = supplierAccountMapper.selectById(supplierId);
        if (account == null) {
            throw new IllegalArgumentException("供应商账号不存在");
        }
        return account;
    }

    /**
     * 作用：统计某个订单状态下有多少订单。
     * 输入：
     * - status：状态，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Long，表示方法算出的数量、编号或顺序值。
     */
    private Long orderCountByStatus(String status) {
        return purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getStatus, status));
    }

    /**
     * 作用：把供应商资料转换成管理员审核页面对象。
     * 输入：
     * - supplier：供应商资料，类型是 SupplierProfile；方法会读取这个值继续处理。
     * 输出：返回 AdminSupplierAuditView，这是给前端页面展示用的数据对象。
     */
    private AdminSupplierAuditView toAdminSupplierAuditView(SupplierProfile supplier) {
        SupplierAccount account = supplierAccountMapper.selectById(supplier.getSupplierId());
        List<SupplierMaterial> materials = nullSafe(supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                .eq(SupplierMaterial::getSupplierId, supplier.getSupplierId())));
        long stockQuantity = materials.stream()
                .map(SupplierMaterial::getStockQuantity)
                .filter(stock -> stock != null)
                .mapToLong(Integer::longValue)
                .sum();
        Integer status = account == null ? AccountStatus.DISABLED.getCode() : account.getStatus();
        String auditStatus = normalizeAuditStatus(supplier.getAuditStatus(), status);
        return new AdminSupplierAuditView(
                supplier.getSupplierId(),
                supplier.getCompanyName(),
                supplier.getContactName(),
                supplier.getContactPhone(),
                supplier.getLicenseNo(),
                supplier.getAddress(),
                supplier.getRatingScore() == null ? "0" : supplier.getRatingScore().stripTrailingZeros().toPlainString(),
                status,
                auditStatusText(auditStatus),
                auditStatus,
                StringUtils.hasText(supplier.getAuditRemark()) ? supplier.getAuditRemark() : "暂无审核备注",
                qualificationCompletion(supplier),
                qualificationRiskTags(supplier, materials),
                (long) materials.size(),
                stockQuantity
        );
    }

    private SupplierQualificationView toSupplierQualificationView(SupplierProfile supplier, List<SupplierMaterial> materials) {
        String auditStatus = normalizeAuditStatus(supplier.getAuditStatus(), AccountStatus.ENABLED.getCode());
        return new SupplierQualificationView(
                supplier.getSupplierId(),
                supplier.getCompanyName(),
                supplier.getContactName(),
                supplier.getContactPhone(),
                supplier.getLicenseNo(),
                supplier.getAddress(),
                supplier.getLongitude(),
                supplier.getLatitude(),
                supplier.getBusinessLicenseUrl(),
                supplier.getSafetyCertUrl(),
                supplier.getInsuranceCertUrl(),
                auditStatus,
                auditStatusText(auditStatus),
                StringUtils.hasText(supplier.getAuditRemark()) ? supplier.getAuditRemark() : "暂无审核备注",
                qualificationCompletion(supplier),
                qualificationRiskTags(supplier, materials)
        );
    }

    private int qualificationCompletion(SupplierProfile supplier) {
        int completed = 0;
        if (StringUtils.hasText(supplier.getCompanyName())) completed++;
        if (StringUtils.hasText(supplier.getContactPhone())) completed++;
        if (StringUtils.hasText(supplier.getLicenseNo())) completed++;
        if (StringUtils.hasText(supplier.getAddress())) completed++;
        if (supplier.getLongitude() != null && supplier.getLatitude() != null) completed++;
        if (StringUtils.hasText(supplier.getBusinessLicenseUrl())) completed++;
        if (StringUtils.hasText(supplier.getSafetyCertUrl())) completed++;
        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(7), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private List<String> qualificationRiskTags(SupplierProfile supplier, List<SupplierMaterial> materials) {
        List<String> risks = new ArrayList<>();
        if (!StringUtils.hasText(supplier.getBusinessLicenseUrl())) {
            risks.add("缺少营业执照附件");
        }
        if (!StringUtils.hasText(supplier.getSafetyCertUrl())) {
            risks.add("缺少安全生产证明");
        }
        if (!StringUtils.hasText(supplier.getInsuranceCertUrl())) {
            risks.add("缺少履约保险证明");
        }
        if (supplier.getLongitude() == null || supplier.getLatitude() == null) {
            risks.add("缺少服务坐标");
        }
        if (materials.isEmpty()) {
            risks.add("暂无上架物资");
        }
        if (risks.isEmpty()) {
            risks.add("资质资料完整");
        }
        return risks;
    }

    private String normalizeAuditStatus(String auditStatus, Integer accountStatus) {
        if (AccountStatus.DISABLED.getCode() == (accountStatus == null ? AccountStatus.DISABLED.getCode() : accountStatus)) {
            return StringUtils.hasText(auditStatus) ? auditStatus : AUDIT_REJECTED;
        }
        return StringUtils.hasText(auditStatus) ? auditStatus : AUDIT_APPROVED;
    }

    private String auditStatusText(String auditStatus) {
        return switch (auditStatus) {
            case AUDIT_PENDING -> "待复核";
            case AUDIT_REJECTED -> "已驳回";
            case AUDIT_APPROVED -> "已通过";
            default -> auditStatus;
        };
    }

    /**
     * 作用：查询供应商是否拥有指定物资，不存在就报错。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - materialId：物资编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 SupplierMaterial，也就是这个方法处理后的结果。
     */
    private SupplierMaterial findSupplierMaterial(Long supplierId, Long materialId) {
        return supplierMaterialMapper.selectList(new LambdaQueryWrapper<SupplierMaterial>()
                        .eq(SupplierMaterial::getSupplierId, supplierId)
                        .eq(SupplierMaterial::getMaterialId, materialId)
                        .eq(SupplierMaterial::getStatus, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("供应商未上架该物资"));
    }

    /**
     * 作用：查询供应商自己的某条物资记录，不存在就报错。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - supplierMaterialId：供应商物资记录编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 SupplierMaterial，也就是这个方法处理后的结果。
     */
    private SupplierMaterial findOwnedSupplierMaterial(Long supplierId, Long supplierMaterialId) {
        SupplierMaterial supplierMaterial = supplierMaterialMapper.selectById(supplierMaterialId);
        if (supplierMaterial == null || !supplierId.equals(supplierMaterial.getSupplierId())) {
            throw new IllegalArgumentException("供应物资不存在");
        }
        return supplierMaterial;
    }

    /**
     * 作用：查询供应商自己的某个订单，不存在就报错。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 PurchaseOrder，也就是这个方法处理后的结果。
     */
    private PurchaseOrder findOrderForSupplier(Long supplierId, String orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null || !supplierId.equals(order.getSupplierId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    /**
     * 作用：按照物资编号查询物资，不存在就报错。
     * 输入：
     * - materialId：物资编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 Material，也就是这个方法处理后的结果。
     */
    private Material findMaterial(Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new IllegalArgumentException("物资不存在");
        }
        return material;
    }

    private PurchaseRfq findRfq(Long rfqId) {
        PurchaseRfq rfq = purchaseRfqMapper.selectById(rfqId);
        if (rfq == null) {
            throw new IllegalArgumentException("询价单不存在");
        }
        return rfq;
    }

    /**
     * 作用：按照采购方编号查询采购方资料，不存在就报错。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 PurchaserProfile，也就是这个方法处理后的结果。
     */
    private PurchaserProfile findPurchaser(Long purchaserId) {
        PurchaserProfile purchaser = purchaserProfileMapper.selectOne(new LambdaQueryWrapper<PurchaserProfile>()
                .eq(PurchaserProfile::getPurchaserId, purchaserId));
        if (purchaser == null) {
            throw new IllegalArgumentException("采购方不存在");
        }
        return purchaser;
    }

    /**
     * 作用：按照司机编号查询司机资料，不存在就报错。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 DriverProfile，也就是这个方法处理后的结果。
     */
    private DriverProfile findDriver(Long driverId) {
        DriverProfile driver = driverProfileMapper.selectOne(new LambdaQueryWrapper<DriverProfile>()
                .eq(DriverProfile::getDriverId, driverId));
        if (driver == null) {
            throw new IllegalArgumentException("司机不存在");
        }
        return driver;
    }

    /**
     * 作用：组装采购方能看到的通知。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<NotificationView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：组装供应商能看到的通知。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<NotificationView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：组装司机能看到的通知。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 List<NotificationView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：组装管理员能看到的通知。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<NotificationView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
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

    /**
     * 作用：创建一条通知展示对象。
     * 输入：
     * - id：编号，类型是 String；方法会读取这个值继续处理。
     * - title：通知标题，类型是 String；方法会读取这个值继续处理。
     * - content：评价内容，类型是 String；方法会读取这个值继续处理。
     * - type：类型，类型是 String；方法会读取这个值继续处理。
     * - status：状态，类型是 String；方法会读取这个值继续处理。
     * - createTime：创建时间，类型是 LocalDateTime；方法会读取这个值继续处理。
     * 输出：返回 NotificationView，这是给前端页面展示用的数据对象。
     */
    private NotificationView notification(String id,
                                          String title,
                                          String content,
                                          String type,
                                          String status,
                                          LocalDateTime createTime) {
        LocalDateTime time = createTime == null ? LocalDateTime.now() : createTime;
        return new NotificationView(id, title, content, type, status, time.format(VIEW_TIME_FORMATTER));
    }

    /**
     * 作用：查询司机和订单对应的推送记录。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 OrderPushRecord，也就是这个方法处理后的结果。
     */
    private OrderPushRecord findDriverPushRecord(Long driverId, String orderId) {
        OrderPushRecord record = orderPushRecordMapper.selectOne(new LambdaQueryWrapper<OrderPushRecord>()
                .eq(OrderPushRecord::getDriverId, driverId)
                .eq(OrderPushRecord::getOrderId, orderId));
        if (record == null) {
            throw new IllegalArgumentException("推送记录不存在");
        }
        return record;
    }

    /**
     * 作用：判断司机和采购方之间是否已有关注关系。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * - followType：关注类型，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 boolean，true 表示条件成立，false 表示条件不成立。
     */
    private boolean existsFollow(Long driverId, Long purchaserId, String followType) {
        return driverFollowMapper.selectCount(new LambdaQueryWrapper<DriverFollow>()
                .eq(DriverFollow::getDriverId, driverId)
                .eq(DriverFollow::getPurchaserId, purchaserId)
                .eq(DriverFollow::getFollowType, followType)) > 0;
    }

    /**
     * 作用：找出和采购方有关联的司机编号。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 Set<Long>，也就是这个方法处理后的结果。
     */
    private Set<Long> relatedDriverIds(Long purchaserId) {
        List<DriverFollow> follows = driverFollowMapper.selectList(new LambdaQueryWrapper<DriverFollow>()
                .eq(DriverFollow::getPurchaserId, purchaserId)
                .in(DriverFollow::getFollowType, DRIVER_FOLLOW_PURCHASER, PURCHASER_FOLLOW_DRIVER));
        return follows.stream()
                .map(DriverFollow::getDriverId)
                .collect(Collectors.toSet());
    }

    /**
     * 作用：为某个司机创建一条订单推送记录。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 boolean，true 表示条件成立，false 表示条件不成立。
     */
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

    /**
     * 作用：给订单增加一条状态变化记录。
     * 输入：
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * - status：状态，类型是 String；方法会读取这个值继续处理。
     * - action：操作动作，类型是 String；方法会读取这个值继续处理。
     * - operatorType：操作人类型，类型是 String；方法会读取这个值继续处理。
     * - operatorId：操作人编号，类型是 Long；方法会读取这个值继续处理。
     * - remark：备注，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    /**
     * 作用：从订单数量文本中解析出数字部分。
     * 输入：
     * - quantity：采购数量，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 int，表示方法算出的数量、编号或顺序值。
     */
    private int parseOrderQuantity(String quantity) {
        String numericQuantity = quantity == null ? "" : quantity.replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(numericQuantity)) {
            throw new IllegalArgumentException("订单数量格式不正确");
        }
        return Math.max(1, new BigDecimal(numericQuantity).setScale(0, RoundingMode.CEILING).intValue());
    }

    /**
     * 作用：从订单数量文本中解析出单位部分。
     * 输入：
     * - quantity：采购数量，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String orderUnit(String quantity) {
        String unit = quantity == null ? "" : quantity.replaceAll("[0-9.\\s]", "");
        return StringUtils.hasText(unit) ? unit : "单位";
    }

    /**
     * 作用：把数据库里的供应商经纬度重新写入 Redis GEO。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    private void writeLatestTransportGeo(Long driverId, String orderId, BigDecimal longitude, BigDecimal latitude) {
        try {
            Point point = new Point(longitude.doubleValue(), latitude.doubleValue());
            redisTemplate.opsForGeo().add(DRIVER_LOCATION_GEO_KEY, point, String.valueOf(driverId));
            redisTemplate.opsForGeo().add(TRANSPORT_ORDER_LOCATION_GEO_KEY, point, orderId);
        } catch (Exception exception) {
            log.warn("transport latest geo cache update failed orderId={} driverId={}", orderId, driverId, exception);
        }
    }

    /**
     * 作用：生成司机出勤 Bitmap 使用的 Redis key。
     * 输入：
     * - date：日期，类型是 LocalDate；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String driverAttendanceKey(LocalDate date) {
        return DRIVER_ATTENDANCE_KEY_PREFIX + date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * 作用：读取某个 RabbitMQ 队列的统计信息。
     * 输入：
     * - queueName：队列名称，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 DeadLetterStatsView，这是给前端页面展示用的数据对象。
     */
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

    /**
     * 作用：检查当前用户是否是订单参与方。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    /**
     * 作用：检查当前用户是否有权限看到这个订单。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    private void assertDispatchRecommendationVisible(PurchaseOrder order, Long userId, String userType) {
        if (TARGET_ADMIN.equals(userType)) {
            return;
        }
        if (TARGET_PURCHASER.equals(userType) && userId.equals(order.getPurchaserId())) {
            return;
        }
        if (TARGET_SUPPLIER.equals(userType) && userId.equals(order.getSupplierId())) {
            return;
        }
        throw new IllegalStateException("只能查看自己参与订单的调度推荐");
    }

    /**
     * 作用：把评价对象类型统一转换成系统使用的标准值。
     * 输入：
     * - targetType：被评价对象类型，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
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

    /**
     * 作用：完成 validateReviewTarget 这一步处理。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * - targetType：被评价对象类型，类型是 String；方法会读取这个值继续处理。
     * - targetId：被评价对象编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    /**
     * 作用：重新计算供应商评分并更新排行榜。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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

    /**
     * 作用：从数据库重建供应商排行榜缓存。
     * 输入：
     * - 无输入参数。
     * 输出：返回 List<SupplierRankingView>，也就是一组结果列表；列表里的每一项都是页面或后续代码要用的数据。
     */
    private List<SupplierRankingView> rebuildSupplierRankingFromDb() {
        List<SupplierProfile> suppliers = supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfile>()
                        .orderByDesc(SupplierProfile::getRatingScore))
                .stream()
                .sorted(Comparator.comparing(SupplierProfile::getRatingScore).reversed())
                .toList();
        ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        if (zSetOperations != null) {
            for (SupplierProfile supplier : suppliers) {
                zSetOperations.add(
                        SUPPLIER_RANKING_KEY,
                        String.valueOf(supplier.getSupplierId()),
                        supplier.getRatingScore().doubleValue()
                );
            }
        }
        List<SupplierRankingView> ranking = new ArrayList<>();
        for (int index = 0; index < suppliers.size(); index++) {
            ranking.add(toSupplierRankingView(suppliers.get(index), index + 1));
        }
        return ranking;
    }

    /**
     * 作用：把金额参数整理成不能小于 0 的金额。
     * 输入：
     * - value：数值，类型是 BigDecimal；方法会读取这个值继续处理。
     * - fieldName：字段名称，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 BigDecimal，也就是这个方法处理后的结果。
     */
    private BigDecimal nonNegativeMoney(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于 0");
        }
        return value;
    }

    /**
     * 作用：把整数参数整理成不能小于 0 的整数。
     * 输入：
     * - value：数值，类型是 Integer；方法会读取这个值继续处理。
     * - fieldName：字段名称，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Integer，表示方法算出的数量、编号或顺序值。
     */
    private Integer nonNegativeInt(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + "不能小于 0");
        }
        return value;
    }

    /**
     * 作用：删除供应商目录缓存，并延迟再删一次。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void invalidateSupplierCatalogCacheWithDelay() {
        redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY);
        CompletableFuture.delayedExecutor(700, TimeUnit.MILLISECONDS)
                .execute(() -> redisTemplate.delete(SUPPLIER_CATALOG_CACHE_KEY));
    }

    /**
     * 作用：根据单价和数量估算订单金额。
     * 输入：
     * - price：价格，类型是 BigDecimal；方法会读取这个值继续处理。
     * - quantity：采购数量，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String estimateAmount(BigDecimal price, String quantity) {
        String numericQuantity = quantity.replaceAll("[^0-9.]", "");
        if (!StringUtils.hasText(numericQuantity)) {
            return "待议价";
        }
        BigDecimal amount = price.multiply(new BigDecimal(numericQuantity));
        return "¥ " + amount.stripTrailingZeros().toPlainString();
    }

    /**
     * 作用：生成新的订单编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String newOrderId() {
        return "PO-" + LocalDateTime.now().format(ORDER_ID_FORMATTER) + "-" + orderSequence.getAndIncrement();
    }

    /**
     * 作用：如果抢购库存还没有初始化，就写入初始库存。
     * 输入：
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void initPanicStockIfNecessary(String orderId) {
        String stockKey = panicStockKey(orderId);
        Boolean initialized = redisTemplate.opsForValue().setIfAbsent(stockKey, "1", Duration.ofHours(2));
        if (Boolean.TRUE.equals(initialized)) {
            redisTemplate.opsForValue().set(stockKey, "1", Duration.ofHours(2));
        }
    }

    private void initTransportClaimStockIfNecessary(String orderId) {
        String stockKey = transportClaimStockKey(orderId);
        Boolean initialized = redisTemplate.opsForValue().setIfAbsent(stockKey, "1", Duration.ofHours(2));
        if (Boolean.TRUE.equals(initialized)) {
            redisTemplate.opsForValue().set(stockKey, "1", Duration.ofHours(2));
        }
    }

    private String panicStockKey(String orderId) {
        return "panic:{" + orderId + "}:stock";
    }

    private String panicBuyerKey(String orderId, Long purchaserId) {
        return "panic:{" + orderId + "}:buyer:" + purchaserId;
    }

    private String transportClaimStockKey(String orderId) {
        return "transport:claim:{" + orderId + "}:stock";
    }

    private String transportClaimDriverKey(String orderId, Long driverId) {
        return "transport:claim:{" + orderId + "}:driver:" + driverId;
    }

    /**
     * 作用：把待异步落库的订单写入 Redis。
     * 输入：
     * - order：订单对象，包含采购方、供应商、物资和状态。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
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
        putIfPresent(fields, "originAddress", order.getOriginAddress());
        putIfPresent(fields, "originLongitude", decimalString(order.getOriginLongitude()));
        putIfPresent(fields, "originLatitude", decimalString(order.getOriginLatitude()));
        putIfPresent(fields, "destinationAddress", order.getDestinationAddress());
        putIfPresent(fields, "destinationLongitude", decimalString(order.getDestinationLongitude()));
        putIfPresent(fields, "destinationLatitude", decimalString(order.getDestinationLatitude()));
        fields.put("createTime", order.getCreateTime().toString());
        fields.put("updateTime", order.getUpdateTime().toString());
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, PENDING_ORDER_TTL);
    }

    private void putIfPresent(Map<String, String> fields, String key, String value) {
        if (StringUtils.hasText(value)) {
            fields.put(key, value);
        }
    }

    private String decimalString(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

}
