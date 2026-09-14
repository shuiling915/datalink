-- ============================================================
-- 数据资产目录表
-- ============================================================

CREATE TABLE IF NOT EXISTS dl_asset (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    datasource_id   BIGINT       DEFAULT NULL COMMENT '数据源ID',
    table_name      VARCHAR(128) NOT NULL COMMENT '表名',
    asset_name      VARCHAR(256) DEFAULT NULL COMMENT '资产名称(业务名称)',
    description     TEXT         DEFAULT NULL COMMENT '资产描述',
    asset_level     TINYINT      DEFAULT 1 COMMENT '资产分级:1公开 2内部 3机密 4绝密',
    owner           VARCHAR(64)  DEFAULT NULL COMMENT '资产负责人',
    business_domain VARCHAR(64)  DEFAULT NULL COMMENT '业务域',
    tags            VARCHAR(512) DEFAULT NULL COMMENT '标签(逗号分隔)',
    access_count    BIGINT       DEFAULT 0 COMMENT '访问次数',
    last_access_at  DATETIME     DEFAULT NULL COMMENT '最近访问时间',
    status          TINYINT      DEFAULT 1 COMMENT '1上架 0下架',
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_table (datasource_id, table_name),
    KEY idx_owner (owner),
    KEY idx_level (asset_level),
    KEY idx_business_domain (business_domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据资产目录表';

CREATE TABLE IF NOT EXISTS dl_asset_tag (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tag_name        VARCHAR(64)  NOT NULL COMMENT '标签名称',
    tag_color       VARCHAR(16)  DEFAULT '#1890ff' COMMENT '标签颜色',
    category        VARCHAR(64)  DEFAULT NULL COMMENT '标签分类',
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产标签表';

CREATE TABLE IF NOT EXISTS dl_asset_favorite (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    asset_id        BIGINT       NOT NULL COMMENT '资产ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_asset_user (asset_id, user_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产收藏表';

-- 初始化标签
INSERT INTO dl_asset_tag (tag_name, tag_color, category) VALUES
('核心表', '#f5222d', '重要性'),
('常用表', '#fa8c16', '重要性'),
('已归档', '#8c8c8c', '状态'),
('需治理', '#faad14', '状态')
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- 初始化资产分级字典
INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('asset_level', '资产分级', '数据资产安全分级', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('asset_level', '公开', '1', 1, 1),
('asset_level', '内部', '2', 2, 1),
('asset_level', '机密', '3', 3, 1),
('asset_level', '绝密', '4', 4, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);