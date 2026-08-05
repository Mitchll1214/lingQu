# 灵渠 · 数据接口平台（Lingqu API Platform）

> 将 SQL 转化为 RESTful API 的低代码平台：多项目管理、独立路由、项目级鉴权、单容器部署、调用日志全量保留。

## 架构总览

```
┌─────────────────────────────── 单容器 ───────────────────────────────┐
│                        Supervisor（进程管理）                         │
│  ┌────────────────────────┐        ┌──────────────────────────────┐  │
│  │ Manager（8081）        │        │ Executor（8080，合并Gateway） │  │
│  │  · 内嵌 Vue3 前端 UI   │        │  · 路由识别（首段路径匹配）   │  │
│  │  · 项目/接口/数据源/   │        │  · 项目鉴权（none/token/     │  │
│  │    Token/日志管理      │        │    apikey）                  │  │
│  │  · Flyway 建表迁移     │        │  · 本地令牌桶限流（Guava）    │  │
│  └──────────┬─────────────┘        │  · MyBatis 动态 SQL 执行      │  │
│             │                      │  · Groovy 脚本执行            │  │
│             │                      │  · 调用日志写入               │  │
│             └──────────┬───────────┘                               │
└────────────────────────┼────────────────────────────────────────────┘
                         │ JDBC
              ┌──────────▼──────────┐      ┌──────────────────────┐
              │ 外部配置库           │      │ 业务数据源（用户配置） │
              │ MySQL 5.7+/PG 12+   │◄─────│ MySQL/PG/Oracle/达梦等 │
              └─────────────────────┘      └──────────────────────┘
```

- **Manager**：Web 管理后台（8081），内嵌前端静态资源，负责全部管理操作（登录、项目、数据源、接口、Token、日志查询、概览）。
- **Executor**：业务 API 执行引擎（8080），合并 Gateway 职责（路由 + 鉴权 + 限流 + SQL 执行）。
- **配置库**：外部 MySQL/PostgreSQL，存储项目、数据源、接口、Token、调用日志、用户。**不开发**独立 Gateway / Sentinel / Redis / MCP / 服务发现 / H2。

## 目录结构

```
├── manager/            Manager 管理后台（Spring Boot 2.7 + MyBatis-Plus）
│   ├── src/main/resources/db/migration/
│   │   ├── mysql/       MySQL 建表脚本（Flyway，{vendor} 自动选择）
│   │   └── postgresql/  PostgreSQL 建表脚本
│   └── src/main/resources/static/   前端构建产物（内嵌托管）
├── executor/           Executor 执行引擎（路由/鉴权/限流/SQL 引擎）
├── frontend/           Vue 3 + Element Plus 前端工程
├── docker/             Dockerfile + supervisord.conf + entrypoint + compose
├── scripts/            build.ps1 / build.sh 一键构建
└── 本文件
```

## 快速开始

### 方式一：本地开发运行

**1. 准备配置库**（外部 MySQL 5.7+ 或 PostgreSQL 12+），例如 MySQL：

```sql
CREATE DATABASE lingqu DEFAULT CHARACTER SET utf8mb4;
```

**2. 构建**（自动完成：前端构建 → 产物复制进 Manager → 打包两个 jar）：

```powershell
# Windows
.\scripts\build.ps1
# Linux/macOS
./scripts/build.sh
```

**3. 启动**（两个进程各自独立运行；首次启动 Manager 会自动建表并创建默认管理员）：

```powershell
$env:DB_TYPE="mysql"; $env:DB_HOST="localhost"; $env:DB_PORT="3306"
$env:DB_NAME="lingqu"; $env:DB_USER="root"; $env:DB_PASSWORD="yourpass"
cd manager;  .\mvnw.cmd spring-boot:run
cd executor; .\mvnw.cmd spring-boot:run
```

前端开发模式（热更新，代理到 8081）：

```powershell
cd frontend; npm install; npm run dev   # http://localhost:5173
```

### 方式二：单容器部署（需求 4.1）

**前置条件**：已安装 Docker（Linux 建议 20.10+；macOS/Windows 用 Docker Desktop），且存在一个可访问的外部 MySQL 5.7+ 或 PostgreSQL 12+ 配置库。

**步骤 1 — 构建镜像**（在项目根目录，首次构建需下载依赖，约 5-15 分钟）：

```bash
docker build -t lingqu .
```

> **国内服务器加速提示**（腾讯云等）：
> 1. 项目已内置多镜像源：Maven 按顺序自动 failover（阿里云 → 腾讯云 → 官方 Central），npm 走 npmmirror；
> 2. 改代码后重建很快：依赖已分层缓存（pom.xml 不变时不再重复下载），通常 1 分钟内完成；
> 3. 拉取基础镜像（eclipse-temurin 等）慢时，给 Docker 配多个加速器（Docker 会依次尝试）：
>    编辑 `/etc/docker/daemon.json`
>    ```json
>    {
>      "registry-mirrors": [
>        "https://mirror.ccs.tencentyun.com",
>        "https://docker.m.daocloud.io",
>        "https://hub-mirror.c.163.com"
>      ]
>    }
>    ```
>    然后 `systemctl restart docker`（腾讯云服务器优先用内网地址 `mirror.ccs.tencentyun.com`）。

**步骤 2 — 创建配置库**（以 MySQL 为例，也可使用你自己的库）：

```sql
CREATE DATABASE lingqu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 首次启动时 Manager 会自动执行 Flyway 建表并创建默认管理员，无需手工建表。

**步骤 3 — 运行容器**（把环境变量换成你的配置库信息）：

```bash
docker run -d --name lingqu -p 8080:8080 -p 8081:8081 \
  -e DB_TYPE=mysql \
  -e DB_HOST=192.168.1.10 \
  -e DB_PORT=3306 \
  -e DB_NAME=lingqu \
  -e DB_USER=lingqu \
  -e DB_PASSWORD='你的密码' \
  -e AES_KEY='换成随机16位以上字符串' \
  -e DEFAULT_ADMIN_PASS='admin初始密码' \
  # 可选：告警邮件
  -e SMTP_HOST=smtp.example.com -e SMTP_PORT=587 \
  -e SMTP_USERNAME=alert@example.com -e SMTP_PASSWORD='xxx' \
  -e ALERT_MAIL_FROM=alert@example.com -e ALERT_MAIL_TO=ops@example.com \
  lingqu
```

或使用 compose 部署（**容器不自建数据库**，直接连接你的外部配置库，配置走 `.env` 文件）：

```bash
cd docker
cp .env.example .env      # 填写外部数据库连接信息（DB_HOST 不要填 localhost）
docker compose up -d --build
```

> 说明：`docker-compose.yml` 只含应用容器，不含数据库服务；`DB_*` 等连接信息
> 全部由同目录 `.env` 文件提供，缺少必填项时 compose 会报错提示，避免配置遗漏。
> `.env` 含数据库密码等敏感信息，已被 `.gitignore` 排除，请勿提交到代码仓库。

**步骤 4 — 验证与访问**：

```bash
docker logs -f lingqu          # 查看启动日志（Manager/Executor 两个进程）
curl http://localhost:8081/actuator/health   # 管理端健康
curl http://localhost:8080/actuator/health   # 业务端健康
```

- 管理后台：<http://localhost:8081>（admin / 你设置的密码）
- 业务 API：<http://localhost:8080>（端口 8080 不暴露给外网更安全，仅供应用服务调用）

**运维提示**：
- 容器内 Supervisor 同时拉起 Manager（8081）和 Executor（8080），任一进程退出会自动重启。
- 配置数据全部存外部配置库，`docker rm` 容器不影响数据；升级只需重新 `docker build` + `docker run`。
- 停止/重启：`docker stop lingqu && docker start lingqu`。
- `AES_KEY` 一旦用于加密数据即不可更改，否则历史密码/Token 无法解密；请妥善保存。

## 环境变量（需求 4.3）

| 变量名 | 必需 | 默认值 | 说明 |
|--------|------|--------|------|
| `DB_TYPE` | ✅ | - | `mysql` 或 `postgresql` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | ✅ | - | 配置库连接信息 |
| `MANAGER_PORT` | ❌ | 8081 | 管理端口 |
| `EXECUTOR_PORT` | ❌ | 8080 | 业务 API 端口 |
| `JVM_OPTS_MANAGER` / `JVM_OPTS_EXECUTOR` | ❌ | `-Xms128m -Xmx256m` | 各进程 JVM 参数 |
| `DEFAULT_ADMIN_USER` / `DEFAULT_ADMIN_PASS` | ❌ | admin / 123456 | 首次启动自动创建的管理员 |
| `AES_KEY` | ❌ | `lingqu-aes-key-01` | AES 加密密钥（生产必改，Manager/Executor 必须一致） |
| `EXECUTOR_BASE_URL` | ❌ | `http://localhost:8080` | Manager 在线调试转发目标（单容器默认即可） |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | ❌ | 空 | 告警邮件 SMTP 通道（不配置则跳过发送） |
| `ALERT_MAIL_FROM` / `ALERT_MAIL_TO` | ❌ | 空 | 告警邮件发件人 / 默认收件人 |

## 项目鉴权与 Token 使用说明（需求 3.4）

### 认证方式

每个项目可独立配置认证方式（项目管理 → 编辑项目 → 认证方式）：

| 认证方式 | 说明 | 调用时携带 |
|----------|------|-----------|
| `不鉴权` | 任何请求可直接调用 | 无 |
| `Bearer Token` | 请求头 `Authorization: Bearer {token}` | 请求头 |
| `API Key` | 请求头 `X-API-Key: {token}` | 请求头 |

### Token 生命周期

- **生成**：项目管理 → 该项目 → 「Token」按钮 → 设置标识名称、**开始时间**（留空=立即生效）、**结束时间**（留空=永不过期）→ 生成。
- **明文查看**：Token 在库中 AES 加密存储；需要时点击列表「查看明文」即可显示并复制（需项目权限）。
- **吊销**：Token 被吊销后立即失效。
- **校验规则**：Token 必须属于目标项目、状态有效、当前时间在 [开始时间, 结束时间] 内；任一不满足返回 401。

### 调用示例

```bash
# 项目认证方式 = Bearer Token
curl -X POST http://{host}:{executor端口}/api/order/getDetail \
  -H "Authorization: Bearer lq_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1001}'

# 项目认证方式 = API Key
curl -X GET "http://{host}:{executor端口}/api/order/getDetail?orderId=1001" \
  -H "X-API-Key: lq_yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"
```

### 常见错误

| HTTP | 含义 | 处理 |
|------|------|------|
| 401 | 缺少 Token / Token 无效 / 未到开始时间 / 已过期 / 已吊销 | 检查请求头与 Token 有效期 |
| 403 | Token 不属于该项目（跨项目使用） | 使用目标项目下生成的 Token |
| 429 | 触发限流 | 降低调用频率或调整接口 QPS |

## 用户与权限说明（多用户）

- **管理员（ADMIN）**：由环境变量 `DEFAULT_ADMIN_USER` / `DEFAULT_ADMIN_PASS` 固定（首次启动自动创建）。管理员拥有全部功能：项目管理（新建/编辑/删除/启停）、数据源管理、告警规则、用户管理。
- **普通用户（USER）**：由管理员在「用户管理」中创建（初始密码 `88888888`），并**绑定可访问的项目**。普通用户登录后：
  - 只能看到/维护**被授权的项目**（项目、接口、日志、调试、文档均按项目过滤）
  - 不能新建/删除/禁用项目，不能查看数据源与告警配置
  - 可修改自己的密码（右上角「修改密码」）
- 管理员可对用户：绑定/解绑项目、启用/禁用账号、**重置密码为默认 `88888888`**。
- 管理员账号受保护：不可被禁用、不可被重置（密码只能通过环境变量修改）。

## 业务 API 调用（需求 7）

```
[GET|POST|PUT|DELETE] /{项目route_prefix}/{接口api_path}
Host: {容器}:8080（或你配置的 EXECUTOR_PORT）
Authorization: Bearer {Token}     # 项目认证方式为 token 时
X-API-Key: {ApiKey}               # 项目认证方式为 apikey 时
Content-Type: application/json    # POST/PUT 时
```

示例：项目 route_prefix=`/api/order`、接口 api_path=`/getDetail`：

```bash
curl -X POST http://localhost:8080/api/order/getDetail \
  -H "Authorization: Bearer lq_xxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1001}'
```

**SQL 编写约定**：

- 参数一律用 `#{param}`（MyBatis 参数化，防注入）；**禁止 `${}`**（上线时会被拦截）。
- 支持 MyBatis 动态标签：`<if>`、`<where>`、`<foreach>`、`<choose>`、`<set>`、`<trim>`、`<bind>`。
- 普通 SQL 中的 `<`/`>` 比较符无需转义（自动走静态解析）；动态 SQL 中比较符请用 `&lt;`/`&gt;` 或 `<![CDATA[ ... ]]>`（MyBatis 标准行为）。
- 请求参数 = query 参数 + JSON body 合并为一个 Map，动态标签 `test` 直接引用参数名。
- `sql_type=groovy` 时脚本可用变量：`params`（参数 Map）、`sql.query(sql, args...)`、`sql.update(sql, args...)`，返回值作为响应。

## 核心功能与需求对照

| 需求 | 实现情况（本轮 MVP = P0+P1） |
|------|------------------------------|
| 3.1 项目管理 | ✅ 创建/列表/编辑/启停/软删除/数据源绑定（编码与路由前缀全局唯一校验） |
| 3.2 数据源管理 | ✅ CRUD + 连接测试 + 密码 AES 加密存储；内置 MySQL/PG 驱动，Oracle/达梦等预留驱动类配置 |
| 3.3 接口管理 | ✅ CRUD + 草稿/上线/下线三态 + 动态 SQL + Groovy 脚本；编辑已上线接口自动回草稿需重新上线；出参映射（字段重命名 + `format` 日期格式化） |
| 3.4 项目鉴权 | ✅ none/token/apikey + Token 生成/列表/吊销/过期；路由首段匹配；Token 加密存储、严格绑定项目 |
| 3.5 调用日志 | ✅ 接口级开关 + 全字段记录（存配置库）+ 日志查询页面（项目/接口/时间/状态筛选、详情查看） |
| 3.6 限流控制 | ✅ Guava 本地令牌桶 + 接口级 QPS + 30s 定期刷新（重启不丢失） |
| 3.7 在线文档/调试 | ✅ 在线文档（按项目展示路径/方法/入参/出参/curl 示例）+ 在线调试（走真实链路转发 Executor） |
| 3.8 系统概览/告警 | ✅ Dashboard（统计卡片 + 近 7 日调用趋势 + 项目/接口 TOP 排行 + 今日错误率）+ 告警邮件通道（超时/错误率规则、防抖、SMTP 发送） |
| 4.1 单容器部署 | ✅ Dockerfile + Supervisor + entrypoint（需在真实 Docker 环境验证） |
| 4.2 外部配置库 | ✅ Flyway `{vendor}` 双方言自动建表 + 首次启动创建默认管理员 |
| 4.5 安全性 | ✅ 参数化查询（禁 `${}`）、AES 加密、BCrypt 密码哈希、Token 过期与项目隔离 |

## 验收对照（需求九）

| # | 验收项 | 如何验证 |
|---|--------|----------|
| 1 | 容器启动 30s 就绪 | `docker compose up` 后访问 8081 |
| 2 | 管理后台登录 | admin / 123456 登录成功 |
| 3 | 创建数据源并测试 | 数据源页「测试连接」返回成功 |
| 4 | 创建项目 | 填信息、绑数据源、设路由前缀 |
| 5 | 创建接口并上线 | 编写 SELECT SQL，点「上线」状态变为已上线 |
| 6 | 调用接口 | `curl http://localhost:8080/{prefix}/{path}` 返回数据 |
| 7 | Token 鉴权 | 启用认证后不带 Token 返回 401，带有效 Token 返回 200 |
| 8 | 调用日志开关 | 开启日志的接口产生记录，关闭的不产生（查询页面下一轮补，数据已入库） |
| 9 | 日志查询 | 见下轮迭代（后端接口已提供 `/api/admin/logs`） |
| 10 | 限流生效 | 接口设 QPS=1，连发请求第二个返回 429 |
| 11 | 数据源切换 | 编辑项目换绑数据源，Executor 30s 内自动重建连接池生效 |
| 12 | 重启不丢配置 | 配置存外部库，重启容器数据保留 |

## 已知限制（本轮）

1. **Groovy 脚本沙箱、出参映射非日期类型格式化** 为后续增强项。
2. Oracle/SQLServer/达梦 等数据源：驱动未内置，需手工填写驱动类名并自行添加驱动 jar。
3. Executor 配置刷新为 30s 轮询（简单可靠；需求 3.6.4 的"简单通知机制"后续可优化为主动推送）。
4. Token 明文仅生成时显示一次；`AES_KEY` 生产环境务必修改，且 Manager/Executor 保持一致。

## 安全说明（重要）

- **Groovy 脚本 = 管理员级代码执行能力**（可调用 `sql` 对象执行任意参数化 SQL）。请仅授予可信管理员使用，并在生产环境做好管理员账号管理。Groovy 脚本不应接收未经校验的外部输入拼接 SQL（脚本内请使用 `sql.query(sql, params)` 参数化形式）。
- SQL 一律使用 `#{}` 参数化，`${}` 在上线时与执行时双重拦截；`X-Forwarded-For` 用于日志记录客户端 IP，生产环境若直接暴露需自行评估伪造风险。
- 默认密钥/默认口令（`lingqu-aes-key-01` / `admin:123456`）仅用于开箱即用，**生产环境必须通过环境变量修改**。
- 管理端登录采用 Session 校验，建议生产环境启用 HTTPS（当前 session cookie 未强制 `Secure`）。

## 技术栈

Spring Boot 2.7.18（Java 11）· MyBatis-Plus 3.5 · Flyway 8 · Vue 3 + Element Plus + Vite · Guava RateLimiter · Groovy · HikariCP · Supervisor

## 开发阶段规划

- ✅ **MVP（本轮）**：P0（建表初始化、登录认证、项目管理、数据源管理）+ P1（接口管理 + SQL 执行引擎、Executor 路由鉴权、前端全部管理页面）
- ⏳ **下一阶段**：P2（日志查询页面、在线调试、在线文档）+ P3（Dashboard 完善、告警邮件、Supervisor/Docker 实测）
