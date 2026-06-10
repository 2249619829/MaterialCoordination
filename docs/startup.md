# 项目启动文档

本文档用于从零启动本项目，并提供最常见的验证和排障命令。

如果要迁移到 Windows 或 WSL2，优先看：[windows-setup.md](windows-setup.md)。

## 0. 最快启动流程

如果你的 MySQL、Redis、RabbitMQ、Nacos 已经在本机启动，可以直接按下面顺序开三个终端。

终端一：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
source use-java21.sh
mvn -q -pl auth-service spring-boot:run
```

终端二：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
source use-java21.sh
mvn -q -pl gateway-service spring-boot:run
```

终端三：

```bash
cd "/Users/didi/Desktop/MaterialCoordination/web-frontend"
npm run start
```

然后访问：

```text
http://localhost:5173
```

如果是第一次运行，先看第 4 节初始化数据库。

## 1. 前置条件

本机需要具备以下环境：

- JDK 21
- Maven
- MySQL 8.x
- Redis
- RabbitMQ
- Nacos
- Python 3

如果使用 Docker 启动中间件，还需要 Docker / Docker Compose。

## 2. 常用端口

| 服务 | 端口 |
| --- | --- |
| MySQL | `3306` |
| Redis | `6379` |
| RabbitMQ | `5672` |
| RabbitMQ 管理页面 | `15672` |
| Nacos | `8848` |
| Gateway Service | `8080` |
| Auth Service | `8081` |
| Frontend | `5173` |

## 3. 启动中间件

### 方式一：使用本机已有中间件

确认这些服务已经启动：

```bash
lsof -nP -iTCP:3306 -sTCP:LISTEN
lsof -nP -iTCP:6379 -sTCP:LISTEN
lsof -nP -iTCP:5672 -sTCP:LISTEN
lsof -nP -iTCP:8848 -sTCP:LISTEN
```

### 方式二：使用 Docker Compose

在项目根目录执行：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
docker compose up -d mysql redis rabbitmq nacos
```

查看容器状态：

```bash
docker compose ps
```

## 4. 初始化数据库

如果是第一次启动，先创建并初始化数据库。

本机 MySQL 常用命令：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
mysql -uroot -e "CREATE DATABASE IF NOT EXISTS material_coordination DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot material_coordination < sql/init/01_schema.sql
mysql -uroot material_coordination < sql/init/02_seed.sql
mysql -uroot material_coordination < sql/init/03_order_timeline.sql
```

如果 MySQL 有 root 密码，把 `mysql -uroot` 改成：

```bash
mysql -uroot -p
```

## 5. 启动后端

项目后端分两个服务：

- `auth-service`：登录、注册、订单、供应商、司机等核心业务。
- `gateway-service`：统一网关，前端请求默认先访问它。

建议开两个终端分别启动。

### 终端一：启动 Auth Service

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
source use-java21.sh
mvn -q -pl auth-service spring-boot:run
```

启动成功后可以看到类似日志：

```text
Tomcat started on port 8081
Started AuthServiceApplication
```

### 终端二：启动 Gateway Service

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
source use-java21.sh
mvn -q -pl gateway-service spring-boot:run
```

启动成功后可以看到类似日志：

```text
Netty started on port 8080
Started GatewayServiceApplication
```

## 6. 启动前端

再开一个终端：

```bash
cd "/Users/didi/Desktop/MaterialCoordination/web-frontend"
npm run start
```

也可以直接执行：

```bash
python3 -m http.server 5173
```

浏览器访问：

```text
http://localhost:5173
```

## 7. 验证项目是否启动成功

可以直接复制下面这组命令验证：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
curl -I http://localhost:5173
curl -s \
  -H 'Content-Type: application/json' \
  -d '{"username":"supplier01","password":"123456","userType":"SUPPLIER"}' \
  http://localhost:8080/auth/login
```

判断标准：

- `8080` 有进程监听：网关启动成功。
- `8081` 有进程监听：业务服务启动成功。
- `5173` 有进程监听：前端启动成功。
- `curl -I http://localhost:5173` 返回 `200`：前端页面能访问。
- 登录接口返回 `code: 200` 和 `token`：后端主链路可用。

### 检查端口

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

### 检查前端首页

```bash
curl -I http://localhost:5173
```

看到 `HTTP/1.0 200 OK` 或 `HTTP/1.1 200 OK` 即可。

### 检查登录接口

```bash
curl -s \
  -H 'Content-Type: application/json' \
  -d '{"username":"supplier01","password":"123456","userType":"SUPPLIER"}' \
  http://localhost:8080/auth/login
```

如果返回里有 `code: 200` 和 `token`，说明网关、业务服务、Redis、MySQL 基本都正常。

## 8. 演示账号

密码均为 `123456`。

| 角色 | 用户名 |
| --- | --- |
| 供应商 | `supplier01` |
| 采购方 | `purchaser01` |
| 司机 | `driver01` |

## 9. 常见问题

### 不确定项目现在是否已经启动

执行：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

如果能看到 `java` 监听 `8080`，说明 `gateway-service` 正在运行。

如果能看到 `java` 监听 `8081`，说明 `auth-service` 正在运行。

如果能看到 `Python` 或其他静态服务器监听 `5173`，说明前端正在运行。

### 端口被占用

查看占用进程：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
```

停止进程：

```bash
kill <PID>
```

### 登录失败

优先检查：

1. MySQL 是否启动。
2. `material_coordination` 数据库是否初始化。
3. `sql/init/02_seed.sql` 是否已导入。
4. Redis 是否启动。
5. 前端请求是否访问 `http://localhost:8080`。

### 网关访问业务接口失败

优先检查：

1. Nacos 是否启动在 `8848`。
2. `auth-service` 是否已经注册到 Nacos。
3. `gateway-service` 是否已经启动。
4. `auth-service` 是否监听 `8081`。

### RabbitMQ 连接失败

默认账号密码是：

```text
guest / guest
```

如果使用 `.env.example`，默认配置也是 `guest / guest`。

RabbitMQ 管理页面：

```text
http://localhost:15672
```

## 10. 停止项目

如果服务是在终端前台启动的，直接按：

```text
Ctrl + C
```

如果需要按端口停止：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:8081 -sTCP:LISTEN
lsof -nP -iTCP:5173 -sTCP:LISTEN
kill <PID>
```

如果中间件是 Docker Compose 启动的：

```bash
cd "/Users/didi/Desktop/MaterialCoordination"
docker compose down
```

如果想连数据卷一起删除：

```bash
docker compose down -v
```

注意：`docker compose down -v` 会删除 MySQL、Redis、RabbitMQ、Nacos 的本地数据。
