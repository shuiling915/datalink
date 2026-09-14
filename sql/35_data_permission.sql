-- ============================================================
-- 数据权限表（行级/列级）
-- ============================================================

CREATE TABLE IF NOT EXISTS dl_data_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id         BIGINT       NOT NULL COMMENT '角色ID',
    datasource_id   BIGINT       DEFAULT NULL COMMENT '数据源ID(NULL=全部)',
    table_name      VARCHAR(128) NOT NULL COMMENT '表名',
    column_name     VARCHAR(128) DEFAULT NULL COMMENT '列名(NULL=行级权限)',
    permission_type VARCHAR(32)  NOT NULL COMMENT 'HIDE隐藏列/MASK脱敏列/ROW_FILTER行过滤',
    rule_value      VARCHAR(512) DEFAULT NULL COMMENT '脱敏规则名 或 行过滤SQL条件',
    enabled         TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    description     VARCHAR(255) DEFAULT NULL,
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_role_id (role_id),
    KEY idx_table (datasource_id, table_name),
    KEY idx_permission_type (permission_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限表(行级/列级)';

-- 初始化数据权限类型字典
INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('data_permission_type', '数据权限类型', '行级列级数据权限', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('data_permission_type', '隐藏列', 'HIDE', 1, 1),
('data_permission_type', '脱敏列', 'MASK', 2, 1),
('data_permission_type', '行过滤', 'ROW_FILTER', 3, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);