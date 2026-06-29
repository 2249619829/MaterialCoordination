package com.material.auth.mq;

import com.material.auth.entity.OrderPushRecord;
import com.material.auth.entity.OrderTimeline;
import com.material.auth.entity.PurchaseOrder;
import com.material.auth.mapper.OrderPushRecordMapper;
import com.material.auth.mapper.OrderTimelineMapper;
import com.material.auth.mapper.PurchaseOrderMapper;
import com.material.auth.mapper.PurchaserProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderClaimedConsumerTest {
    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private PurchaserProfileMapper purchaserProfileMapper;
    @Mock
    private OrderPushRecordMapper orderPushRecordMapper;
    @Mock
    private OrderTimelineMapper orderTimelineMapper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    @Test
    void transportClaimMessageBindsDriverUpdatesPushRecordAndWritesTimeline() throws Exception {
        OrderClaimedConsumer consumer = new OrderClaimedConsumer(
                purchaseOrderMapper,
                purchaserProfileMapper,
                orderPushRecordMapper,
                orderTimelineMapper,
                redissonClient
        );
        PurchaseOrder order = new PurchaseOrder();
        order.setId("PO-TRANSPORT-001");
        order.setStatus("待司机接单");
        OrderPushRecord pushRecord = new OrderPushRecord();
        pushRecord.setOrderId("PO-TRANSPORT-001");
        pushRecord.setDriverId(8L);
        pushRecord.setStatus("PENDING");
        when(redissonClient.getLock("lock:transport:claim:PO-TRANSPORT-001")).thenReturn(lock);
        when(lock.tryLock(3, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(purchaseOrderMapper.selectById("PO-TRANSPORT-001")).thenReturn(order);
        when(purchaseOrderMapper.update(any(PurchaseOrder.class), any())).thenReturn(1);
        when(orderPushRecordMapper.selectOne(any())).thenReturn(pushRecord);

        consumer.handleOrderClaimed("transport:PO-TRANSPORT-001:8");

        ArgumentCaptor<PurchaseOrder> orderCaptor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderMapper).update(orderCaptor.capture(), any());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("司机已接单");
        assertThat(orderCaptor.getValue().getDriverId()).isEqualTo(8L);
        assertThat(pushRecord.getStatus()).isEqualTo("CLAIMED");
        verify(orderPushRecordMapper).updateById(pushRecord);
        ArgumentCaptor<OrderTimeline> timelineCaptor = ArgumentCaptor.forClass(OrderTimeline.class);
        verify(orderTimelineMapper).insert(timelineCaptor.capture());
        assertThat(timelineCaptor.getValue().getAction()).isEqualTo("司机抢运输单异步落库");
        verify(lock).unlock();
    }
}
