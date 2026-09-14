-- SQL片段/模板库表
CREATE TABLE IF NOT EXISTS dl_sql_snippet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    snippet_type VARCHAR(32) DEFAULT 'custom' COMMENT 'system/custom/template',
    sql_text TEXT NOT NULL,
    description VARCHAR(500),
    category VARCHAR(100) COMMENT '分类: 查询/统计/运维/清洗',
    variables VARCHAR(1000) COMMENT '变量定义 JSON: [{"name":"start_date","default":"2024-01-01","desc":"开始日期"}]',
    tags VARCHAR(500),
    created_by VARCHAR(100),
    is_public TINYINT DEFAULT 0 COMMENT '0私有 1公开',
    usage_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_creator (created_by),
    INDEX idx_category (category),
    INDEX idx_usage (usage_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SQL片段/模板库';