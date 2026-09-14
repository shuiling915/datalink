-- 数据源分组表
CREATE TABLE IF NOT EXISTS dl_datasource_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL,
    group_code VARCHAR(50),
    description VARCHAR(500),
    sort_order INT DEFAULT 0,
    created_by VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_name (group_name),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源分组';

-- 为数据源表添加分组字段（先检查列是否存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dl_datasource' AND COLUMN_NAME = 'group_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE dl_datasource ADD COLUMN group_id BIGINT DEFAULT NULL COMMENT ''分组ID''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dl_datasource' AND INDEX_NAME = 'idx_group_id');
SET @sql2 = IF(@idx_exists = 0,
    'ALTER TABLE dl_datasource ADD INDEX idx_group_id (group_id)',
    'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;