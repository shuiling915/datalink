-- ============================================================
-- 数据生命周期管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS dl_lifecycle_policy (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    policy_name     VARCHAR(128) NOT NULL COMMENT '策略名称',
    datasource_id   BIGINT       DEFAULT NULL COMMENT '数据源ID',
    database_name   VARCHAR(128) NOT NULL COMMENT '数据库名',
    table_name      VARCHAR(128) NOT NULL COMMENT '表名',
    partition_column VARCHAR(128) DEFAULT NULL COMMENT '分区/时间列名(用于过期判断)',
    policy_type     VARCHAR(32)  NOT NULL COMMENT 'DELETE过期删除/ARCHIVE归档/COLD标记冷数据',
    retention_days  INT          NOT NULL COMMENT '保留天数',
    archive_target  VARCHAR(256) DEFAULT NULL COMMENT '归档目标(归档表名或存储路径)',
    enabled         TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    schedule_cron   VARCHAR(64)  DEFAULT '0 0 2 * * ?' COMMENT '调度cron',
    last_run_at     DATETIME     DEFAULT NULL COMMENT '上次执行时间',
    description     VARCHAR(512) DEFAULT NULL,
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_table (datasource_id, database_name, table_name),
    KEY idx_policy_type (policy_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据生命周期策略表';

CREATE TABLE IF NOT EXISTS dl_lifecycle_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    policy_id       BIGINT       NOT NULL COMMENT '策略ID',
    run_status      VARCHAR(16)  DEFAULT NULL COMMENT 'SUCCESS/FAILED',
    affected_rows   BIGINT       DEFAULT 0 COMMENT '影响行数',
    exec_sql        TEXT         DEFAULT NULL COMMENT '执行SQL',
    error_message   TEXT         DEFAULT NULL COMMENT '错误信息',
    duration_ms     INT          DEFAULT 0 COMMENT '耗时(毫秒)',
    started_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    finished_at     DATETIME     DEFAULT NULL,
    KEY idx_policy_id (policy_id),
    KEY idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生命周期执行日志表';

-- 初始化策略类型字典
INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('lifecycle_policy_type', '生命周期策略类型', '数据生命周期管理策略', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('lifecycle_policy_type', '过期删除', 'DELETE', 1, 1),
('lifecycle_policy_type', '归档迁移', 'ARCHIVE', 2, 1),
('lifecycle_policy_type', '冷数据标记', 'COLD', 3, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);