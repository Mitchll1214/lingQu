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

```bash
# 1. 构建镜像
docker build -t lingqu .

# 2. 运行（需先有外部 MySQL/PG）
docker run -d --name lingqu -p 8080:8080 -p 8081:8081 \
  -e DB_TYPE=mysql -e DB_HOST=192.168.1.10 -e DB_PORT=3306 \
  -e DB_NAME=lingqu -e DB_USER=lingqu -e DB_PASSWORD=lingqu123 \
  -e DEFAULT_ADMIN_USER=admin -e DEFAULT_ADMIN_PASS=123456 \
  lingqu
```

或使用 `docker/docker-compose.yml`（自带 MySQL 配置库，一条命令联调）：

```bash
cd docker && docker compose up -d --build
```

启动后访问：管理后台 <http://localhost:8081>（admin / 123456）。

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

## 业务 API 调用（需求 7）

```
POST /{项目route_prefix}/{接口api_path}
Host: {容器}:8080
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
| 3.3 接口管理 | ✅ CRUD + 草稿/上线/下线三态 + 动态 SQL + Groovy 脚本；编辑已上线接口自动回草稿需重新上线 |
| 3.4 项目鉴权 | ✅ none/token/apikey + Token 生成/列表/吊销/过期；路由首段匹配；Token 加密存储、严格绑定项目 |
| 3.5 调用日志 | ✅ 接口级开关 + 全字段记录（存配置库）+ Manager 查询接口（查询页面下一轮补） |
| 3.6 限流控制 | ✅ Guava 本地令牌桶 + 接口级 QPS + 30s 定期刷新（重启不丢失） |
| 3.7 在线文档/调试 | ⏳ 下一阶段 |
| 3.8 系统概览 | ✅ 基础统计 + 项目调用量排行（告警配置下一阶段） |
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

1. **日志查询页面、在线调试、在线文档、告警、Dashboard 完整版** 为下一阶段（后端接口大多已就绪）。
2. Oracle/SQLServer/达梦 等数据源：驱动未内置，需手工填写驱动类名并自行添加驱动 jar。
3. Executor 配置刷新为 30s 轮询（简单可靠；需求 3.6.4 的"简单通知机制"后续可优化为主动推送）。
4. 出参映射（`response_format`）当前存储但不参与转换，下一阶段实现。
5. Docker 相关文件已就绪，需在有 Docker 的环境执行 `docker compose up` 实测。
6. Token 明文仅生成时显示一次；`AES_KEY` 生产环境务必修改，且 Manager/Executor 保持一致。

## 技术栈

Spring Boot 2.7.18（Java 11）· MyBatis-Plus 3.5 · Flyway 8 · Vue 3 + Element Plus + Vite · Guava RateLimiter · Groovy · HikariCP · Supervisor

## 开发阶段规划

- ✅ **MVP（本轮）**：P0（建表初始化、登录认证、项目管理、数据源管理）+ P1（接口管理 + SQL 执行引擎、Executor 路由鉴权、前端全部管理页面）
- ⏳ **下一阶段**：P2（日志查询页面、在线调试、在线文档）+ P3（Dashboard 完善、告警邮件、Supervisor/Docker 实测）
