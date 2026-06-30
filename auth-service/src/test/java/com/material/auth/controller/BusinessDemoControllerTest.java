package com.material.auth.controller;

import com.material.auth.dto.business.DispatchRecommendationView;
import com.material.auth.dto.business.PurchaseOrderView;
import com.material.auth.dto.business.TransportLocationReportRequest;
import com.material.auth.dto.business.TransportLocationReportView;
import com.material.auth.service.impl.BusinessDemoService;
import com.material.common.constant.AuthConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessDemoControllerTest {
    private final BusinessDemoService businessDemoService = mock(BusinessDemoService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BusinessDemoController(businessDemoService))
            .build();

    /**
     * 作用：完成 driverCanClaimTransportOrderByOrderIdPathVariable 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void driverCanClaimTransportOrderByOrderIdPathVariable() throws Exception {
        String orderId = "PO-20260603-1001";
        when(businessDemoService.claimTransportOrder(1L, orderId)).thenReturn(new PurchaseOrderView(
                orderId,
                1L,
                "Shanghai Material Purchaser Co., Ltd.",
                1L,
                "Shanghai Reliable Supplier Co., Ltd.",
                101L,
                "HRB400E 抗震钢筋",
                "钢材",
                "100 吨",
                "¥ 302000",
                "司机已接单",
                "采购方确认购货后进入平台大厅",
                "司机 1 已抢单",
                1L,
                null,
                "2026-06-03 11:18",
                "待验收",
                "运输完成后由采购方验收签收",
                "",
                "待付款",
                "验收完成后由采购方登记付款凭证",
                "",
                "",
                null,
                null,
                null,
                null,
                null,
                null
        ));

        mockMvc.perform(post("/api/transport-orders/{orderId}/claim", orderId)
                        .header(AuthConstants.HEADER_USER_ID, 1L)
                        .header(AuthConstants.HEADER_USER_TYPE, "DRIVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("司机已接单"));
    }

    @Test
    void driverCanUploadTransportLocationByOrderIdPathVariable() throws Exception {
        String orderId = "PO-20260603-1001";
        when(businessDemoService.reportTransportLocation(eq(8L), eq(orderId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TransportLocationReportView(
                        99L,
                        orderId,
                        8L,
                        new BigDecimal("121.473701"),
                        new BigDecimal("31.230416"),
                        "到达中转点",
                        "2026-06-30 16:40"
                ));

        mockMvc.perform(post("/api/transport-orders/{orderId}/location", orderId)
                        .header(AuthConstants.HEADER_USER_ID, 8L)
                        .header(AuthConstants.HEADER_USER_TYPE, "DRIVER")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "longitude": 121.473701,
                                  "latitude": 31.230416,
                                  "remark": "到达中转点"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.driverId").value(8L))
                .andExpect(jsonPath("$.data.longitude").value(121.473701))
                .andExpect(jsonPath("$.data.latitude").value(31.230416))
                .andExpect(jsonPath("$.data.remark").value("到达中转点"));

        ArgumentCaptor<TransportLocationReportRequest> requestCaptor =
                ArgumentCaptor.forClass(TransportLocationReportRequest.class);
        verify(businessDemoService).reportTransportLocation(eq(8L), eq(orderId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().longitude()).isEqualByComparingTo("121.473701");
        assertThat(requestCaptor.getValue().latitude()).isEqualByComparingTo("31.230416");
        assertThat(requestCaptor.getValue().remark()).isEqualTo("到达中转点");
    }

    @Test
    void orderDispatchRecommendationsUseOrderPathVariableAndCurrentUser() throws Exception {
        String orderId = "PO-20260611-1001";
        when(businessDemoService.dispatchRecommendations(7L, "SUPPLIER", orderId)).thenReturn(List.of(
                new DispatchRecommendationView(
                        8L,
                        "李师傅",
                        "沪A-8899",
                        "4.2米厢式货车",
                        true,
                        new BigDecimal("0.21"),
                        "4.7",
                        new BigDecimal("166.79"),
                        "在线 · 距发货地 0.21 KM · 评分 4.7",
                        1
                )
        ));

        mockMvc.perform(get("/api/orders/{orderId}/dispatch-recommendations", orderId)
                        .header(AuthConstants.HEADER_USER_ID, 7L)
                        .header(AuthConstants.HEADER_USER_TYPE, "SUPPLIER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].driverId").value(8L))
                .andExpect(jsonPath("$.data[0].online").value(true))
                .andExpect(jsonPath("$.data[0].reason").value("在线 · 距发货地 0.21 KM · 评分 4.7"));
    }
}
