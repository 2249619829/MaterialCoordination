package com.material.auth.controller;

import com.material.auth.dto.business.DriverFollowView;
import com.material.auth.dto.business.DispatchRecommendationView;
import com.material.auth.dto.business.FulfillmentRankingsView;
import com.material.auth.dto.business.FollowRequest;
import com.material.auth.dto.business.AdminDashboardView;
import com.material.auth.dto.business.AdminSupplierAuditView;
import com.material.auth.dto.business.MaterialOptionView;
import com.material.auth.dto.business.DeadLetterStatsView;
import com.material.auth.dto.business.DriverAttendanceView;
import com.material.auth.dto.business.NearbySupplierView;
import com.material.auth.dto.business.NotificationView;
import com.material.auth.dto.business.OrderAcceptanceRequest;
import com.material.auth.dto.business.OrderPaymentRequest;
import com.material.auth.dto.business.OrderReviewRequest;
import com.material.auth.dto.business.OrderReviewView;
import com.material.auth.dto.business.OrderTimelineView;
import com.material.auth.dto.business.PurchaseCartCheckoutRequest;
import com.material.auth.dto.business.PurchaseOrderRequest;
import com.material.auth.dto.business.PurchaseOrderView;
import com.material.auth.dto.business.PurchaseRfqRequest;
import com.material.auth.dto.business.PurchaseRfqView;
import com.material.auth.dto.business.RfqQuoteView;
import com.material.auth.dto.business.SupplierCatalogView;
import com.material.auth.dto.business.SupplierMaterialManageRequest;
import com.material.auth.dto.business.SupplierMaterialManageView;
import com.material.auth.dto.business.SupplierQuoteRequest;
import com.material.auth.dto.business.SupplierQualificationRequest;
import com.material.auth.dto.business.SupplierQualificationView;
import com.material.auth.dto.business.SupplierRankingView;
import com.material.auth.dto.business.SupplierStoreView;
import com.material.auth.dto.business.TransportLocationReportRequest;
import com.material.auth.dto.business.TransportLocationReportView;
import com.material.auth.dto.business.TransportTrackingView;
import com.material.auth.security.AuthUserContext;
import com.material.auth.service.impl.BusinessDemoService;
import com.material.common.constant.AuthConstants;
import com.material.common.model.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BusinessDemoController {
    private final BusinessDemoService businessDemoService;

    /**
     * 作用：创建 BusinessDemoController 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - businessDemoService：business Demo 业务服务对象，类型是 BusinessDemoService；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public BusinessDemoController(BusinessDemoService businessDemoService) {
        this.businessDemoService = businessDemoService;
    }

    /**
     * 作用：查询采购方能看到的供应商目录。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Result<List<SupplierCatalogView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/suppliers/catalog")
    public Result<List<SupplierCatalogView>> supplierCatalog() {
        return Result.success(businessDemoService.supplierCatalog());
    }

    /**
     * 作用：查询某个供应商的店铺详情和物资列表。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 Result<SupplierStoreView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/suppliers/{supplierId}/store")
    public Result<SupplierStoreView> supplierStore(@PathVariable("supplierId") Long supplierId) {
        return Result.success(businessDemoService.supplierStore(supplierId));
    }

    /**
     * 作用：查询页面下拉框可选择的物资基础数据。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Result<List<MaterialOptionView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/materials/options")
    public Result<List<MaterialOptionView>> materialOptions() {
        return Result.success(businessDemoService.materialOptions());
    }

    /**
     * 作用：查询供应商履约评分排行榜。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Result<List<SupplierRankingView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/suppliers/ranking")
    public Result<List<SupplierRankingView>> supplierRanking() {
        return Result.success(businessDemoService.supplierRanking());
    }

    @GetMapping("/rankings/fulfillment")
    public Result<FulfillmentRankingsView> fulfillmentRankings() {
        return Result.success(businessDemoService.fulfillmentRankings());
    }

    /**
     * 作用：按照经纬度查找附近供应商。
     * 输入：
     * - longitude：经度，类型是 Double；方法会读取这个值继续处理。
     * - latitude：纬度，类型是 Double；方法会读取这个值继续处理。
     * - radiusKm：搜索半径，单位是公里；方法用它限制附近供应商的范围。
     * 输出：返回 Result<List<NearbySupplierView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/suppliers/nearby")
    public Result<List<NearbySupplierView>> nearbySuppliers(@RequestParam("longitude") Double longitude,
                                                            @RequestParam("latitude") Double latitude,
                                                            @RequestParam(value = "radiusKm", required = false) Double radiusKm) {
        return Result.success(businessDemoService.nearbySuppliers(longitude, latitude, radiusKm));
    }

    /**
     * 作用：查询某个供应商自己维护的供应物资。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<SupplierMaterialManageView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/supplier/materials")
    public Result<List<SupplierMaterialManageView>> supplierMaterials(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.supplierMaterials(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    @GetMapping("/supplier/qualification")
    public Result<SupplierQualificationView> supplierQualification(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                   @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                   @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.supplierQualification(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    @PutMapping("/supplier/qualification")
    public Result<SupplierQualificationView> updateSupplierQualification(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                         @RequestBody SupplierQualificationRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.updateSupplierQualification(user.requireRole(AuthUserContext.SUPPLIER), request));
    }

    /**
     * 作用：处理前端新增供应商物资的请求。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<SupplierMaterialManageView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/supplier/materials")
    public Result<SupplierMaterialManageView> createSupplierMaterial(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                     @RequestBody SupplierMaterialManageRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.saveSupplierMaterial(user.requireRole(AuthUserContext.SUPPLIER), request));
    }

    /**
     * 作用：修改一条供应商物资信息。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - id：编号，类型是 Long；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<SupplierMaterialManageView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PutMapping("/supplier/materials/{id}")
    public Result<SupplierMaterialManageView> updateSupplierMaterial(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                     @PathVariable("id") Long id,
                                                                     @RequestBody SupplierMaterialManageRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.updateSupplierMaterial(user.requireRole(AuthUserContext.SUPPLIER), id, request));
    }

    /**
     * 作用：把一条供应商物资设置为下架状态。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - id：编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 Result<SupplierMaterialManageView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/supplier/materials/{id}/offline")
    public Result<SupplierMaterialManageView> offlineSupplierMaterial(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                      @PathVariable("id") Long id) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.offlineSupplierMaterial(user.requireRole(AuthUserContext.SUPPLIER), id));
    }

    /**
     * 作用：创建一笔采购订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/purchase-orders")
    public Result<PurchaseOrderView> createPurchaseOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @RequestBody PurchaseOrderRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.createPurchaseOrder(user.requireRole(AuthUserContext.PURCHASER), request));
    }

    /**
     * 作用：把采购清单里的多项物资批量生成订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/purchase-orders/cart/checkout")
    public Result<List<PurchaseOrderView>> checkoutPurchaseCart(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                @RequestBody PurchaseCartCheckoutRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.checkoutPurchaseCart(user.requireRole(AuthUserContext.PURCHASER), request));
    }

    @PostMapping("/purchase-rfqs")
    public Result<PurchaseRfqView> createPurchaseRfq(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                     @RequestBody PurchaseRfqRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.createPurchaseRfq(user.requireRole(AuthUserContext.PURCHASER), request));
    }

    @GetMapping("/purchase-rfqs/mine")
    public Result<List<PurchaseRfqView>> purchaserRfqs(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                       @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                       @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.purchaserRfqs(user.requireRole(AuthUserContext.PURCHASER)));
    }

    @GetMapping("/purchase-rfqs/{rfqId}/quotes")
    public Result<List<RfqQuoteView>> purchaserRfqQuotes(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("rfqId") Long rfqId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.purchaserRfqQuotes(user.requireRole(AuthUserContext.PURCHASER), rfqId));
    }

    @PostMapping("/purchase-rfqs/quotes/{quoteId}/accept")
    public Result<PurchaseOrderView> acceptRfqQuote(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                    @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                    @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                    @PathVariable("quoteId") Long quoteId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.acceptRfqQuote(user.requireRole(AuthUserContext.PURCHASER), quoteId));
    }

    @GetMapping("/supplier/rfqs/open")
    public Result<List<PurchaseRfqView>> supplierOpenRfqs(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.openRfqsForSupplier(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    @GetMapping("/supplier/rfqs/quotes")
    public Result<List<RfqQuoteView>> supplierRfqQuotes(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.supplierRfqQuotes(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    @PostMapping("/supplier/rfqs/quotes")
    public Result<RfqQuoteView> quoteRfq(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                         @RequestBody SupplierQuoteRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.quoteRfq(user.requireRole(AuthUserContext.SUPPLIER), request));
    }

    /**
     * 作用：查询采购方自己的采购订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/purchase-orders/mine")
    public Result<List<PurchaseOrderView>> purchaserOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                           @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                           @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.purchaserOrders(user.requireRole(AuthUserContext.PURCHASER)));
    }

    @PostMapping("/purchase-orders/{orderId}/acceptance")
    public Result<PurchaseOrderView> acceptPurchaseOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId,
                                                         @RequestBody OrderAcceptanceRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.acceptPurchaseOrder(user.requireRole(AuthUserContext.PURCHASER), orderId, request));
    }

    @PostMapping("/purchase-orders/{orderId}/payment")
    public Result<PurchaseOrderView> payPurchaseOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                      @PathVariable("orderId") String orderId,
                                                      @RequestBody OrderPaymentRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.payPurchaseOrder(user.requireRole(AuthUserContext.PURCHASER), orderId, request));
    }

    /**
     * 作用：创建订单评价，并在评价供应商时刷新供应商评分。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<OrderReviewView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/orders/{orderId}/reviews")
    public Result<OrderReviewView> createOrderReview(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                     @PathVariable("orderId") String orderId,
                                                     @RequestBody OrderReviewRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.createOrderReview(user.userId(), user.userType(), orderId, request));
    }

    /**
     * 作用：查询一个订单下的所有评价。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<OrderReviewView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/orders/{orderId}/reviews")
    public Result<List<OrderReviewView>> orderReviews(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                      @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.orderReviews(user.userId(), user.userType(), orderId));
    }

    /**
     * 作用：查询一个订单的状态变化记录。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<OrderTimelineView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/orders/{orderId}/timeline")
    public Result<List<OrderTimelineView>> orderTimeline(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.orderTimeline(user.userId(), user.userType(), orderId));
    }

    @GetMapping("/transport-orders/{orderId}/tracking")
    public Result<TransportTrackingView> transportTracking(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                           @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                           @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                           @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.transportTracking(user.userId(), user.userType(), orderId));
    }

    @PostMapping("/transport-orders/{orderId}/location")
    public Result<TransportLocationReportView> reportTransportLocation(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                       @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                       @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                       @PathVariable("orderId") String orderId,
                                                                       @RequestBody TransportLocationReportRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.reportTransportLocation(user.requireRole(AuthUserContext.DRIVER), orderId, request));
    }

    @GetMapping("/orders/{orderId}/dispatch-recommendations")
    public Result<List<DispatchRecommendationView>> dispatchRecommendations(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                            @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                            @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                            @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.dispatchRecommendations(user.userId(), user.userType(), orderId));
    }

    /**
     * 作用：查询供应商自己的供货订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/supplier/orders")
    public Result<List<PurchaseOrderView>> supplierOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.supplierOrders(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    /**
     * 作用：供应商确认供货，并扣减库存。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/supplier/orders/{orderId}/confirm")
    public Result<PurchaseOrderView> confirmSupplierOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                          @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.confirmSupplierOrder(user.requireRole(AuthUserContext.SUPPLIER), orderId));
    }

    /**
     * 作用：供应商拒绝供货订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/supplier/orders/{orderId}/reject")
    public Result<PurchaseOrderView> rejectSupplierOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.rejectSupplierOrder(user.requireRole(AuthUserContext.SUPPLIER), orderId));
    }

    /**
     * 作用：查询等待司机接单的运输大厅订单。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/transport-orders/hall")
    public Result<List<PurchaseOrderView>> transportHall() {
        return Result.success(businessDemoService.transportHall());
    }

    /**
     * 作用：查询正在抢购中的订单资源。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/purchase-orders/panic-buy/hall")
    public Result<List<PurchaseOrderView>> panicBuyHall() {
        return Result.success(businessDemoService.panicBuyHall());
    }

    /**
     * 作用：采购方抢购一个处于抢购状态的订单资源。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/purchase-orders/{orderId}/panic-buy")
    public Result<PurchaseOrderView> panicBuyOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                   @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                   @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                   @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.panicBuyOrder(user.requireRole(AuthUserContext.PURCHASER), orderId));
    }

    /**
     * 作用：查询推送给某个司机的订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/transport-orders/push")
    public Result<List<PurchaseOrderView>> driverPushOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                            @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                            @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.driverPushOrders(user.requireRole(AuthUserContext.DRIVER)));
    }

    /**
     * 作用：查询司机已经接下的运输订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/transport-orders/mine")
    public Result<List<PurchaseOrderView>> driverOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.driverOrders(user.requireRole(AuthUserContext.DRIVER)));
    }

    /**
     * 作用：把司机收到的订单推送标记为已读。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/transport-orders/push/{orderId}/read")
    public Result<PurchaseOrderView> markPushRead(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                  @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                  @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                  @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.markPushRead(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    /**
     * 作用：司机抢下一个待接单的运输订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/transport-orders/{orderId}/claim")
    public Result<PurchaseOrderView> claimTransportOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.claimTransportOrder(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    /**
     * 作用：司机把订单状态改为运输中。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/transport-orders/{orderId}/start")
    public Result<PurchaseOrderView> startTransportOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.startTransportOrder(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    /**
     * 作用：司机把订单状态改为已完成。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<PurchaseOrderView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/transport-orders/{orderId}/complete")
    public Result<PurchaseOrderView> completeTransportOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                            @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                            @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                            @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.completeTransportOrder(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    /**
     * 作用：记录司机今天是否出勤。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - online：是否在线或出勤，true 表示在线，false 表示离线。
     * 输出：返回 Result<DriverAttendanceView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/drivers/attendance")
    public Result<DriverAttendanceView> markDriverAttendance(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                             @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                             @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                             @RequestParam(value = "online", defaultValue = "true") boolean online) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.markDriverAttendance(user.requireRole(AuthUserContext.DRIVER), online));
    }

    /**
     * 作用：查询司机今天是否出勤。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<DriverAttendanceView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/drivers/attendance/today")
    public Result<DriverAttendanceView> todayDriverAttendance(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                              @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                              @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.todayDriverAttendance(user.requireRole(AuthUserContext.DRIVER)));
    }

    /**
     * 作用：查询管理员首页需要展示的统计数据。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<AdminDashboardView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/admin/dashboard")
    public Result<AdminDashboardView> adminDashboard(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.adminDashboard());
    }

    /**
     * 作用：查询管理员审核供应商时使用的列表。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<AdminSupplierAuditView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/admin/suppliers")
    public Result<List<AdminSupplierAuditView>> adminSuppliers(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                               @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                               @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.adminSuppliers());
    }

    /**
     * 作用：把供应商账号改为审核通过状态。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 Result<AdminSupplierAuditView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/admin/suppliers/{supplierId}/approve")
    public Result<AdminSupplierAuditView> approveSupplier(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                          @PathVariable("supplierId") Long supplierId) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.approveSupplier(supplierId));
    }

    /**
     * 作用：把供应商账号改为审核拒绝或禁用状态。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：返回 Result<AdminSupplierAuditView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/admin/suppliers/{supplierId}/reject")
    public Result<AdminSupplierAuditView> rejectSupplier(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("supplierId") Long supplierId) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.rejectSupplier(supplierId));
    }

    /**
     * 作用：查询管理员能看到的全部订单。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<PurchaseOrderView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/admin/orders")
    public Result<List<PurchaseOrderView>> adminOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                       @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                       @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.adminOrders());
    }

    /**
     * 作用：为缺失推送记录的订单重新生成司机推送记录。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<Integer>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/order-push/retry")
    public Result<Integer> retryOrderPushRecords(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                 @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                 @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.retryOrderPushRecords());
    }

    /**
     * 作用：查询 RabbitMQ 死信队列中的消息数量。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<DeadLetterStatsView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/mq/dead-letters")
    public Result<List<DeadLetterStatsView>> deadLetterStats(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                             @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                             @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.deadLetterStats());
    }

    /**
     * 作用：查询司机关注了哪些采购方。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<DriverFollowView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/drivers/follows")
    public Result<List<DriverFollowView>> driverFollows(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.driverFollows(user.requireRole(AuthUserContext.DRIVER)));
    }

    /**
     * 作用：根据角色查询通知中心消息。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 Result<List<NotificationView>>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @GetMapping("/notifications")
    public Result<List<NotificationView>> notifications(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.notifications(user.userId(), user.userType()));
    }

    /**
     * 作用：让司机关注一个采购方。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 Result<DriverFollowView>，这是统一接口响应，里面包含状态码、提示信息和真正的数据。
     */
    @PostMapping("/drivers/follows")
    public Result<DriverFollowView> followPurchaser(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                    @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                    @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                    @RequestBody FollowRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.followPurchaser(user.requireRole(AuthUserContext.DRIVER), request.targetId()));
    }

    /**
     * 作用：根据登录 Token 或用户请求头读取当前登录用户。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 String；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 AuthUserContext，也就是这个方法处理后的结果。
     */
    private AuthUserContext currentUser(Long userId, String userType, String username) {
        return AuthUserContext.from(userId, userType, username);
    }

    /**
     * 作用：完成 requireOpsAccess 这一步处理。
     * 输入：
     * - user：User，类型是 AuthUserContext；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void requireOpsAccess(AuthUserContext user) {
        user.requireRole(AuthUserContext.ADMIN);
    }
}
