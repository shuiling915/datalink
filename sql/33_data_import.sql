-- ============================================================
-- 数据导入记录表
-- ============================================================

CREATE TABLE IF NOT EXISTS dl_data_import (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size       BIGINT       DEFAULT NULL COMMENT '文件大小(字节)',
    datasource_id   BIGINT       DEFAULT NULL COMMENT '目标数据源ID',
    database_name   VARCHAR(128) DEFAULT NULL COMMENT '目标库名',
    table_name      VARCHAR(128) NOT NULL COMMENT '目标表名',
    row_count       INT          DEFAULT 0 COMMENT '导入行数',
    column_count    INT          DEFAULT 0 COMMENT '列数',
    columns_info    TEXT         DEFAULT NULL COMMENT '列信息JSON',
    status          VARCHAR(16)  DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED/PARTIAL',
    error_message   TEXT         DEFAULT NULL COMMENT '错误信息',
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_created_by (created_by),
    KEY idx_table_name (table_name),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入记录表';