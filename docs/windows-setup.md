# Windows 迁移与启动说明

本文档用于把项目从 macOS 迁移到 Windows 后启动运行。推荐使用 WSL2 或 Docker Desktop，避免在纯 Windows 环境里分别安装 Redis、RabbitMQ、Nacos 等中间件。

## 1. 推荐方案

推荐：

- Windows 11
- WSL2 Ubuntu
- Docker Desktop，并启用 WSL2 backend
- JDK 21
- Maven
- Git
- Python 3

不推荐直接使用旧版 Windows Redis。Redis 生产环境通常跑在 Linux 上；在 Windows 开发机上建议通过 Docker 容器或 WSL2 使用。

## 2. 克隆项目

在 WSL2 里执行：

```bash
cd ~
git clone git@github.com:2249619829/MaterialCoordination.git
cd MaterialCoordination
```

如果 Windows 还没有配置 GitHub SSH key，也可以先使用 HTTPS：

```bash
git clone https://github.com/2249619829/MaterialCoordination.git
```

建议把项目放在 WSL2 文件系统里，例如 `~/MaterialCoordination`，不要长期放在 `/mnt/c/...`，否则 Maven 编译和文件扫描会慢。

## 3. 启动中间件

在项目根目录执行：

```bash
docker compose up -d mysql redis rabbitmq nacos
docker compose ps
```

Docker Compose 默认端口：

| 服务 | 地址 |
| --- | --- |
| MySQL | `127.0.0.1:3306` |
| Redis | `127.0.0.1:6379` |
| RabbitMQ | `127.0.0.1:5672` |
| RabbitMQ 管理页面 | `http://localhost:15672` |
| Nacos | `http://localhost:8848` |

默认账号密码：

| 服务 | 用户名 | 密码 |
| --- | --- | --- |
| MySQL | `root` | `root` |
| RabbitMQ | `guest` | `guest` |

## 4. 初始化数据库

Docker 第一次创建 MySQL 数据卷时会自动执行 `sql/init` 下的初始化脚本。

如果数据库已经存在，或者需要手动补初始化，执行：

```bash
mysql -h127.0.0.1 -uroot -proot -e "CREATE DATABASE IF NOT EXISTS material_coordination DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/01_schema.sql
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/02_seed.sql
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/03_order_timeline.sql
```

如果本机没有 `mysql` 命令，也可以进入 Docker 容器执行：

```bash
docker compose exec -T mysql mysql -uroot -proot material_coordination < sql/init/01_schema.sql
docker compose exec -T mysql mysql -uroot -proot material_coordination < sql/init/02_seed.sql
docker compose exec -T mysql mysql -uroot -proot material_coordination < sql/init/03_order_timeline.sql
```

## 5. 启动后端

Docker Compose 的 MySQL 默认密码是 `root`，所以启动后端前需要设置 `MYSQL_PASSWORD=root`。

同时建议固定 Nacos 注册 IP 为 `127.0.0.1`，避免 WSL2 或 Docker 网卡导致服务注册成不可访问的虚拟网卡地址。

终端一：

```bash
export MYSQL_PASSWORD=root
export SPRING_CLOUD_NACOS_DISCOVERY_IP=127.0.0.1
mvn -q -pl auth-service spring-boot:run
```

终端二：

```bash
export MYSQL_PASSWORD=root
export SPRING_CLOUD_NACOS_DISCOVERY_IP=127.0.0.1
mvn -q -pl gateway-service spring-boot:run
```

如果使用 PowerShell，而不是 WSL2 bash：

```powershell
$env:MYSQL_PASSWORD="root"
$env:SPRING_CLOUD_NACOS_DISCOVERY_IP="127.0.0.1"
mvn -q -pl auth-service spring-boot:run
```

网关服务：

```powershell
$env:MYSQL_PASSWORD="root"
$env:SPRING_CLOUD_NACOS_DISCOVERY_IP="127.0.0.1"
mvn -q -pl gateway-service spring-boot:run
```

## 6. 启动前端

WSL2：

```bash
cd web-frontend
python3 -m http.server 5173
```

PowerShell：

```powershell
cd web-frontend
python -m http.server 5173
```

浏览器访问：

```text
http://localhost:5173
```

## 7. 验证

```bash
curl -I http://localhost:5173
curl -s \
  -H 'Content-Type: application/json' \
  -d '{"username":"supplier01","password":"123456","userType":"SUPPLIER"}' \
  http://localhost:8080/auth/login
```

返回 `code: 200` 和 `token` 即代表前端、网关、业务服务、MySQL、Redis 基本链路正常。

演示账号密码均为 `123456`：

| 角色 | 用户名 |
| --- | --- |
| 供应商 | `supplier01` |
| 采购方 | `purchaser01` |
| 司机 | `driver01` |

## 8. 常见问题

### Redis 在 Windows 上怎么处理

建议使用 Docker Compose 里的 Redis 服务，不需要单独安装 Windows 版 Redis。

```bash
docker compose up -d redis
```

项目默认连接：

```text
localhost:6379
```

### 登录接口 500

优先检查数据库结构是否最新：

```bash
mysql -h127.0.0.1 -uroot -proot material_coordination < sql/init/01_schema.sql
```

`01_schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，不会删除已有数据。

### 网关访问接口超时或 500

检查 Nacos 中 `auth-service` 是否注册为 `127.0.0.1:8081`。如果注册成 WSL2 或 Docker 虚拟网卡地址，重启后端时加上：

```bash
export SPRING_CLOUD_NACOS_DISCOVERY_IP=127.0.0.1
```

### MySQL 连接失败

Docker Compose 默认 MySQL root 密码是 `root`，启动后端前确认：

```bash
export MYSQL_PASSWORD=root
```

### 停止服务

中间件：

```bash
docker compose down
```

如果要删除 Docker 数据卷：

```bash
docker compose down -v
```

注意：`docker compose down -v` 会删除 MySQL、Redis、RabbitMQ、Nacos 的本地数据。
