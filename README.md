# DataLink

**轻量级一站式数据平台** — 覆盖数据开发、实时计算、数据治理、数据湖、AI 辅助全链路。单 JAR 包即可运行。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-green.svg)](https://spring.io/projects/spring-boot)

---

## 平台架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                         DataLink 一站式平台                            │
├───────────┬───────────┬───────────┬───────────┬───────────┬──────────┤
│  数据开发  │  实时开发  │  数据治理  │  数据湖   │   AI 智能  │  BI看板  │
│ HiveSQL   │ Flink SQL │ 质量监控   │ 多模态存储 │ NL2SQL     │ DataEase │
│ 数据同步   │ 流式计算   │ 指标管理   │ 算子编排   │ SQL 优化   │ 仪表板   │
│ 任务调度   │ 执行计划   │ 元数据管理 │ 文件处理   │ 知识库     │ 多数据源  │
├───────────┴───────────┴───────────┴───────────┴───────────┴──────────┤
│  基础设施：MySQL / Hive / Flink / DataX / Docker / FastAPI             │
└──────────────────────────────────────────────────────────────────────┘
```

## 模块说明

### 数据开发
- Monaco Editor 在线 SQL IDE，支持 HiveSQL 语法高亮、自动补全
- 脚本管理：文件夹组织、版本历史、发布审批
- 数据同步：MySQL → Hive 全量/增量同步（DataX 引擎），字段映射，一键建表
- 结果展示：表格排序、筛选、CSV 导出

### 实时开发
- 基于 Apache Flink 的流式计算，支持 Flink SQL
- 任务 CRUD、流式运行/批量运行、日志实时推送（WebSocket）
- SQL 语法校验（EXPLAIN）与执行计划可视化
- 通过 Flink SQL Gateway REST API 与集群交互，避免嵌入式类加载问题

### 数据治理
- 数据质量：规则配置、质量检查、检查历史与统计
- 指标管理：业务指标定义、分类管理、指标查询
- 元数据管理：表结构探查、字段统计、数据预览

### 多模态数据湖
- 多模态文件存储与检索（文本、图像、视频、文档）
- 可扩展算子编排（文本清洗、图像转换、文件压缩等 40+ 算子）
- 知识图谱构建、向量化搜索
- DORIS 告警引擎与巡检调度
-> React 前端，FastAPI 后端，本地文件存储 / S3 兼容

### BI 看板
- 集成 DataEase 开源 BI 工具（Docker 部署）
- 拖拽式图表制作，支持 20+ 数据源（MySQL、Hive、ClickHouse 等）
- 仪表板创建与管理，公共链接分享，iframe 嵌入
- 默认地址 `http://localhost:8100`，账号 `admin / DataEase@123456`

### AI 智能
- NL2SQL：自然语言转 SQL
- SQL 解释与优化建议
- AI 语义搜索（表名、字段名模糊匹配）
- 知识库管理：文档向量化、语义检索、版本管理

### 任务调度
- Cron 定时调度，DAG 依赖管理
- 失败重试（指数退避）、超时告警
- 补数回溯、批量重跑、下游暂停/恢复
- 执行历史与趋势统计

### 系统管理
- 用户认证与权限管理
- 数据源连接配置（Hive、MySQL）
- JVM / 系统资源监控
- AI 配置（API Key、模型选择）

## 技术栈

| 层 | 技术 |
|----|------|
| 后端框架 | Java 8 + Spring Boot 2.7 + MyBatis-Plus 3.5 |
| 前端 | Vanilla JS SPA (workspace.html) + Vue 3 (数据建模) + React (数据湖) |
| 元数据库 | MySQL 8.0 |
| 数仓引擎 | Apache Hive 3 |
| 实时引擎 | Apache Flink 1.17 (Docker, SQL Gateway) |
| 数据同步 | DataX |
| 数据湖 API | Python 3 + FastAPI + LanceDB |
| BI 工具 | DataEase (Docker) |
| WebSocket | Spring WebSocket + STOMP |
| AI 服务 | Python 3 + FastAPI + OpenAI SDK |

## 快速开始

### 一键启动

```bash
git clone git@github.com:shuiling915/datalink.git
cd datalink

# 修改配置（MySQL 密码等）
cp datalink.conf.example datalink.conf
vi datalink.conf

# 一键启动所有服务
./dn-up.sh

# 访问 http://localhost:8099/workspace.html
```

### 分步部署

```bash
# 1. 安装 Hadoop + Hive（Docker）
./setup-hive.sh
./setup-hive.sh test          # 验证 Hive

# 2. 安装 DataX（需要数据同步时）
./setup-datax.sh start

# 3. 启动 Flink 集群（需要实时开发时）
cd flink-docker && docker-compose up -d

# 4. 启动 DataLink（自动编译、建库）
./setup-datalink.sh
```

### 环境要求

| 组件 | 要求 |
|------|------|
| Docker | Desktop 20+ (Hive / Flink / DataX) |
| Java | JDK 8+ |
| Maven | 3.6+ |
| Python | 3.9+ (数据湖模块) |
| Node.js | 16+ (前端构建) |

## 配置说明

核心配置通过 `datalink.conf` 或环境变量：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_PASSWORD` | MySQL root 密码 | (必填) |
| `DB_HOST` | MySQL 地址 | 127.0.0.1 |
| `DB_PORT` | MySQL 端口 | 3306 |
| `HIVE_HOST` | HiveServer2 地址 | 127.0.0.1 |
| `FLINK_REST_URL` | Flink SQL Gateway | http://127.0.0.1:8083 |
| `DATA_NOTE_PORT` | 应用端口 | 8099 |

Hive / DataX / AI 等也可在页面配置：系统管理 → 数据源管理 / 环境配置 / AI 配置。

## 项目结构

```
datalink/
├── src/main/java/com/datalink/
│   ├── controller/        # 30+ REST API 控制器
│   │   ├── RealtimeTaskController.java   # 实时开发
│   │   ├── DataDevelopmentController.java # 数据开发
│   │   ├── QualityController.java        # 数据治理
│   │   ├── SystemMonitorController.java  # 系统监控
│   │   └── ...
│   ├── service/           # 业务服务层
│   │   ├── FlinkService.java            # Flink SQL 执行
│   │   ├── HiveService.java             # Hive 查询
│   │   ├── LogBroadcastService.java     # WebSocket 广播
│   │   └── ...
│   ├── mapper/            # MyBatis-Plus Mapper
│   ├── model/             # 实体类 / DTO
│   └── config/            # Spring 配置
├── src/main/resources/
│   ├── static/workspace.html   # 主前端 SPA (~780KB)
│   └── application.yml         # Spring Boot 配置
├── frontend/              # Vue 3 数据建模前端
├── multimodal-data-lake/  # 多模态数据湖（Python + React）
│   ├── backend/           # FastAPI 后端
│   ├── frontend/          # React 前端
│   └── agents/            # AI Agent 子系统
├── flink-docker/          # Flink Docker Compose 配置
├── ai-service/            # AI 服务（RAG、需求分析 Agent）
├── docker/                # Hive/Hadoop 配置
├── sql/init-all.sql       # 数据库初始化
├── dn-up.sh               # 一键启动
├── dn-down.sh             # 一键停止
└── pom.xml
```

## API 概览

| 模块 | 前缀 | 说明 |
|------|------|------|
| 数据开发 | `/api/scripts` | 脚本 CRUD、文件夹、版本 |
| 数据开发 | `/api/datasync` | 同步任务 CRUD、DataX 作业 |
| 数据开发 | `/api/hive` | SQL 执行、结果查询 |
| 实时开发 | `/api/realtime` | Flink 任务管理、运行、执行计划 |
| 数据治理 | `/api/quality` | 质量规则、检查执行 |
| 指标管理 | `/api/metrics` | 指标 CRUD、分类 |
| 数据地图 | `/api/datamap` | 搜索、表详情、探查 |
| 任务调度 | `/api/scheduler` | 调度配置、运行记录 |
| 系统监控 | `/api/monitor` | JVM、系统资源、任务统计 |
| 数据湖 | `/api/lake/*` | 多模态存储、算子、检索 |
| AI 智能 | `/api/ai/*` | NL2SQL、知识库、RAG |

完整 API 文档：启动后访问 `http://localhost:8099/swagger-ui.html`

## 数据湖模块 (multimodal-data-lake)

多模态数据湖提供统一的多模态数据管理能力：

- **文件管理**：上传、预览、元数据提取（PDF/Word/PPT/Excel/图片）
- **算子编排**：40+ 内置算子（文本清洗、图片转换、文件分割等），支持可视化拖拽
- **知识图谱**：实体关系抽取、图谱查询
- **向量检索**：LanceDB 向量存储，语义相似搜索
- **DORIS 监控**：数据巡检、告警引擎

```
# 单独启动数据湖
cd multimodal-data-lake
python3 -m uvicorn backend.main:app --host 0.0.0.0 --port 27844
```

## 与同类项目对比

| 特性 | DataLink | DataSphereStudio | Dinky | DolphinScheduler |
|------|----------|-----------------|-------|-----------------|
| 部署 | 单 JAR | Linkis + 多子项目 | Docker | 微服务集群 |
| 前端 | 单 HTML SPA | React 多模块 | Ant Design Pro | Vue 3 |
| 实时开发 | Flink SQL | 需插件 | 核心功能 | 无 |
| 数据湖 | 多模态 + 算子 | 无 | 无 | 无 |
| 数据同步 | 内置 DataX | Exchangis | FlinkCDC | 无 |
| AI 辅助 | NL2SQL + 知识库 | 无 | 无 | 无 |
| 数据质量 | 内置 | Qualitis | 无 | 无 |
| 上手时间 | 5 分钟 | 1-2 天 | 30 分钟 | 1 小时 |

## 许可证

[Apache License 2.0](LICENSE) © DataLink Team