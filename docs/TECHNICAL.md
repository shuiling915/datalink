# DataLink 技术文档

## 1. 系统架构

### 1.1 总体架构

```
                         ┌─────────────────────────────────────┐
                         │          Nginx / 直接访问              │
                         │          http://localhost:8099          │
                         └──────────────┬──────────────────────┘
                                        │
              ┌─────────────────────────┼─────────────────────────┐
              │                         │                         │
    ┌─────────▼─────────┐    ┌─────────▼─────────┐    ┌─────────▼─────────┐
    │   workspace.html   │    │   Vue 3 前端       │    │   React 前端       │
    │   (Vanilla JS)     │    │   (数据建模)        │    │   (数据湖)          │
    │   主交互页面         │    │   端口: 27841       │    │   端口: 27844       │
    └─────────┬─────────┘    └─────────┬─────────┘    └─────────┬─────────┘
              │                        │                         │
    ┌─────────▼────────────────────────▼─────────────────────────▼─────────┐
    │                     Spring Boot 2.7 (端口 8099)                       │
    │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
    │  │ Controller│ │ Service   │ │ Mapper   │ │ Config   │ │ WebSocket│   │
    │  │ 30+ REST  │ │ 20+ 服务  │ │ 40+ Mapper│ │ Spring   │ │ STOMP    │   │
    │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
    └────────┬────────────────────┬────────────────────┬──────────────────┘
             │                    │                    │
    ┌────────▼────────┐  ┌───────▼───────┐  ┌────────▼──────────────────┐
    │   MySQL 8.0     │  │  Hive 3.x     │  │  FastAPI (端口 27844)      │
    │   (元数据)       │  │  (数仓引擎)    │  │  multimodal-data-lake      │
    │   端口: 3306     │  │  端口: 10000   │  │  多模态存储 / 算子 / 检索  │
    └─────────────────┘  └───────────────┘  └───────────────────────────┘

    ┌──────────────────────────────────────────────────────────┐
    │                  外部服务 (Docker)                         │
    │  ┌──────────────────┐  ┌──────────────────┐               │
    │  │ Flink 1.17        │  │ DataX            │               │
    │  │ JobManager: 8081  │  │ (MySQL → Hive)   │               │
    │  │ SQL Gateway: 8083 │  │                  │               │
    │  │ TaskManager: 1-2  │  │                  │               │
    │  └──────────────────┘  └──────────────────┘               │
    │  ┌──────────────────────┐                                  │
    │  │ DataEase BI (Docker) │                                  │
    │  │ 端口: 8100            │                                  │
    │  │ MySQL 内部: 3308      │                                  │
    │  └──────────────────────┘                                  │
    └──────────────────────────────────────────────────────────┘
```

### 1.2 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| DataLink Spring Boot | 8099 | 主应用，包含所有 API 和主前端 |
| Vue 3 数据建模 | 27841 | Vite 开发服务器 (dev) 或静态文件 (prod) |
| FastAPI 数据湖 | 27844 | 多模态数据湖 API 和 React 前端 |
| MySQL | 3306 | Docker 容器 datalink-mysql |
| HiveServer2 | 10000 | Docker 容器 datalink-hiveserver2 |
| HDFS NameNode | 8020 | Docker 容器 datalink-namenode |
| Flink JobManager | 8081 | Docker 容器 flink-jobmanager |
| Flink SQL Gateway | 8083 | Docker 容器 flink-sql-gateway |
| AI RAG 服务 | 8001 | Python ai-service/rag_admin_service.py |
| 需求管理 Agent | 8000 | Python ai-service/requirement_analyst.py |
| DataEase BI | 8100 | DataEase BI 看板 (Docker) |
| DataEase MySQL | 3308 | BI 内部数据库 (Docker) |

---

## 2. 核心模块设计

### 2.1 实时开发模块

#### 架构

```
工作区页面 (workspace.html)
       │
       ├── REST: /api/realtime/*
       │   ├── GET  /list              → 任务列表
       │   ├── GET  /{id}              → 任务详情
       │   ├── POST /save              → 保存任务
       │   ├── POST /{id}/run          → 批量执行 Flink SQL
       │   ├── POST /{id}/run-stream   → 流式执行
       │   ├── POST /{id}/explain      → 执行计划 (EXPLAIN)
       │   ├── POST /{id}/start        → 启动任务
       │   └── POST /{id}/stop         → 停止任务
       │
       └── WebSocket: /ws
           ├── /topic/realtime-log     → 实时日志推送
           └── /topic/realtime-result  → 执行结果推送
```

#### FlinkService

核心执行逻辑位于 [FlinkService.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/service/FlinkService.java)：

- 通过 Flink SQL Gateway REST API (`http://localhost:8083`) 执行 SQL
- 流程：创建 Session → 提交 SQL → 轮询状态 → 获取结果
- 支持批量执行（`STATEMENT_SET`）和流式执行
- 异步执行，通过 WebSocket 推送日志和结果

```
executeSQL(sql):
  1. POST /v1/sessions → 获取 sessionHandle
  2. POST /v1/sessions/{id}/statements → 提交 SQL
  3. GET  /v1/sessions/{id}/operations/{op}/status → 轮询
  4. 状态 = FINISHED → 等待 1s → 获取结果
  5. GET  /v1/sessions/{id}/operations/{op}/result/0 → 分页取结果
  6. DELETE /v1/sessions/{id} → 清理 Session
```

#### 数据库表

```sql
CREATE TABLE dn_realtime_task (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_name       VARCHAR(255) NOT NULL,
  content         TEXT,
  task_type       VARCHAR(50) DEFAULT 'sql',
  status          VARCHAR(20) DEFAULT 'draft',
  folder_id       BIGINT,
  last_start_time DATETIME,
  last_stop_time  DATETIME,
  last_error      TEXT,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 2.2 数据开发模块

核心控制器 [DataDevelopmentController.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/controller/DataDevelopmentController.java)：

- `/api/scripts/*` — 脚本 CRUD、文件夹管理、版本历史、发布审批
- `/api/datasync/*` — 同步任务管理（MySQL → Hive）
- `/api/hive/*` — HiveSQL 执行、结果分页查询、DDL 生成
- `/api/lineage/*` — 字段级血缘分析

#### 脚本版本管理

```
保存脚本 → 自动生成版本快照（dn_script_version 表）
发布流程：草稿 → 提交审批 → 审批通过 → 已发布
```

### 2.3 任务调度模块

#### 组件

| 组件 | 文件 | 说明 |
|------|------|------|
| `TaskSchedulerService` | [TaskSchedulerService.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/service/TaskSchedulerService.java) | 核心调度引擎，Cron 触发、DAG 拓扑排序 |
| `TaskExecutionService` | [TaskExecutionService.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/service/TaskExecutionService.java) | 任务执行、重试逻辑 |
| `TaskDependencyService` | [TaskDependencyService.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/service/TaskDependencyService.java) | DAG 依赖计算、下游暂停 |
| `BackfillService` | [BackfillService.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/service/BackfillService.java) | 补数回溯 |

#### 执行状态机

```
PENDING → RUNNING → SUCCESS
                  → FAILED  → PENDING (重试, retryTimes > 0)
                            → FAILED  (最终失败, 触发告警)
                  → TIMEOUT (超时, 触发告警)
```

### 2.4 WebSocket 实时推送

配置类：[WebSocketConfig.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/config/WebSocketConfig.java)
广播服务：[LogBroadcastService.java](file:///Users/nayy/JavaProjects/datalink/src/main/java/com/datalink/service/LogBroadcastService.java)

```java
// 日志广播
logBroadcastService.broadcast("realtime-log", level, taskId, taskName, message);

// 结果广播
logBroadcastService.broadcast("realtime-result", "OK", taskId, taskName, summary);
```

前端通过 SockJS + STOMP 订阅：
- `/topic/realtime-log` — 实时执行日志
- `/topic/realtime-result` — 执行结果通知

### 2.5 多模态数据湖

#### 服务组成

| 组件 | 路径 | 技术 |
|------|------|------|
| FastAPI 后端 | `multimodal-data-lake/backend/` | Python 3, FastAPI, LanceDB |
| React 前端 | `multimodal-data-lake/frontend/` | React, Vite |
| AI Agent | `multimodal-data-lake/agents/` | Python, LLM |

#### API 模块

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户认证 | `/api/auth/*` | JWT 认证，bcrypt 密码哈希 |
| 文件管理 | `/api/files/*` | 上传、下载、预览、元数据 |
| 算子编排 | `/api/operators/*` | 40+ 算子注册与执行 |
| 知识图谱 | `/api/knowledge/*` | 实体关系抽取与检索 |
| 向量搜索 | `/api/search/*` | LanceDB 语义搜索 |
| DORIS 监控 | `/api/doris/*` | 数据巡检与告警 |

#### 环境变量

```bash
# multimodal-data-lake/.env
S3_ENDPOINT_URL=           # 留空使用本地存储
CORS_ALLOW_ORIGINS=http://localhost:8099
BACKEND_RELOAD=false
```

### 2.6 BI 看板 (DataEase)

#### 部署架构

```
bi-docker/
├── docker-compose.yml    # MySQL 8.0 + DataEase v2.10.6
├── setup-bi.sh           # 独立启动脚本
└── (数据卷 de-*)          # Docker volumes (已被 gitignore)
```

#### 服务组成

| 组件 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| DataEase | de-server | 8100 | BI 应用 (Spring Boot 3.3) |
| MySQL | de-mysql | 3308 | BI 内部元数据库 |

#### 默认凭证

- 地址: `http://localhost:8100`
- 账号: `admin`
- 密码: `DataEase@123456`

#### 集成方式

DataEase 通过 iframe 嵌入到 workspace.html 的 `#viewBI` 页面视图中。
用户可在 DataEase 中连接 MySQL/Hive 数据源，创建仪表板后通过公共链接分享，
实现与 DataLink 的统一展示。

```

---

## 3. 前端架构

### 3.1 workspace.html (主 SPA)

位于 `src/main/resources/static/workspace.html`，约 16000 行，完全自包含的 vanilla JS SPA。

#### 路由系统

```javascript
var ROUTES = {
  'home':        { view: 'viewHome',       layout: 'page',   init: ... },
  'develop':     { view: null,             layout: 'editor', init: null },
  'realtime':    { view: null,             layout: 'editor', init: ... },
  'operations':  { view: 'viewScheduler',  layout: 'editor', init: ... },
  'catalog':     { view: 'viewDatamap',    layout: 'editor', init: ... },
  'governance':  { view: 'viewQuality',    layout: 'editor', init: ... },
  'metrics':     { view: 'viewMetrics',    layout: 'editor', init: ... },
  'project':     { view: 'viewProject',    layout: 'editor', init: ... },
  'settings':    { view: 'viewSettings',   layout: 'editor', init: ... },
  'requirement': { view: 'viewRequirement', layout: 'page',  init: ... },
  'ai':          { view: 'viewAI',         layout: 'page',   init: ... },
  'lake':        { view: 'viewLake',       layout: 'page',   init: null }
};
```

两种布局模式：
- `page` — 独立页面（首页、AI、需求、数据湖）
- `editor` — 编辑器布局（左侧树 + 右侧编辑器 + 底部结果）

### 3.2 Vue 3 前端 (数据建模)

位于 `frontend/`，使用 Vue 3 + Element Plus + Vite。

路由：`/vue/#/domains`, `/vue/#/dimensions`, `/vue/#/fact-tables` 等。

### 3.3 React 前端 (数据湖)

位于 `multimodal-data-lake/frontend/`，使用 React + Vite。

构建后由 FastAPI 静态文件服务托管。

---

## 4. 部署架构

### 4.1 Docker 容器

```
datalink-mysql          → MySQL 8.0 (3306)
datalink-namenode       → HDFS NameNode (8020)
datalink-datanode       → HDFS DataNode
datalink-metastore      → Hive Metastore (9083)
datalink-hiveserver2    → HiveServer2 (10000)
datalink-datax          → DataX 执行容器

flink-jobmanager        → Flink JobManager (8081)
flink-taskmanager       → Flink TaskManager
flink-sql-gateway       → Flink SQL Gateway (8083)
```

### 4.2 启动脚本

| 脚本 | 说明 |
|------|------|
| `dn-up.sh` | 一键启动所有服务（MySQL → Hive → DataX → 数据湖 → DataLink） |
| `dn-down.sh` | 一键停止所有服务 |
| `setup-hive.sh` | 初始化 Hadoop + Hive Docker 集群 |
| `setup-datax.sh` | 初始化 DataX 容器 |
| `setup-datalink.sh` | 编译并启动 DataLink |

### 4.3 Flink Docker 配置

`flink-docker/docker-compose.yml`:

```yaml
services:
  jobmanager:
    image: flink:1.17.2-scala_2.12-java11
    ports: ["8081:8081"]
    command: jobmanager
    environment:
      - FLINK_PROPERTIES=jobmanager.rpc.address: jobmanager

  taskmanager:
    image: flink:1.17.2-scala_2.12-java11
    depends_on: [jobmanager]
    command: taskmanager
    environment:
      - FLINK_PROPERTIES=jobmanager.rpc.address: jobmanager
                          taskmanager.numberOfTaskSlots: 2

  sql-gateway:
    build: ./sql-gateway
    ports: ["8083:8083"]
    depends_on:
      jobmanager:
        condition: service_healthy
```

---

## 5. 开发指南

### 5.1 本地开发环境

```bash
# 终端 1: 启动 Spring Boot
cd datalink
mvn spring-boot:run

# 终端 2: 启动 Vue 前端 (可选)
cd datalink/frontend
npm run dev                    # http://localhost:27841

# 终端 3: 启动数据湖 FastAPI (可选)
cd datalink/multimodal-data-lake
python3 -m uvicorn backend.main:app --reload --port 27844

# 终端 4: 启动 Flink (可选)
cd datalink/flink-docker
docker-compose up -d
```

### 5.2 添加新功能

1. **新增 API 端点**：
   - 创建 Controller 类，使用 `@RestController` + `@RequestMapping`
   - 在 `workspace.html` 中添加对应的 fetch 调用
   - 更新 ROUTES 配置

2. **新增页面**：
   - 在 `workspace.html` 中添加 `page-view` div
   - 在 ROUTES 中注册路由
   - 添加 header-tab 导航标签

3. **新增数据表**：
   - 在 `sql/init-all.sql` 中添加 DDL
   - 创建对应的 Model / Mapper 类
   - 重启应用（或执行手动建表）

### 5.3 代码规范

- Controller: `@RestController` + `@RequestMapping("/api/...")` + Swagger 注解
- Service: `@Service` 接口 + 实现，注入 Mapper
- Model: MyBatis-Plus 注解 (`@TableName`, `@TableId`)
- Mapper: `@Mapper` + `extends BaseMapper<T>`
- 统一响应：`R.ok(data)` / `R.fail(message)`

---

## 6. 测试

### 6.1 API 测试

```bash
# 实时开发
curl http://localhost:8099/api/realtime/list

# 监控概览
curl http://localhost:8099/api/monitor/overview

# Flink 集群状态
curl http://localhost:8081/overview

# Flink SQL Gateway
curl http://localhost:8083/v1/info

# 数据湖健康检查
curl http://localhost:27844/api/health
```

### 6.2 Flink SQL 测试

```sql
-- 基础运算
SELECT 1 + 1 AS result;

-- 查看 Catalog
SHOW DATABASES;

-- 创建测试表
CREATE TABLE test_table (id INT, name STRING) WITH (
  'connector' = 'datagen', 'rows-per-second' = '1'
);
```

---

## 7. 故障排查

| 问题 | 排查步骤 |
|------|----------|
| Spring Boot 启动失败 | 检查 MySQL 是否运行；确认 `datalink.conf` 配置 |
| Flink SQL 执行超时 | 检查 SQL Gateway 是否健康；查看 `docker logs flink-sql-gateway` |
| WebSocket 断连 | 浏览器控制台检查 STOMP 连接；确认 `CorsConfig` 配置 |
| 数据湖 API 无响应 | 确认 Python 依赖已安装；检查 `.env` 配置 |
| 数据同步失败 | 确认 DataX 容器运行；检查 MySQL/Hive 连接测试 |