# Project Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the logistics coordination platform stable to start, verify, demo, and explain from a clean local checkout.

**Architecture:** Keep business behavior unchanged and add operational polish around it: deterministic local service registration, repeatable scripts, and a smoke test that exercises the web gateway and the new logistics route fields. Documentation points users to the scripts first, with manual commands kept as fallback.

**Tech Stack:** Java 21, Spring Boot, Spring Cloud Gateway, Nacos, MySQL, Redis, RabbitMQ, Bash, Python 3, native HTML/CSS/JavaScript.

---

### Task 1: Stabilize Local Nacos Registration

**Files:**
- Modify: `auth-service/src/main/resources/application.yml`
- Modify: `gateway-service/src/main/resources/application.yml`

- [ ] **Step 1: Set a local default discovery IP**

Add `ip: ${NACOS_DISCOVERY_IP:127.0.0.1}` under each service's `spring.cloud.nacos.discovery` block.

- [ ] **Step 2: Verify service logs**

Run each service and expect logs similar to:

```text
nacos registry, DEFAULT_GROUP auth-service 127.0.0.1:8081 register finished
nacos registry, DEFAULT_GROUP gateway-service 127.0.0.1:8080 register finished
```

### Task 2: Add Local Operations Scripts

**Files:**
- Create: `scripts/start-local.sh`
- Create: `scripts/stop-local.sh`
- Create: `scripts/smoke-test.sh`

- [ ] **Step 1: Add `scripts/start-local.sh`**

The script starts `auth-service`, `gateway-service`, and the frontend with logs under `.run/logs` and PID files under `.run/pids`.

- [ ] **Step 2: Add `scripts/stop-local.sh`**

The script stops processes recorded by PID files and reports any remaining local ports.

- [ ] **Step 3: Add `scripts/smoke-test.sh`**

The script logs in as `driver01`, verifies frontend reachability, verifies pushed transport orders, and checks that route coordinates are returned.

### Task 3: Update Startup Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/startup.md`

- [ ] **Step 1: Document the fast path**

Point users to:

```bash
scripts/start-local.sh
scripts/smoke-test.sh
```

- [ ] **Step 2: Document the Nacos IP issue**

Explain that local services default to `127.0.0.1` and can be overridden with `NACOS_DISCOVERY_IP`.

### Task 4: Verify End-to-End

**Files:**
- No code files.

- [ ] **Step 1: Run backend tests**

```bash
mvn -q test
```

- [ ] **Step 2: Run frontend tests**

```bash
node --test web-frontend/assets/app.test.js web-frontend/assets/js/selectors.test.js
```

- [ ] **Step 3: Run smoke test**

```bash
scripts/smoke-test.sh
```

Expected: all checks pass and the transport order response includes origin/destination coordinates.
