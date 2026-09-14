-- ============================================================
--  补全缺失表 + 初始化配置数据
-- ============================================================
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 1. 提示词表
CREATE TABLE IF NOT EXISTS dl_prompt (
  id bigint NOT NULL AUTO_INCREMENT,
  code varchar(100) NOT NULL COMMENT '标识符',
  name varchar(200) NOT NULL COMMENT '名称',
  description varchar(500) DEFAULT NULL COMMENT '用途说明',
  content text COMMENT '提示词内容',
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 提示词管理';

-- 2. 数据画像表
CREATE TABLE IF NOT EXISTS dl_data_profile (
  id bigint NOT NULL AUTO_INCREMENT,
  db_name varchar(100) DEFAULT NULL,
  table_name varchar(200) DEFAULT NULL,
  raw_stats text COMMENT '原始统计 JSON',
  ai_report text COMMENT 'AI 生成的画像报告',
  profile_time datetime DEFAULT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_db_table (db_name, table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据画像';

-- 3. 脚本发布表
CREATE TABLE IF NOT EXISTS dl_script_publish (
  id bigint NOT NULL AUTO_INCREMENT,
  dev_script_id bigint DEFAULT NULL,
  prod_script_id bigint DEFAULT NULL,
  publish_version varchar(50) DEFAULT NULL,
  publish_type varchar(50) DEFAULT NULL,
  publish_status varchar(50) DEFAULT NULL,
  dev_content text,
  prod_content_before text,
  grayscale_enabled int DEFAULT 0,
  grayscale_limit int DEFAULT NULL,
  grayscale_dev_rows bigint DEFAULT NULL,
  grayscale_prod_rows bigint DEFAULT NULL,
  grayscale_dev_time bigint DEFAULT NULL,
  grayscale_prod_time bigint DEFAULT NULL,
  grayscale_match int DEFAULT NULL,
  grayscale_detail text,
  published_by varchar(100) DEFAULT NULL,
  publish_comment varchar(500) DEFAULT NULL,
  published_at datetime DEFAULT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脚本发布记录';

-- 4. 需求表
CREATE TABLE IF NOT EXISTS dl_requirement (
  id bigint NOT NULL AUTO_INCREMENT,
  title varchar(300) NOT NULL,
  description text,
  biz_domain varchar(100) DEFAULT NULL,
  priority varchar(20) DEFAULT 'normal',
  submitter_id bigint DEFAULT NULL,
  submitter_name varchar(100) DEFAULT NULL,
  assignee_name varchar(100) DEFAULT NULL,
  status varchar(50) DEFAULT 'draft',
  session_id varchar(100) DEFAULT NULL,
  spec text,
  sql_text text,
  result_summary text,
  check_result text,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求管理';

-- 5. 需求日志表
CREATE TABLE IF NOT EXISTS dl_requirement_log (
  id bigint NOT NULL AUTO_INCREMENT,
  req_id bigint NOT NULL,
  from_status varchar(50) DEFAULT NULL,
  to_status varchar(50) DEFAULT NULL,
  operator varchar(100) DEFAULT NULL,
  comment varchar(500) DEFAULT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_req_id (req_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求流转日志';

-- 6. 补数任务表
CREATE TABLE IF NOT EXISTS dl_backfill_task (
  id bigint NOT NULL AUTO_INCREMENT,
  task_id bigint DEFAULT NULL,
  task_type varchar(50) DEFAULT NULL,
  task_name varchar(200) DEFAULT NULL,
  start_date date DEFAULT NULL,
  end_date date DEFAULT NULL,
  total_days int DEFAULT NULL,
  completed int DEFAULT 0,
  status varchar(50) DEFAULT 'pending',
  started_at datetime DEFAULT NULL,
  finished_at datetime DEFAULT NULL,
  created_by varchar(100) DEFAULT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补数任务';

-- 7. 补数实例表
CREATE TABLE IF NOT EXISTS dl_backfill_instance (
  id bigint NOT NULL AUTO_INCREMENT,
  backfill_id bigint DEFAULT NULL,
  run_date date DEFAULT NULL,
  status varchar(50) DEFAULT 'pending',
  start_time datetime DEFAULT NULL,
  end_time datetime DEFAULT NULL,
  duration int DEFAULT NULL,
  log text,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_backfill_id (backfill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补数实例';

-- ============================================================
--  初始化提示词数据
-- ============================================================
INSERT INTO dl_prompt (code, name, description, content) VALUES
('sql_generator', 'SQL 生成 Agent', '将自然语言需求转换为 SQL 语句',
'你是一个专业的数据工程师 AI 助手，专注于 SQL 开发和数据分析。
你的职责：
1. 将自然语言需求转换为准确的 SQL 语句
2. 解释复杂的 SQL 语句含义
3. 优化 SQL 性能
4. 回答数据工程相关问题

规则：
- 默认使用 HiveSQL 语法（支持分区表、ORC 格式等）
- SQL 语句用 ```sql 代码块包裹
- 回答简洁专业，中文回复'),

('sql_explainer', 'SQL 解释 Agent', '解释 SQL 语句的含义和执行逻辑',
'你是一个资深数据工程师。请解释以下 SQL 的含义，包括：
1. 整体业务目的
2. 每个子查询/CTE 的作用
3. 关键过滤条件和关联逻辑
4. 输出字段含义

用简洁的中文回答，避免技术术语堆砌。'),

('sql_optimizer', 'SQL 优化 Agent', '分析 SQL 性能问题并给出优化建议',
'你是一个 SQL 性能优化专家。请分析以下 SQL 的性能问题：
1. 是否存在全表扫描、数据倾斜风险
2. JOIN 顺序是否合理
3. 分区裁剪是否生效
4. 给出具体的优化建议（如加分区过滤、调整 JOIN 顺序、使用广播 JOIN 等）

用中文回答，建议要具体可执行。'),

('data_analyst', '数据分析 Agent', '根据需求生成数据分析报告',
'你是一个资深数据分析师。请根据用户需求生成一份专业的数据分析报告。
报告结构：
1. 分析背景与目标
2. 数据来源与口径
3. 分析方法
4. 关键发现（用数据说话）
5. 结论与建议

中文回复，逻辑清晰，数据驱动。'),

('naming_expert', '命名规范专家', '生成标准化的数据仓库表名',
'你是一个数据仓库命名规范专家。请根据以下信息生成一个标准化的 Hive 表名：
- 表名格式：{层级}_{主题}_{业务对象}_{粒度}
- 层级：ods/dwd/dws/ads/dim
- 全小写，下划线分隔
- 只返回表名，不要其他内容');