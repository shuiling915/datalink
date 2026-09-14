-- ============================================================
-- 数据字典表
-- 用于管理系统中的枚举值、状态码、分类等
-- ============================================================

-- 字典类型表
CREATE TABLE IF NOT EXISTS dl_dict_type (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dict_code    VARCHAR(64)  NOT NULL COMMENT '字典类型编码（唯一）',
    dict_name    VARCHAR(128) NOT NULL COMMENT '字典类型名称',
    description  VARCHAR(255) DEFAULT NULL COMMENT '描述',
    status       TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    created_by   VARCHAR(64)  DEFAULT NULL,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典项表
CREATE TABLE IF NOT EXISTS dl_dict_item (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dict_code    VARCHAR(64)  NOT NULL COMMENT '所属字典类型编码',
    item_label   VARCHAR(128) NOT NULL COMMENT '字典项显示名称',
    item_value   VARCHAR(255) NOT NULL COMMENT '字典项值',
    sort_order   INT          DEFAULT 0 COMMENT '排序（升序）',
    status       TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    remark       VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dict_code (dict_code),
    UNIQUE KEY uk_dict_value (dict_code, item_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 初始化常用字典类型
INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('task_status', '任务状态', '数据开发任务运行状态', 1),
('schedule_status', '调度状态', '脚本调度上线状态', 1),
('alert_level', '告警级别', '告警通知级别', 1),
('data_source_type', '数据源类型', '支持的数据源类型', 1),
('sql_approval_status', 'SQL审批状态', 'SQL审批流程状态', 1),
('quality_rule_type', '质量规则类型', '数据质量检查规则类型', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

-- 初始化任务状态字典项
INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('task_status', '等待中', 'pending', 1, 1),
('task_status', '运行中', 'running', 2, 1),
('task_status', '成功', 'success', 3, 1),
('task_status', '失败', 'failed', 4, 1),
('task_status', '已跳过', 'skipped', 5, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

-- 初始化调度状态字典项
INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('schedule_status', '草稿', 'draft', 1, 1),
('schedule_status', '已上线', 'online', 2, 1),
('schedule_status', '已下线', 'offline', 3, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

-- 初始化告警级别字典项
INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('alert_level', '信息', 'info', 1, 1),
('alert_level', '警告', 'warning', 2, 1),
('alert_level', '严重', 'critical', 3, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

-- 初始化数据源类型字典项
INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('data_source_type', 'MySQL', 'mysql', 1, 1),
('data_source_type', 'PostgreSQL', 'postgresql', 2, 1),
('data_source_type', 'ClickHouse', 'clickhouse', 3, 1),
('data_source_type', 'Oracle', 'oracle', 4, 1),
('data_source_type', 'Hive', 'hive', 5, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

-- 初始化SQL审批状态字典项
INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('sql_approval_status', '待审批', 'pending', 1, 1),
('sql_approval_status', '已通过', 'approved', 2, 1),
('sql_approval_status', '已驳回', 'rejected', 3, 1),
('sql_approval_status', '已执行', 'executed', 4, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

-- 初始化质量规则类型字典项
INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('quality_rule_type', '空值检查', 'null_check', 1, 1),
('quality_rule_type', '唯一性检查', 'unique_check', 2, 1),
('quality_rule_type', '值域检查', 'value_range', 3, 1),
('quality_rule_type', '正则检查', 'regex_check', 4, 1),
('quality_rule_type', '自定义SQL', 'custom_sql', 5, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);