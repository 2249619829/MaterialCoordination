# Fleetbase-Inspired Logistics Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reframe the existing supply-chain project as an emergency logistics order coordination platform by adding origin/destination coordinates, order tracking, and high-concurrency driver claim flow.

**Architecture:** Keep the current Spring Boot/MyBatis/RabbitMQ/Redis application shape. Extend `purchase_order` as the transport order aggregate, add Fleetbase-style place fields, and upgrade driver order claiming to Redis Lua reservation plus RabbitMQ asynchronous persistence guarded by Redisson.

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis-Plus, MySQL, Redis Lua, RabbitMQ, Redisson, JUnit 5, Mockito.

---

### Task 1: Add Order Location Model

**Files:**
- Modify: `sql/init/01_schema.sql`
- Modify: `auth-service/src/main/java/com/material/auth/entity/PurchaseOrder.java`
- Modify: `auth-service/src/main/java/com/material/auth/dto/business/PurchaseOrderView.java`
- Modify: `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`
- Test: `auth-service/src/test/java/com/material/auth/service/BusinessDemoServicePersistenceTest.java`

- [x] Add failing tests that created orders expose supplier origin and purchaser destination coordinates.
- [x] Add `origin_*` and `destination_*` columns to `purchase_order`.
- [x] Populate location fields when building normal purchase orders and RFQ-derived orders.
- [x] Include location fields in Redis pending-order snapshots and order views.

### Task 2: Upgrade Driver Claim Flow

**Files:**
- Modify: `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`
- Modify: `auth-service/src/main/java/com/material/auth/mq/OrderClaimedConsumer.java`
- Test: `auth-service/src/test/java/com/material/auth/service/BusinessDemoServicePersistenceTest.java`
- Test: `auth-service/src/test/java/com/material/auth/mq/OrderClaimedConsumerTest.java`

- [x] Add failing tests that `claimTransportOrder` reserves in Redis and publishes MQ without direct MySQL assignment.
- [x] Add failing tests that the consumer handles `transport:{orderId}:{driverId}` messages idempotently.
- [x] Implement Redis Lua keys `transport:claim:stock:{orderId}` and `transport:claim:driver:{orderId}:{driverId}`.
- [x] Extend the consumer to support transport claim messages with Redisson lock and conditional MySQL update.

### Task 3: Add Tracking Projection

**Files:**
- Create: `auth-service/src/main/java/com/material/auth/dto/business/TransportTrackingView.java`
- Modify: `auth-service/src/main/java/com/material/auth/controller/BusinessDemoController.java`
- Modify: `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`
- Test: `auth-service/src/test/java/com/material/auth/controller/BusinessDemoControllerTest.java`

- [x] Add endpoint `GET /api/transport-orders/{orderId}/tracking`.
- [x] Return order origin, destination, assigned driver, status, and timeline events.

### Task 4: Update Project Narrative

**Files:**
- Modify: `README.md`
- Modify: `docs/architecture.md`
- Modify: `docs/api.md`

- [x] Update project name and background to emergency logistics.
- [x] Document Fleetbase-inspired order pool, places, tracking, and high-concurrency claim flow.
- [x] Keep local pressure-test caveats clear.

### Verification

- [x] Run `mvn -q -pl auth-service test`.
- [x] Run any frontend tests if frontend DTO assumptions changed.
