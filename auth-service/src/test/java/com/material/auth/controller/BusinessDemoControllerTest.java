package com.material.auth.controller;

import com.material.auth.dto.business.PurchaseOrderView;
import com.material.auth.service.impl.BusinessDemoService;
import com.material.common.constant.AuthConstants;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BusinessDemoControllerTest {
    private final BusinessDemoService businessDemoService = mock(BusinessDemoService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BusinessDemoController(businessDemoService))
            .build();

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
                "2026-06-03 11:18"
        ));

        mockMvc.perform(post("/api/transport-orders/{orderId}/claim", orderId)
                        .header(AuthConstants.HEADER_USER_ID, 1L)
                        .header(AuthConstants.HEADER_USER_TYPE, "DRIVER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("司机已接单"));
    }
}
