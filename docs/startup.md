# 项目启动文档

本文档用于从零启动本项目，并提供最常见的验证和排障命令。

如果要迁移到 Windows 或 WSL2，优先看：[windows-setup.md](windows-setup.md)。

## 0. 最快启动流程

### 新电脑使用 Docker 启动中间件

Docker Compose 只负责启动 MySQL、Redis Cluster、RabbitMQ、Nacos；后端和前端仍在本机运行，所以还需要 JDK 21、Maven 和 Python 3。

```bash
git clone https://github.com/2249619829/MaterialCoordination.git
cd MaterialCoordination

docker compose up -d
docker compose ps

export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1

scripts/start-local.sh
scripts/smoke-test.sh
```

注意：Docker Compose 默认 MySQL root 密码是 `root`，而项目后端默认 `MYSQL_PASSWORD` 为空；使用 Docker MySQL 时必须设置 `MYSQL_PASSWORD=root`。

如果 `scripts/start-local.sh` 提示 Java 路径不存在，先确认本机 `java -version` 是 21，再按本机 JDK 安装位置调整 `use-java21.sh`，或按第 5 节手动启动后端。

### 本机已有中间件

如果你的 MySQL、Redis Cluster、RabbitMQ、Nacos 已经在本机启动，推荐直接用脚本启动和验证：

```bash
cd MaterialCoordination
scripts/start-local.sh
scripts/start-openresty.sh
scripts/smoke-test.sh
```

脚本会启动：

- `auth-service`：`8081`
- `gateway-service`：`8080`
- `web-frontend`：`5173`
- `openresty`：`8088`

日志在：

```text
.run/logs/
```

停止应用：

```bash
scripts/stop-local.sh
scripts/stop-openresty.sh
```

在 Codex 或某些会自动回收后台进程的终端环境里，可以使用前台保活模式：

```bash
scripts/start-local.sh --keep-alive
```

这个模式下保持终端窗口打开，按 `Ctrl + C` 会调用停止脚本。

如果想手动启动，也可以按下面顺序开三个终端。

终端一：

```bash
cd MaterialCoordination
source use-java21.sh
mvn -q -pl auth-service spring-boot:run
```

终端二：

```bash
cd MaterialCoordination
source use-java21.sh
mvn -q -pl gateway-service spring-boot:run
```

终端三：

```bash
cd MaterialCoordination/web-frontend
npm run start
```

然后访问：

```text
http://localhost:5173
```

如果要走 OpenResty 统一入口和令牌桶限流，访问：

```text
http://127.0.0.1:8088
```

如果是第一次运行，先看第 4 节初始化数据库。

## 1. 前置条件

本机完整本地运行需要具备以下环境：

- JDK 21
- Maven
- MySQL 8.x
- Redis Cluster
- RabbitMQ
- Nacos
- Python 3
- OpenResty

如果使用 Docker 启动中间件，本机可以不单独安装 MySQL、Redis、RabbitMQ、Nacos，但仍需要 JDK 21、Maven、Python 3 和 Docker / Docker Compose。OpenResty 是可选入口，不影响主流程启动。

## 2. 常用端口

| 服务 | 端口 |
| --- | --- |
| MySQL | `3306` |
| Redis Cluster | `6379-6384` |
| Redis Cluster bus | `16379-16384` |
| RabbitMQ | `5672` |
| RabbitMQ 管理页面 | `15672` |
| Nacos | `8848` |
| Gateway Service | `8080` |
| Auth Service | `8081` |
| Frontend | `5173` |
| OpenResty 统一入口 | `8088` |

## 3. 启动中间件

### 方式一：使用本机已有中间件

确认这些服务已经启动：

```bash
lsof -nP -iTCP:3306 -sTCP:LISTEN
lsof -nP -iTCP:6379 -sTCP:LISTEN
lsof -nP -iTCP:6384 -sTCP:LISTEN
lsof -nP -iTCP:5672 -sTCP:LISTEN
lsof -nP -iTCP:8848 -sTCP:LISTEN
```

### 方式二：使用 Docker Compose

在项目根目录执行：

```bash
cd MaterialCoordination
docker compose up -d
```

查看容器状态：

```bash
docker compose ps
```

Docker Compose 默认账号密码：

| 服务 | 用户名 | 密码 |
| --- | --- | --- |
| MySQL | `root` | `root` |
| RabbitMQ | `guest` | `guest` |

Redis Cluster 默认由 `redis-node-1` 到 `redis-node-6` 组成 3 主 3 从，应用默认读取：

```bash
REDIS_CLUSTER_NODES=localhost:6379,localhost:6380,localhost:6381,localhost:6382,localhost:6383,localhost:6384
```

使用 Docker MySQL 启动后端前，需要在启动后端的终端里设置：

```bash
export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1
```

## 4. 初始化数据库

如果是第一次启动，先创建并初始化数据库。

如果是 Docker Compose 第一次创建 MySQL 数据卷，`docker-compose.yml` 会自动执行 `sql/init` 下的初始化脚本。也可以手动检查或补导入：

```bash
mysql -h127.0.0.1 -uroot -proot -e "CREATE DATABASE IF NOT EXISTS material_coordination DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/01_schema.sql
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/02_seed.sql
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/03_order_timeline.sql
```

如果本机没有 `mysql` 命令，可以进入 Docker 容器执行：

```bash
docker compose exec -T mysql mysql -uroot -proot material_coordination < sql/init/01_schema.sql
docker compose exec -T mysql mysql -uroot -proot material_coordination < sql/init/02_seed.sql
docker compose exec -T mysql mysql -uroot -proot material_coordination < sql/init/03_order_timeline.sql
```

本机 MySQL 常用命令：

```bash
cd MaterialCoordination
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

如果中间件由 Docker Compose 启动，建议使用：

```bash
export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1
scripts/start-local.sh
```

如果使用本机无密码 MySQL，直接执行：

```bash
scripts/start-local.sh
```

如果本机 MySQL 有其他密码，按实际密码设置 `MYSQL_PASSWORD`。

如果手动启动，开两个终端分别执行下面命令。

### 终端一：启动 Auth Service

```bash
cd MaterialCoordination
export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1
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
cd MaterialCoordination
export MYSQL_PASSWORD=root
export NACOS_DISCOVERY_IP=127.0.0.1
source use-java21.sh
mvn -q -pl gateway-service spring-boot:run
```

启动成功后可以看到类似日志：

```text
Netty started on port 8080
Started GatewayServiceApplication
```

### Nacos 本机 IP 说明

本地开发默认把服务注册到 Nacos 的 IP 固定为 `127.0.0.1`：

```yaml
spring.cloud.nacos.discovery.ip: ${NACOS_DISCOVERY_IP:127.0.0.1}
```

这样可以避免 Mac 切换网络后，网关从 Nacos 拿到已经失效的内网 IP，导致访问 `/auth/login` 超时。

如果要让同局域网其他设备访问后端，可以按实际机器 IP 覆盖：

```bash
export NACOS_DISCOVERY_IP=192.168.x.x
```

## 6. 启动前端

再开一个终端：

```bash
cd MaterialCoordination/web-frontend
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

## 7. 启动 OpenResty 统一入口

OpenResty 是带 Lua 能力的 Nginx。本项目用它做第一层入口限流：

```text
浏览器 -> OpenResty:8088 -> gateway-service:8080 -> auth-service:8081
```

首次使用先安装 OpenResty 和令牌桶 Lua 包：

```bash
brew tap openresty/brew
brew trust openresty/brew
brew install openresty/brew/openresty --without-geoip
opm get upyun/lua-resty-limit-rate
```

启动 OpenResty：

```bash
cd MaterialCoordination
scripts/start-openresty.sh
```

访问统一入口：

```text
http://127.0.0.1:8088
```

这个地址会直接展示前端页面。页面里的 `/auth/**` 和 `/api/**` 请求会先经过 OpenResty 的令牌桶限流，再转发给 Gateway。

OpenResty 当前分三档限流：

| 接口类型 | 默认令牌速度 | 默认桶容量 | 说明 |
| --- | --- | --- | --- |
| 登录、注册 | `1/s` | `3` | 防止暴力尝试密码 |
| 抢购、抢单 | `1/s` | `2` | 保护高并发核心接口 |
| 普通接口 | `20/s` | `40` | 允许页面加载时的合理突发 |

如果要调整速度，可以设置环境变量后再启动：

```bash
export OPENRESTY_PORT=8088
export GATEWAY_HOST=127.0.0.1
export GATEWAY_PORT=8080
export RATE_LIMIT_AUTH_REPLENISH_RATE=1
export RATE_LIMIT_AUTH_BURST_CAPACITY=3
export RATE_LIMIT_SENSITIVE_REPLENISH_RATE=1
export RATE_LIMIT_SENSITIVE_BURST_CAPACITY=2
export RATE_LIMIT_API_REPLENISH_RATE=20
export RATE_LIMIT_API_BURST_CAPACITY=40
scripts/start-openresty.sh
```

停止 OpenResty：

```bash
scripts/stop-openresty.sh
```

## 8. 验证项目是否启动成功

推荐直接执行：

```bash
scripts/smoke-test.sh
```

脚本会验证：

- 前端首页可访问。
- 司机账号能通过网关登录。
- 司机运输单接口返回发货地、目的地和经纬度。
- 运输追踪接口返回路线信息。

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

如果要验证 OpenResty 入口，把脚本的地址改成 `8088`：

```bash
FRONTEND_URL=http://127.0.0.1:8088 API_BASE=http://127.0.0.1:8088 scripts/smoke-test.sh
```

判断标准：

- `8080` 有进程监听：网关启动成功。
- `8081` 有进程监听：业务服务启动成功。
- `5173` 有进程监听：前端启动成功。
- `curl -I http://localhost:5173` 返回 `200`：前端页面能访问。
- 登录接口返回 `code: 200` 和 `token`：后端主链路可用。
- `curl -I http://127.0.0.1:8088` 返回 `200`：OpenResty 统一入口可用。

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

## 9. 演示账号

密码均为 `123456`。

| 角色 | 用户名 |
| --- | --- |
| 供应商 | `supplier01` |
| 采购方 | `purchaser01` |
| 司机 | `driver01` |

## 10. 常见问题

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

如果 `auth-service` 自己能访问，但通过网关访问超时，通常是 Nacos 里有旧 IP。直接重启应用：

```bash
scripts/stop-local.sh
scripts/start-local.sh
scripts/smoke-test.sh
```

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

## 11. 停止项目

如果使用脚本启动，执行：

```bash
scripts/stop-local.sh
```

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
cd MaterialCoordination
docker compose down
```

如果想连数据卷一起删除：

```bash
docker compose down -v
```

注意：`docker compose down -v` 会删除 MySQL、Redis Cluster、RabbitMQ、Nacos 的本地数据。
