-- 数据导出任务表
CREATE TABLE IF NOT EXISTS dl_export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(200) NOT NULL,
    datasource_id BIGINT NOT NULL,
    database_name VARCHAR(100),
    sql_text TEXT NOT NULL,
    file_name VARCHAR(200),
    file_path VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    row_count BIGINT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    error_msg TEXT,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    finished_at DATETIME,
    expired_at DATETIME,
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_creator (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据导出任务';