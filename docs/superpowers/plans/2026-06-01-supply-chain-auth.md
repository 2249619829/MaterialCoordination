# Supply Chain Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first-stage microservice foundation for the supply-chain material coordination platform and implement Redis-token account-password login.

**Architecture:** Use a Maven multi-module Spring Boot 3.5.x project with Java 21. `gateway-service` is the only public entrypoint, `auth-service` owns login/logout/current-user APIs, and `common-lib` shares DTOs, constants, enums, response wrappers, and exceptions. MySQL, Redis, RabbitMQ, and Nacos run locally through Docker Compose.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Cloud 2025.0.x, Spring Cloud Alibaba 2025.0.x, Spring Cloud Gateway, Nacos, MyBatis Plus, MySQL 8, Redis 7, RabbitMQ 3, Docker Compose, BCrypt.

---

## Project Root

All implementation work happens in:

```text
/Users/didi/Desktop/Material Coordination
```

The current spec lives at:

```text
/Users/didi/Desktop/Material Coordination/docs/superpowers/specs/2026-06-01-supply-chain-auth-design.md
```

## File Structure

Create or modify these files:

```text
/Users/didi/Desktop/Material Coordination/pom.xml
/Users/didi/Desktop/Material Coordination/docker-compose.yml
/Users/didi/Desktop/Material Coordination/docker/mysql/init/01_schema.sql
/Users/didi/Desktop/Material Coordination/common-lib/pom.xml
/Users/didi/Desktop/Material Coordination/common-lib/src/main/java/com/material/common/Result.java
/Users/didi/Desktop/Material Coordination/common-lib/src/main/java/com/material/common/ErrorCode.java
/Users/didi/Desktop/Material Coordination/common-lib/src/main/java/com/material/common/BusinessException.java
/Users/didi/Desktop/Material Coordination/common-lib/src/main/java/com/material/common/UserTypeEnum.java
/Users/didi/Desktop/Material Coordination/common-lib/src/main/java/com/material/common/LoginUserDTO.java
/Users/didi/Desktop/Material Coordination/common-lib/src/main/java/com/material/common/AuthConstants.java
/Users/didi/Desktop/Material Coordination/auth-service/pom.xml
/Users/didi/Desktop/Material Coordination/auth-service/src/main/resources/application.yml
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/AuthServiceApplication.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/controller/AuthController.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/service/AuthService.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/service/impl/AuthServiceImpl.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/dto/LoginRequest.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/dto/LoginResponse.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/entity/PurchaserAccount.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/entity/SupplierAccount.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/entity/DriverAccount.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/mapper/PurchaserAccountMapper.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/mapper/SupplierAccountMapper.java
/Users/didi/Desktop/Material Coordination/auth-service/src/main/java/com/material/auth/mapper/DriverAccountMapper.java
/Users/didi/Desktop/Material Coordination/gateway-service/pom.xml
/Users/didi/Desktop/Material Coordination/gateway-service/src/main/resources/application.yml
/Users/didi/Desktop/Material Coordination/gateway-service/src/main/java/com/material/gateway/GatewayServiceApplication.java
/Users/didi/Desktop/Material Coordination/gateway-service/src/main/java/com/material/gateway/filter/AuthGlobalFilter.java
```

## Task 1: Initialize Maven Multi-Module Project

**Files:**
- Create: `/Users/didi/Desktop/Material Coordination/pom.xml`
- Create: module directories for `common-lib`, `auth-service`, `gateway-service`

- [ ] **Step 1: Create the parent POM**

Create `/Users/didi/Desktop/Material Coordination/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.material</groupId>
    <artifactId>supply-chain-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>common-lib</module>
        <module>auth-service</module>
        <module>gateway-service</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.5.0</spring-boot.version>
        <spring-cloud.version>2025.0.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
        <mybatis-plus.version>3.5.12</mybatis-plus.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <release>${maven.compiler.release}</release>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: Verify Maven can read the project**

Run:

```bash
mvn -q -N help:effective-pom
```

Expected: command exits with code 0.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: initialize multi-module project"
```

## Task 2: Add Docker Compose and Database Schema

**Files:**
- Create: `/Users/didi/Desktop/Material Coordination/docker-compose.yml`
- Create: `/Users/didi/Desktop/Material Coordination/docker/mysql/init/01_schema.sql`

- [ ] **Step 1: Create Docker Compose**

Create `/Users/didi/Desktop/Material Coordination/docker-compose.yml`:

```yaml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: material_coordination
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - ./docker/mysql/init:/docker-entrypoint-initdb.d
      - material_mysql_data:/var/lib/mysql
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci

  redis:
    image: redis:7.2
    ports:
      - "6379:6379"
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - material_redis_data:/data

  rabbitmq:
    image: rabbitmq:3-management
    environment:
      RABBITMQ_DEFAULT_USER: material
      RABBITMQ_DEFAULT_PASS: material
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - material_rabbitmq_data:/var/lib/rabbitmq

  nacos:
    image: nacos/nacos-server:v2.5.1
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "false"
      JVM_XMS: 256m
      JVM_XMX: 512m
    ports:
      - "8848:8848"
      - "9848:9848"

volumes:
  material_mysql_data:
  material_redis_data:
  material_rabbitmq_data:
```

- [ ] **Step 2: Create database schema and seed data**

Create `/Users/didi/Desktop/Material Coordination/docker/mysql/init/01_schema.sql` with the agreed account/profile/material tables and seed one user of each type. Use BCrypt hash for plaintext password `123456`.

```sql
CREATE DATABASE IF NOT EXISTS material_coordination DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE material_coordination;

CREATE TABLE purchaser_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_purchaser_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchaser_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    purchaser_id BIGINT NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    address VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_purchaser_profile_account (purchaser_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    company_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    license_no VARCHAR(64) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    longitude DECIMAL(10, 6) DEFAULT NULL,
    latitude DECIMAL(10, 6) DEFAULT NULL,
    rating_score DECIMAL(4, 2) NOT NULL DEFAULT 5.00,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_profile_account (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE driver_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_driver_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE driver_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    driver_id BIGINT NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    vehicle_no VARCHAR(32) DEFAULT NULL,
    vehicle_type VARCHAR(32) DEFAULT NULL,
    longitude DECIMAL(10, 6) DEFAULT NULL,
    latitude DECIMAL(10, 6) DEFAULT NULL,
    attendance_status TINYINT NOT NULL DEFAULT 0,
    rating_score DECIMAL(4, 2) NOT NULL DEFAULT 5.00,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_driver_profile_account (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_material_code (material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE supplier_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    supply_price DECIMAL(12, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    daily_capacity INT NOT NULL DEFAULT 0,
    delivery_radius_km INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_supplier_material_supplier (supplier_id),
    KEY idx_supplier_material_material (material_id),
    UNIQUE KEY uk_supplier_material (supplier_id, material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO purchaser_account (id, username, password_hash, status)
VALUES (1, 'purchaser01', '$2a$10$7QJ8wHq4zYcY9wUcYhsAZOjUx2nTg8wEf3FyBzUJvXcCPt3KkZc7W', 1);
INSERT INTO purchaser_profile (purchaser_id, company_name, contact_name, contact_phone, address)
VALUES (1, '应急采购中心', '采购联系人', '13800000001', '上海市浦东新区');

INSERT INTO supplier_account (id, username, password_hash, status)
VALUES (1, 'supplier01', '$2a$10$7QJ8wHq4zYcY9wUcYhsAZOjUx2nTg8wEf3FyBzUJvXcCPt3KkZc7W', 1);
INSERT INTO supplier_profile (supplier_id, company_name, contact_name, contact_phone, license_no, address, longitude, latitude)
VALUES (1, '华东物资供应有限公司', '供应联系人', '13800000002', 'SUP-2026-001', '上海市闵行区', 121.381709, 31.112813);

INSERT INTO driver_account (id, username, password_hash, status)
VALUES (1, 'driver01', '$2a$10$7QJ8wHq4zYcY9wUcYhsAZOjUx2nTg8wEf3FyBzUJvXcCPt3KkZc7W', 1);
INSERT INTO driver_profile (driver_id, real_name, contact_phone, vehicle_no, vehicle_type, longitude, latitude)
VALUES (1, '张司机', '13800000003', '沪A12345', '厢式货车', 121.473701, 31.230416);

INSERT INTO material (id, material_code, material_name, category, unit, description)
VALUES (1, 'MASK-N95', 'N95口罩', '防护物资', '只', '应急防护口罩');
INSERT INTO supplier_material (supplier_id, material_id, supply_price, stock_quantity, daily_capacity, delivery_radius_km)
VALUES (1, 1, 2.50, 100000, 20000, 80);
```

- [ ] **Step 3: Start middleware**

Run:

```bash
docker compose up -d
```

Expected: containers `material-mysql`, `material-redis`, `material-rabbitmq`, and `material-nacos` are running.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml docker/mysql/init/01_schema.sql
git commit -m "chore: add local middleware compose"
```

## Task 3: Implement Common Lib

**Files:**
- Create: `/Users/didi/Desktop/Material Coordination/common-lib/pom.xml`
- Create Java shared classes under `common-lib/src/main/java/com/material/common`

- [ ] **Step 1: Create common-lib POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.material</groupId>
        <artifactId>supply-chain-platform</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>common-lib</artifactId>
    <packaging>jar</packaging>
</project>
```

- [ ] **Step 2: Add shared classes**

Create:

```java
package com.material.common;

public record Result<T>(int code, String message, T data) {
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> failed(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
```

Create `ErrorCode`, `BusinessException`, `UserTypeEnum`, `LoginUserDTO`, and `AuthConstants` with these required values:

```java
public enum UserTypeEnum {
    PURCHASER, SUPPLIER, DRIVER
}
```

```java
public final class AuthConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";
    public static final long LOGIN_TOKEN_TTL_MINUTES = 30;
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_TYPE = "X-User-Type";
    public static final String HEADER_USERNAME = "X-Username";

    private AuthConstants() {
    }
}
```

- [ ] **Step 3: Compile common-lib**

Run:

```bash
mvn -q -pl common-lib test
```

Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add common-lib
git commit -m "feat: add shared common library"
```

## Task 4: Implement Auth Service

**Files:**
- Create all Auth Service files listed in the File Structure section.

- [ ] **Step 1: Create auth-service POM**

Include dependencies:

```xml
<dependency>
    <groupId>com.material</groupId>
    <artifactId>common-lib</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

- [ ] **Step 2: Add application.yml**

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
  datasource:
    url: jdbc:mysql://localhost:3306/material_coordination?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

- [ ] **Step 3: Add login implementation**

Implement `AuthServiceImpl` so it:

- Validates `userType`, `username`, and `password`
- Selects the matching account table by `userType`
- Rejects missing, disabled, or password-mismatched users
- Generates `UUID.randomUUID().toString().replace("-", "")`
- Stores a Redis Hash at `login:token:{token}`
- Sets TTL to 30 minutes
- Returns `LoginResponse`

- [ ] **Step 4: Add logout and me**

`DELETE /auth/logout` deletes the current token key.  
`GET /auth/me` reads the current token from `Authorization` and returns Redis login data.

- [ ] **Step 5: Compile auth-service**

Run:

```bash
mvn -q -pl auth-service -am test
```

Expected: build succeeds.

- [ ] **Step 6: Commit**

```bash
git add auth-service
git commit -m "feat: implement redis token login"
```

## Task 5: Implement Gateway Service

**Files:**
- Create all Gateway Service files listed in the File Structure section.

- [ ] **Step 1: Create gateway-service POM**

Include dependencies:

```xml
<dependency>
    <groupId>com.material</groupId>
    <artifactId>common-lib</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

- [ ] **Step 2: Add application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      server:
        webflux:
          routes:
            - id: auth-service
              uri: lb://auth-service
              predicates:
                - Path=/auth/**
  data:
    redis:
      host: localhost
      port: 6379
```

- [ ] **Step 3: Add AuthGlobalFilter**

Implement a `GlobalFilter` that:

- Allows `POST /auth/login`
- Requires `Authorization: Bearer {token}` for all other requests
- Reads Redis Hash `login:token:{token}`
- Returns 401 if missing or expired
- Refreshes TTL to 30 minutes
- Adds `X-User-Id`, `X-User-Type`, and `X-Username` request headers before routing

- [ ] **Step 4: Compile gateway-service**

Run:

```bash
mvn -q -pl gateway-service -am test
```

Expected: build succeeds.

- [ ] **Step 5: Commit**

```bash
git add gateway-service
git commit -m "feat: add gateway token authentication"
```

## Task 6: End-to-End Verification

**Files:**
- No new files required.

- [ ] **Step 1: Start middleware**

Run:

```bash
docker compose up -d
```

Expected: all middleware containers are healthy or running.

- [ ] **Step 2: Start auth-service**

Run:

```bash
mvn -pl auth-service spring-boot:run
```

Expected: service starts on port 8081 and registers with Nacos.

- [ ] **Step 3: Start gateway-service**

Run in another terminal:

```bash
mvn -pl gateway-service spring-boot:run
```

Expected: service starts on port 8080 and registers with Nacos.

- [ ] **Step 4: Login through Gateway**

Run:

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userType":"SUPPLIER","username":"supplier01","password":"123456"}'
```

Expected: JSON result contains `code: 200` and a non-empty `token`.

- [ ] **Step 5: Verify current user**

Run:

```bash
curl -s http://localhost:8080/auth/me \
  -H "Authorization: Bearer ${TOKEN}"
```

Expected: JSON result contains `userType: SUPPLIER` and `username: supplier01`.

- [ ] **Step 6: Verify unauthorized request**

Run:

```bash
curl -i http://localhost:8080/auth/me
```

Expected: HTTP 401.

- [ ] **Step 7: Logout**

Run:

```bash
curl -i -X DELETE http://localhost:8080/auth/logout \
  -H "Authorization: Bearer ${TOKEN}"
```

Expected: HTTP 200.

- [ ] **Step 8: Verify token invalid after logout**

Run:

```bash
curl -i http://localhost:8080/auth/me \
  -H "Authorization: Bearer ${TOKEN}"
```

Expected: HTTP 401.

- [ ] **Step 9: Final commit**

```bash
git status --short
git commit --allow-empty -m "test: verify auth gateway flow"
```

## Self-Review

- Spec coverage: The plan covers local middleware, multi-module Spring project, Nacos registration, Gateway routing, Redis token login, multi-end login, MySQL tables, and first-stage verification.
- Scope check: Supplier read/write, high-concurrency order grabbing, RabbitMQ business queues, push platform, ZSet, GEO, BitMap, Redis Cluster, and cache consistency patterns are intentionally excluded from this stage.
- Placeholder scan: No TODO/TBD placeholders remain.
- Type consistency: User type values are consistently `PURCHASER`, `SUPPLIER`, and `DRIVER`; Redis key prefix is consistently `login:token:`.
