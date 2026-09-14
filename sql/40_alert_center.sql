-- 告警事件表
CREATE TABLE IF NOT EXISTS dl_alert_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(50) NOT NULL COMMENT '来源: quality/task/lifecycle/api/system',
    source_id BIGINT COMMENT '来源记录ID',
    severity VARCHAR(20) NOT NULL COMMENT '严重级别: critical/warning/info',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/ACKNOWLEDGED/RESOLVED',
    assignee VARCHAR(100),
    acknowledged_by VARCHAR(100),
    acknowledged_at DATETIME,
    resolved_by VARCHAR(100),
    resolved_at DATETIME,
    resolve_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source (source_type, source_id),
    INDEX idx_status (status),
    INDEX idx_severity (severity),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警事件';