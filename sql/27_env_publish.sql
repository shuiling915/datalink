-- DataNote 双环境 + 灰度发布体系

-- 1. 为 dn_script 增加环境字段
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'environment') = 0,
    'ALTER TABLE dn_script ADD COLUMN environment VARCHAR(16) DEFAULT ''dev'' COMMENT ''环境: dev/prod''',
    'SELECT ''environment already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 为 dn_script 增加关联字段（dev脚本与prod脚本的关联）
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'dev_script_id') = 0,
    'ALTER TABLE dn_script ADD COLUMN dev_script_id BIGINT DEFAULT NULL COMMENT ''关联的开发环境脚本ID(prod环境用)''',
    'SELECT ''dev_script_id already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'prod_script_id') = 0,
    'ALTER TABLE dn_script ADD COLUMN prod_script_id BIGINT DEFAULT NULL COMMENT ''关联的生产环境脚本ID(dev环境用)''',
    'SELECT ''prod_script_id already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 脚本发布记录表
CREATE TABLE IF NOT EXISTS dn_script_publish (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dev_script_id   BIGINT       NOT NULL COMMENT '开发环境脚本ID',
    prod_script_id  BIGINT       DEFAULT NULL COMMENT '生产环境脚本ID',
    publish_version VARCHAR(32)  NOT NULL COMMENT '发布版本号 v1.0.0',
    publish_type    VARCHAR(16)  DEFAULT 'normal' COMMENT 'normal=正式发布 / rollback=回滚',
    publish_status  VARCHAR(16)  DEFAULT 'pending' COMMENT 'pending/grayscale_testing/published/failed/rolled_back',
    
    -- 内容快照
    dev_content     LONGTEXT     DEFAULT NULL COMMENT '发布时dev脚本内容快照',
    prod_content_before LONGTEXT DEFAULT NULL COMMENT '发布前prod脚本内容(用于回滚)',
    
    -- 灰度测试
    grayscale_enabled   TINYINT  DEFAULT 0 COMMENT '是否启用灰度测试: 0否/1是',
    grayscale_limit     INT      DEFAULT 100 COMMENT '灰度测试采样行数',
    grayscale_dev_rows  BIGINT   DEFAULT NULL COMMENT '灰度测试dev返回行数',
    grayscale_prod_rows BIGINT  DEFAULT NULL COMMENT '灰度测试prod返回行数',
    grayscale_dev_time  BIGINT   DEFAULT NULL COMMENT '灰度测试dev耗时(ms)',
    grayscale_prod_time BIGINT  DEFAULT NULL COMMENT '灰度测试prod耗时(ms)',
    grayscale_match     TINYINT  DEFAULT NULL COMMENT '灰度结果是否一致: 0不一致/1一致',
    grayscale_detail    TEXT     DEFAULT NULL COMMENT '灰度测试详情(JSON)',
    
    -- 发布信息
    published_by    VARCHAR(64)  DEFAULT NULL COMMENT '发布人',
    publish_comment VARCHAR(500) DEFAULT NULL COMMENT '发布说明',
    published_at    DATETIME     DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_dev_script (dev_script_id),
    INDEX idx_prod_script (prod_script_id),
    INDEX idx_status (publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='脚本发布记录';

-- 4. 更新已有脚本的 environment 字段为 dev
UPDATE dn_script SET environment = 'dev' WHERE environment IS NULL;