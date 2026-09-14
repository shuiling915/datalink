# DataLink 企业内部部署指南

> 本文档面向企业内部部署场景，涵盖安全加固、环境准备、部署步骤、配置说明、运维监控与故障排查。

---

## 一、改造概述

为适配企业内部部署，DataLink 已完成以下改造：

### 1.1 品牌与命名统一

| 项目 | 改造前 | 改造后 |
|------|--------|--------|
| 项目名 | DataNote | DataLink |
| Java 包名 | `com.datanote` | `com.datalink` |
| 启动类 | `DataNoteApplication` | `DataLinkApplication` |
| 实体类前缀 | `Dn*` | `Dl*` |
| 数据库表前缀 | `dn_*` | `dl_*` |
| 配置前缀 | `datanote.*` | `datalink.*` |
| 配置文件 | `datanote.conf` | `datalink.conf` |
| 启动脚本 | `setup-datanote.sh` | `setup-datalink.sh` |

### 1.2 安全加固

| 加固项 | 说明 |
|--------|------|
| 加密密钥外置 | `CRYPTO_KEY` 必须通过环境变量注入，不再硬编码 |
| 数据库密码外置 | `DB_PASSWORD` 通过环境变量注入，默认空 |
| 登录认证强制 | `DATALINK_PASSWORD` 非空时启用认证，空密码仅用于本地开发 |
| CORS 可配置 | `CORS_ALLOWED_ORIGINS` 限制允许的跨域来源 |
| 接口文档可关闭 | 生产环境通过 `SPRINGDOC_ENABLED=false` 关闭 Swagger |
| 连接池参数化 | HikariCP 连接池大小、超时等可通过环境变量配置 |
| 日志滚动策略 | 日志文件大小、保留天数、总容量上限可配置 |
| 健康检查端点 | `/actuator/health` 供负载均衡与监控使用 |

### 1.3 可观测性

- 集成 Spring Boot Actuator，暴露健康检查端点
- 日志输出到文件并支持按大小滚动，避免磁盘占满

---

## 二、环境要求

### 2.1 硬件最低配置

| 组件 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| DataLink 应用 | 4 核 | 8 GB | 50 GB |
| MySQL | 4 核 | 8 GB | 100 GB（SSD） |
| Hadoop + Hive | 8 核 | 16 GB | 500 GB |

> 测试环境可适当降低配置，生产环境建议 MySQL 与 Hive 独立部署。

### 2.2 软件版本

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| 操作系统 | Linux（CentOS 7+/Ubuntu 20.04+） | 生产建议 Linux；macOS 仅用于开发 |
| JDK | 8（必须） | Lombok 注解处理依赖 Java 8 |
| Maven | 3.6+ | 编译打包 |
| MySQL | 5.7+ / 8.0+ | 元数据库，字符集 utf8mb4 |
| Hadoop | 2.7+ / 3.x | 可选，已有集群可复用 |
| Hive | 2.3+ / 3.x | 可选，已有集群可复用 |
| Docker | 20.10+ | 使用容器化部署 Hive 时需要 |
| Python | 3.8+ | AI 服务（可选） |

### 2.3 网络端口

| 端口 | 服务 | 是否必须 |
|------|------|----------|
| 8099 | DataLink 应用 | 是 |
| 3307 | MySQL（容器） | 是 |
| 10800 | HiveServer2 | 是（数据开发功能） |
| 9870 | HDFS NameNode Web UI | 否 |
| 8000 | AI 对话 Agent | 否（AI 功能） |
| 8001 | AI RAG 管理 | 否（AI 功能） |

---

## 三、部署步骤

### 3.1 准备阶段

```bash
# 1. 拉取代码
git clone <企业内部 Git 仓库地址> datalink
cd datalink

# 2. 生成加密密钥（32 位随机字符串）
LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 32; echo

# 3. 复制配置模板
cp datalink.conf.example datalink.conf
chmod 600 datalink.conf

# 4. 编辑配置
vi datalink.conf
```

### 3.2 配置 `datalink.conf`

```ini
# ---------- MySQL ----------
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3307
MYSQL_USERNAME=datalink          # 建议创建专用账号
MYSQL_PASSWORD=<强密码>

# ---------- DataLink ----------
DATALINK_PORT=8099
DATALINK_USERNAME=admin
DATALINK_PASSWORD=<强密码>        # 必须设置，否则放行所有请求

# ---------- 加密密钥 ----------
CRYPTO_KEY=<第 2 步生成的密钥>

# ---------- DataX ----------
DATAX_MODE=docker
```

> **安全提示**：`datalink.conf` 包含敏感信息，权限设为 `600`，且已被 `.gitignore` 排除，不会提交到仓库。

### 3.3 部署 Hadoop + Hive（已有集群可跳过）

```bash
./setup-hive.sh
```

等待 3-5 分钟，验证：

```bash
./setup-hive.sh test
```

### 3.4 部署 DataLink

```bash
./setup-datalink.sh
```

脚本自动完成：检查 Java → 检查 MySQL → 编译 JAR → 初始化数据库 → 启动应用。

启动成功后访问：`http://<服务器IP>:8099`

### 3.5 配置 Hive 连接

登录后进入 **系统管理 → 数据源管理**，填写 Hive 连接信息：

| 配置项 | 示例值 |
|--------|--------|
| 主机地址 | 127.0.0.1 |
| 端口 | 10800 |
| 认证方式 | NOSASL |
| NameNode | hdfs://localhost:8020 |
| 仓库路径 | /user/hive/warehouse |

点击 **测试连接** → 通过后 **保存配置**。

### 3.6 部署 AI 服务（可选）

```bash
cd ai-service
cp env.example .env
vi .env                    # 填入 QWEN_API_KEY
pip install -r requirements.txt
./start-ai.sh
```

---

## 四、生产环境配置参考

### 4.1 完整环境变量清单

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SERVER_PORT` | 8099 | 应用端口 |
| `DB_HOST` | 127.0.0.1 | MySQL 主机 |
| `DB_PORT` | 3307 | MySQL 端口 |
| `DB_USERNAME` | root | MySQL 用户名 |
| `DB_PASSWORD` | （空） | MySQL 密码 |
| `DB_USE_SSL` | false | MySQL 是否使用 SSL |
| `DB_POOL_SIZE` | 20 | 连接池最大连接数 |
| `DB_POOL_MIN_IDLE` | 5 | 连接池最小空闲数 |
| `CRYPTO_KEY` | （空） | 加密密钥，必填 |
| `DATALINK_USERNAME` | admin | 登录用户名 |
| `DATALINK_PASSWORD` | （空） | 登录密码，生产必填 |
| `HIVE_URL` | jdbc:hive2://... | Hive JDBC URL |
| `DATAX_MODE` | local | DataX 执行模式 |
| `CORS_ALLOWED_ORIGINS` | * | 允许的跨域来源 |
| `SPRINGDOC_ENABLED` | true | 是否开启接口文档 |
| `ACTUATOR_ENDPOINTS` | health | 暴露的 Actuator 端点 |
| `LOG_LEVEL_ROOT` | INFO | 根日志级别 |
| `LOG_LEVEL_DATALINK` | INFO | DataLink 日志级别 |
| `LOG_FILE` | ./logs/datalink.log | 日志文件路径 |
| `LOG_MAX_FILE_SIZE` | 100MB | 单日志文件最大 |
| `LOG_MAX_HISTORY` | 30 | 日志保留天数 |
| `LOG_TOTAL_SIZE_CAP` | 3GB | 日志总容量上限 |

### 4.2 生产环境推荐配置

```ini
# datalink.conf 生产环境示例
MYSQL_HOST=10.0.0.10
MYSQL_PORT=3306
MYSQL_USERNAME=datalink_app
MYSQL_PASSWORD=<强密码>

DATALINK_PORT=8099
DATALINK_USERNAME=admin
DATALINK_PASSWORD=<强密码>

CRYPTO_KEY=<32位随机密钥>
DATAX_MODE=docker

CORS_ALLOWED_ORIGINS=https://datalink.company.com
SPRINGDOC_ENABLED=false
LOG_LEVEL_DATALINK=INFO
```

### 4.3 systemd 服务配置（推荐生产使用）

创建 `/etc/systemd/system/datalink.service`：

```ini
[Unit]
Description=DataLink 数据开发平台
After=network.target mysql.service

[Service]
Type=simple
User=datalink
WorkingDirectory=/opt/datalink
EnvironmentFile=/opt/datalink/datalink.env
ExecStart=/usr/bin/java -Xms1g -Xmx2g -jar /opt/datalink/target/datalink-1.0.0.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/datalink/app.log
StandardError=append:/var/log/datalink/error.log

[Install]
WantedBy=multi-user.target
```

创建环境变量文件 `/opt/datalink/datalink.env`：

```ini
DB_HOST=10.0.0.10
DB_PORT=3306
DB_USERNAME=datalink_app
DB_PASSWORD=<强密码>
CRYPTO_KEY=<密钥>
DATALINK_USERNAME=admin
DATALINK_PASSWORD=<密码>
CORS_ALLOWED_ORIGINS=https://datalink.company.com
SPRINGDOC_ENABLED=false
```

启动服务：

```bash
systemctl daemon-reload
systemctl enable datalink
systemctl start datalink
systemctl status datalink
```

---

## 五、运维监控

### 5.1 健康检查

```bash
# 检查应用健康状态
curl http://localhost:8099/actuator/health
```

返回示例：

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### 5.2 日志查看

```bash
# 实时日志
tail -f /tmp/datalink.log

# 错误日志
grep ERROR /tmp/datalink.log | tail -50
```

### 5.3 常用运维命令

```bash
# 启动
./setup-datalink.sh

# 停止
./setup-datalink.sh stop

# 查看进程
lsof -ti:8099

# 重启
./setup-datalink.sh stop && sleep 3 && ./setup-datalink.sh
```

---

## 六、数据备份

### 6.1 MySQL 备份

```bash
# 全量备份
mysqldump -h 127.0.0.1 -P 3307 -u root -p --databases datalink > datalink_backup_$(date +%Y%m%d).sql

# 恢复
mysql -h 127.0.0.1 -P 3307 -u root -p datalink < datalink_backup_20260101.sql
```

建议配置定时任务（crontab）每日备份。

### 6.2 加密密钥备份

`CRYPTO_KEY` 用于加密数据源密码等敏感字段。**务必妥善备份**：

- 密钥丢失后，已加密的数据源密码将无法解密
- 建议将密钥存储在企业密钥管理系统（KMS）中
- 更换密钥后需在页面重新录入所有数据源密码

---

## 七、安全加固清单

部署完成后，请逐项确认：

- [ ] `datalink.conf` 权限为 `600`
- [ ] `CRYPTO_KEY` 已设置为随机值，非默认值
- [ ] `DATALINK_PASSWORD` 已设置强密码
- [ ] MySQL 使用专用账号，非 root
- [ ] MySQL 密码为强密码
- [ ] `CORS_ALLOWED_ORIGINS` 已限制为企业域名
- [ ] `SPRINGDOC_ENABLED=false`（生产环境关闭接口文档）
- [ ] 防火墙仅开放必要端口（8099）
- [ ] 日志目录已配置滚动策略
- [ ] 已配置 MySQL 定时备份
- [ ] `CRYPTO_KEY` 已备份到安全位置

---

## 八、常见问题

**Q: 启动报错「请在 datalink.conf 中设置 CRYPTO_KEY」？**
A: 编辑 `datalink.conf`，填入 `CRYPTO_KEY`（32 位随机字符串），重新启动。

**Q: 页面打不开？**
A: 查看日志 `tail -50 /tmp/datalink.log`，通常是 MySQL 连接问题。确认 `DB_HOST`、`DB_PORT`、`DB_PASSWORD` 配置正确。

**Q: 数据源密码无法解密？**
A: `CRYPTO_KEY` 被修改过。恢复原密钥，或在页面重新录入所有数据源密码。

**Q: 如何关闭接口文档？**
A: 设置环境变量 `SPRINGDOC_ENABLED=false` 或在 `datalink.conf` 中添加 `SPRINGDOC_ENABLED=false`。

**Q: 如何限制跨域访问？**
A: 设置 `CORS_ALLOWED_ORIGINS=https://datalink.company.com`，多个来源用逗号分隔。

**Q: 健康检查端点返回 401？**
A: `/actuator/health` 已配置为无需认证。若返回 401，请检查是否有反向代理拦截了该路径。

---

## 九、升级流程

```bash
# 1. 停止服务
./setup-datalink.sh stop

# 2. 备份数据库
mysqldump -h 127.0.0.1 -P 3307 -u root -p datalink > backup_before_upgrade.sql

# 3. 拉取新版本
git pull

# 4. 重新编译（如果需要）
mvn clean package -DskipTests

# 5. 启动
./setup-datalink.sh
```

> 升级前务必备份数据库和 `datalink.conf`。