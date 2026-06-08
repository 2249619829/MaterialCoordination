package com.material.auth.controller;

import com.material.auth.dto.business.DriverFollowView;
import com.material.auth.dto.business.FollowRequest;
import com.material.auth.dto.business.AdminDashboardView;
import com.material.auth.dto.business.AdminSupplierAuditView;
import com.material.auth.dto.business.MaterialOptionView;
import com.material.auth.dto.business.DeadLetterStatsView;
import com.material.auth.dto.business.DriverAttendanceView;
import com.material.auth.dto.business.NearbySupplierView;
import com.material.auth.dto.business.NotificationView;
import com.material.auth.dto.business.OrderReviewRequest;
import com.material.auth.dto.business.OrderReviewView;
import com.material.auth.dto.business.OrderTimelineView;
import com.material.auth.dto.business.PurchaseCartCheckoutRequest;
import com.material.auth.dto.business.PurchaseOrderRequest;
import com.material.auth.dto.business.PurchaseOrderView;
import com.material.auth.dto.business.SupplierCatalogView;
import com.material.auth.dto.business.SupplierMaterialManageRequest;
import com.material.auth.dto.business.SupplierMaterialManageView;
import com.material.auth.dto.business.SupplierRankingView;
import com.material.auth.dto.business.SupplierStoreView;
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

    public BusinessDemoController(BusinessDemoService businessDemoService) {
        this.businessDemoService = businessDemoService;
    }

    @GetMapping("/suppliers/catalog")
    public Result<List<SupplierCatalogView>> supplierCatalog() {
        return Result.success(businessDemoService.supplierCatalog());
    }

    @GetMapping("/suppliers/{supplierId}/store")
    public Result<SupplierStoreView> supplierStore(@PathVariable("supplierId") Long supplierId) {
        return Result.success(businessDemoService.supplierStore(supplierId));
    }

    @GetMapping("/materials/options")
    public Result<List<MaterialOptionView>> materialOptions() {
        return Result.success(businessDemoService.materialOptions());
    }

    @GetMapping("/suppliers/ranking")
    public Result<List<SupplierRankingView>> supplierRanking() {
        return Result.success(businessDemoService.supplierRanking());
    }

    @GetMapping("/suppliers/nearby")
    public Result<List<NearbySupplierView>> nearbySuppliers(@RequestParam("longitude") Double longitude,
                                                            @RequestParam("latitude") Double latitude,
                                                            @RequestParam(value = "radiusKm", required = false) Double radiusKm) {
        return Result.success(businessDemoService.nearbySuppliers(longitude, latitude, radiusKm));
    }

    @GetMapping("/supplier/materials")
    public Result<List<SupplierMaterialManageView>> supplierMaterials(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.supplierMaterials(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    @PostMapping("/supplier/materials")
    public Result<SupplierMaterialManageView> createSupplierMaterial(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                     @RequestBody SupplierMaterialManageRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.saveSupplierMaterial(user.requireRole(AuthUserContext.SUPPLIER), request));
    }

    @PutMapping("/supplier/materials/{id}")
    public Result<SupplierMaterialManageView> updateSupplierMaterial(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                     @PathVariable("id") Long id,
                                                                     @RequestBody SupplierMaterialManageRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.updateSupplierMaterial(user.requireRole(AuthUserContext.SUPPLIER), id, request));
    }

    @PostMapping("/supplier/materials/{id}/offline")
    public Result<SupplierMaterialManageView> offlineSupplierMaterial(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                      @PathVariable("id") Long id) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.offlineSupplierMaterial(user.requireRole(AuthUserContext.SUPPLIER), id));
    }

    @PostMapping("/purchase-orders")
    public Result<PurchaseOrderView> createPurchaseOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @RequestBody PurchaseOrderRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.createPurchaseOrder(user.requireRole(AuthUserContext.PURCHASER), request));
    }

    @PostMapping("/purchase-orders/cart/checkout")
    public Result<List<PurchaseOrderView>> checkoutPurchaseCart(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                                @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                                @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                                @RequestBody PurchaseCartCheckoutRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.checkoutPurchaseCart(user.requireRole(AuthUserContext.PURCHASER), request));
    }

    @GetMapping("/purchase-orders/mine")
    public Result<List<PurchaseOrderView>> purchaserOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                           @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                           @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.purchaserOrders(user.requireRole(AuthUserContext.PURCHASER)));
    }

    @PostMapping("/orders/{orderId}/reviews")
    public Result<OrderReviewView> createOrderReview(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                     @PathVariable("orderId") String orderId,
                                                     @RequestBody OrderReviewRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.createOrderReview(user.userId(), user.userType(), orderId, request));
    }

    @GetMapping("/orders/{orderId}/reviews")
    public Result<List<OrderReviewView>> orderReviews(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                      @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                      @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                      @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.orderReviews(user.userId(), user.userType(), orderId));
    }

    @GetMapping("/orders/{orderId}/timeline")
    public Result<List<OrderTimelineView>> orderTimeline(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.orderTimeline(user.userId(), user.userType(), orderId));
    }

    @GetMapping("/supplier/orders")
    public Result<List<PurchaseOrderView>> supplierOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.supplierOrders(user.requireRole(AuthUserContext.SUPPLIER)));
    }

    @PostMapping("/supplier/orders/{orderId}/confirm")
    public Result<PurchaseOrderView> confirmSupplierOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                          @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.confirmSupplierOrder(user.requireRole(AuthUserContext.SUPPLIER), orderId));
    }

    @PostMapping("/supplier/orders/{orderId}/reject")
    public Result<PurchaseOrderView> rejectSupplierOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.rejectSupplierOrder(user.requireRole(AuthUserContext.SUPPLIER), orderId));
    }

    @GetMapping("/transport-orders/hall")
    public Result<List<PurchaseOrderView>> transportHall() {
        return Result.success(businessDemoService.transportHall());
    }

    @GetMapping("/purchase-orders/panic-buy/hall")
    public Result<List<PurchaseOrderView>> panicBuyHall() {
        return Result.success(businessDemoService.panicBuyHall());
    }

    @PostMapping("/purchase-orders/{orderId}/panic-buy")
    public Result<PurchaseOrderView> panicBuyOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                   @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                   @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                   @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.panicBuyOrder(user.requireRole(AuthUserContext.PURCHASER), orderId));
    }

    @GetMapping("/transport-orders/push")
    public Result<List<PurchaseOrderView>> driverPushOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                            @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                            @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.driverPushOrders(user.requireRole(AuthUserContext.DRIVER)));
    }

    @GetMapping("/transport-orders/mine")
    public Result<List<PurchaseOrderView>> driverOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.driverOrders(user.requireRole(AuthUserContext.DRIVER)));
    }

    @PostMapping("/transport-orders/push/{orderId}/read")
    public Result<PurchaseOrderView> markPushRead(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                  @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                  @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                  @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.markPushRead(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    @PostMapping("/transport-orders/{orderId}/claim")
    public Result<PurchaseOrderView> claimTransportOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.claimTransportOrder(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    @PostMapping("/transport-orders/{orderId}/start")
    public Result<PurchaseOrderView> startTransportOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.startTransportOrder(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    @PostMapping("/transport-orders/{orderId}/complete")
    public Result<PurchaseOrderView> completeTransportOrder(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                            @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                            @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                            @PathVariable("orderId") String orderId) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.completeTransportOrder(user.requireRole(AuthUserContext.DRIVER), orderId));
    }

    @PostMapping("/drivers/attendance")
    public Result<DriverAttendanceView> markDriverAttendance(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                             @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                             @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                             @RequestParam(value = "online", defaultValue = "true") boolean online) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.markDriverAttendance(user.requireRole(AuthUserContext.DRIVER), online));
    }

    @GetMapping("/drivers/attendance/today")
    public Result<DriverAttendanceView> todayDriverAttendance(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                              @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                              @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.todayDriverAttendance(user.requireRole(AuthUserContext.DRIVER)));
    }

    @GetMapping("/admin/dashboard")
    public Result<AdminDashboardView> adminDashboard(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                     @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                     @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.adminDashboard());
    }

    @GetMapping("/admin/suppliers")
    public Result<List<AdminSupplierAuditView>> adminSuppliers(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                               @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                               @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.adminSuppliers());
    }

    @PostMapping("/admin/suppliers/{supplierId}/approve")
    public Result<AdminSupplierAuditView> approveSupplier(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                          @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                          @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                          @PathVariable("supplierId") Long supplierId) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.approveSupplier(supplierId));
    }

    @PostMapping("/admin/suppliers/{supplierId}/reject")
    public Result<AdminSupplierAuditView> rejectSupplier(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                         @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                         @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                         @PathVariable("supplierId") Long supplierId) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.rejectSupplier(supplierId));
    }

    @GetMapping("/admin/orders")
    public Result<List<PurchaseOrderView>> adminOrders(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                       @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                       @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.adminOrders());
    }

    @PostMapping("/order-push/retry")
    public Result<Integer> retryOrderPushRecords(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                 @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                 @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.retryOrderPushRecords());
    }

    @GetMapping("/mq/dead-letters")
    public Result<List<DeadLetterStatsView>> deadLetterStats(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                             @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                             @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        requireOpsAccess(user);
        return Result.success(businessDemoService.deadLetterStats());
    }

    @GetMapping("/drivers/follows")
    public Result<List<DriverFollowView>> driverFollows(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.driverFollows(user.requireRole(AuthUserContext.DRIVER)));
    }

    @GetMapping("/notifications")
    public Result<List<NotificationView>> notifications(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                        @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                        @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.notifications(user.userId(), user.userType()));
    }

    @PostMapping("/drivers/follows")
    public Result<DriverFollowView> followPurchaser(@RequestHeader(AuthConstants.HEADER_USER_ID) Long userId,
                                                    @RequestHeader(AuthConstants.HEADER_USER_TYPE) String userType,
                                                    @RequestHeader(value = AuthConstants.HEADER_USERNAME, required = false) String username,
                                                    @RequestBody FollowRequest request) {
        AuthUserContext user = currentUser(userId, userType, username);
        return Result.success(businessDemoService.followPurchaser(user.requireRole(AuthUserContext.DRIVER), request.targetId()));
    }

    private AuthUserContext currentUser(Long userId, String userType, String username) {
        return AuthUserContext.from(userId, userType, username);
    }

    private void requireOpsAccess(AuthUserContext user) {
        user.requireRole(AuthUserContext.ADMIN);
    }
}
