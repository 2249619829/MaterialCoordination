# Driver Location Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: implemented and verified on 2026-06-30.

**Goal:** Let drivers upload arrival-node coordinates from the browser and show those uploaded nodes in transport tracking.

**Architecture:** The browser uses Geolocation to collect longitude and latitude, then posts them to a new driver-only transport location endpoint. The backend persists every upload in `transport_location_report`, writes an order timeline event, and updates Redis GEO keys for the latest driver/order position. Tracking reads the existing order route plus the persisted upload records.

**Tech Stack:** Spring Boot, MyBatis Plus, MySQL, Redis GEO through `StringRedisTemplate`, vanilla JavaScript frontend, Node test runner, Maven/JUnit/Mockito.

---

### Task 1: Backend Location Report Model

**Files:**
- Create: `auth-service/src/main/java/com/material/auth/entity/TransportLocationReport.java`
- Create: `auth-service/src/main/java/com/material/auth/mapper/TransportLocationReportMapper.java`
- Create: `auth-service/src/main/java/com/material/auth/dto/business/TransportLocationReportRequest.java`
- Create: `auth-service/src/main/java/com/material/auth/dto/business/TransportLocationReportView.java`
- Modify: `auth-service/src/main/java/com/material/auth/dto/business/TransportTrackingView.java`
- Modify: `sql/init/01_schema.sql`
- Create: `sql/migrations/20260630_transport_location_report.sql`

- [x] Write a failing service test proving location upload persists a report, creates a timeline event, and updates Redis GEO.
- [x] Add the entity, mapper, request DTO, response view, and schema/migration.
- [x] Extend tracking response with `locationReports`.
- [x] Run the backend test and confirm it passes.

### Task 2: Backend Endpoint and Service

**Files:**
- Modify: `auth-service/src/main/java/com/material/auth/controller/BusinessDemoController.java`
- Modify: `auth-service/src/main/java/com/material/auth/service/impl/BusinessDemoService.java`
- Modify: `auth-service/src/test/java/com/material/auth/controller/BusinessDemoControllerTest.java`

- [x] Write a failing controller test for `POST /api/transport-orders/{orderId}/location`.
- [x] Implement controller binding and service method.
- [x] Enforce driver ownership and transport-stage status checks.
- [x] Keep MySQL as source of truth; Redis GEO failure should log and not block saved history.

### Task 3: Frontend Upload and Tracking Display

**Files:**
- Modify: `web-frontend/assets/app.js`
- Modify: `web-frontend/assets/js/views.js`
- Modify: `web-frontend/assets/styles.css`
- Modify: `web-frontend/assets/app.test.js`
- Modify: `web-frontend/index.html`

- [x] Write failing frontend tests for the driver upload button and tracking display of uploaded nodes.
- [x] Add `uploadTransportLocation(orderId)` using `navigator.geolocation.getCurrentPosition`.
- [x] Post `{ longitude, latitude, remark }` to the new endpoint.
- [x] Refresh role data and the open tracking modal after upload.
- [x] Render `locationReports` as highlighted driver-uploaded nodes in the tracking modal.

### Task 4: Docs and Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/iteration-history.md`

- [x] Document the new API and Redis GEO/MySQL responsibility split.
- [x] Run frontend Node tests.
- [x] Run targeted backend tests.
- [x] Run `scripts/smoke-test.sh`.
