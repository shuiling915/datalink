-- ============================================================
-- 数据血缘表
-- ============================================================

CREATE TABLE IF NOT EXISTS dl_lineage (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_db       VARCHAR(128) NOT NULL COMMENT '源库',
    source_table    VARCHAR(128) NOT NULL COMMENT '源表',
    target_db       VARCHAR(128) NOT NULL COMMENT '目标库',
    target_table    VARCHAR(128) NOT NULL COMMENT '目标表',
    transform_type  VARCHAR(32)  DEFAULT 'ETL' COMMENT '转换类型:ETL/ELT/REPLICA/AGG',
    transform_sql   TEXT         DEFAULT NULL COMMENT '转换SQL',
    job_id          BIGINT       DEFAULT NULL COMMENT '关联任务ID',
    job_name        VARCHAR(128) DEFAULT NULL COMMENT '关联任务名称',
    frequency       VARCHAR(32)  DEFAULT 'daily' COMMENT '同步频率:realtime/hourly/daily/weekly',
    owner           VARCHAR(64)  DEFAULT NULL COMMENT '负责人',
    description     VARCHAR(512) DEFAULT NULL,
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_source (source_db, source_table),
    KEY idx_target (target_db, target_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘关系表';

CREATE TABLE IF NOT EXISTS dl_lineage_column (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lineage_id      BIGINT       NOT NULL COMMENT '血缘ID',
    source_column   VARCHAR(128) DEFAULT NULL COMMENT '源列',
    target_column   VARCHAR(128) DEFAULT NULL COMMENT '目标列',
    transform_expr  VARCHAR(512) DEFAULT NULL COMMENT '转换表达式',
    KEY idx_lineage_id (lineage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘字段映射表';

INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('lineage_transform_type', '血缘转换类型', '数据血缘转换类型', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('lineage_transform_type', 'ETL', 'ETL', 1, 1),
('lineage_transform_type', 'ELT', 'ELT', 2, 1),
('lineage_transform_type', '数据复制', 'REPLICA', 3, 1),
('lineage_transform_type', '聚合统计', 'AGG', 4, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('lineage_frequency', '同步频率', '数据同步频率', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('lineage_frequency', '实时', 'realtime', 1, 1),
('lineage_frequency', '每小时', 'hourly', 2, 1),
('lineage_frequency', '每天', 'daily', 3, 1),
('lineage_frequency', '每周', 'weekly', 4, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);