-- ============================================================
-- Dataphin 数据建模模块表
-- 设计日期：2026-09-09
-- 说明：复现 Dataphin 的数据建模能力（维度表、事实表、汇总表）
-- ============================================================

-- 维度表（Dimension Table）
CREATE TABLE IF NOT EXISTS dn_dimension (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dim_code        VARCHAR(100) NOT NULL COMMENT '维度编码',
    dim_name        VARCHAR(200) NOT NULL COMMENT '维度名称',
    domain_id       BIGINT NOT NULL COMMENT '所属数据域ID',
    description     TEXT COMMENT '维度描述',
    layer           VARCHAR(20) DEFAULT 'DIM' COMMENT '层级（DIM）',
    status          VARCHAR(20) DEFAULT 'draft' COMMENT '状态（draft开发中/published已发布/deprecated已下线）',
    publish_version INT DEFAULT 0 COMMENT '发布版本号',
    created_by      VARCHAR(100) DEFAULT 'default',
    published_by    VARCHAR(100) DEFAULT NULL COMMENT '发布人',
    published_at    DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dim_code (dim_code),
    INDEX idx_domain_id (domain_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维度表';

-- 维度字段
CREATE TABLE IF NOT EXISTS dn_dimension_field (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dim_id          BIGINT NOT NULL COMMENT '维度ID',
    field_name      VARCHAR(200) NOT NULL COMMENT '字段名',
    field_type      VARCHAR(50) NOT NULL COMMENT '字段类型（string/int/bigint/decimal/datetime等）',
    field_comment   VARCHAR(500) DEFAULT NULL COMMENT '字段注释',
    is_primary      TINYINT DEFAULT 0 COMMENT '是否主键（0否 1是）',
    is_partition    TINYINT DEFAULT 0 COMMENT '是否分区字段（0否 1是）',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dim_id (dim_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维度字段';

-- 事实表（Fact Table）
CREATE TABLE IF NOT EXISTS dn_fact_table (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fact_code       VARCHAR(100) NOT NULL COMMENT '事实表编码',
    fact_name       VARCHAR(200) NOT NULL COMMENT '事实表名称',
    domain_id       BIGINT NOT NULL COMMENT '所属数据域ID',
    process_id      BIGINT NOT NULL COMMENT '所属业务过程ID',
    fact_type       VARCHAR(30) NOT NULL COMMENT '事实表类型（transaction事务/periodic_snapshot周期快照/accumulating_snapshot累积快照）',
    description     TEXT COMMENT '事实表描述',
    layer           VARCHAR(20) DEFAULT 'DWD' COMMENT '层级（DWD/DWS）',
    status          VARCHAR(20) DEFAULT 'draft' COMMENT '状态（draft开发中/published已发布/deprecated已下线）',
    publish_version INT DEFAULT 0 COMMENT '发布版本号',
    created_by      VARCHAR(100) DEFAULT 'default',
    published_by    VARCHAR(100) DEFAULT NULL COMMENT '发布人',
    published_at    DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fact_code (fact_code),
    INDEX idx_domain_id (domain_id),
    INDEX idx_process_id (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事实表';

-- 事实表字段
CREATE TABLE IF NOT EXISTS dn_fact_field (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fact_id         BIGINT NOT NULL COMMENT '事实表ID',
    field_name      VARCHAR(200) NOT NULL COMMENT '字段名',
    field_type      VARCHAR(50) NOT NULL COMMENT '字段类型',
    field_comment   VARCHAR(500) DEFAULT NULL COMMENT '字段注释',
    field_category  VARCHAR(30) NOT NULL COMMENT '字段类别（dimension维度/measure度量/attribute属性/partition分区）',
    related_dim_id  BIGINT DEFAULT NULL COMMENT '关联维度ID（维度字段用）',
    is_primary      TINYINT DEFAULT 0 COMMENT '是否主键',
    is_partition    TINYINT DEFAULT 0 COMMENT '是否分区字段',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fact_id (fact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事实表字段';

-- 汇总表（Summary Table）
CREATE TABLE IF NOT EXISTS dn_summary_table (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_code    VARCHAR(100) NOT NULL COMMENT '汇总表编码',
    summary_name    VARCHAR(200) NOT NULL COMMENT '汇总表名称',
    domain_id       BIGINT NOT NULL COMMENT '所属数据域ID',
    description     TEXT COMMENT '汇总表描述',
    layer           VARCHAR(20) DEFAULT 'DWS' COMMENT '层级（DWS/ADS）',
    status          VARCHAR(20) DEFAULT 'draft' COMMENT '状态（draft开发中/published已发布/deprecated已下线）',
    publish_version INT DEFAULT 0 COMMENT '发布版本号',
    created_by      VARCHAR(100) DEFAULT 'default',
    published_by    VARCHAR(100) DEFAULT NULL COMMENT '发布人',
    published_at    DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_summary_code (summary_code),
    INDEX idx_domain_id (domain_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='汇总表';

-- 汇总表字段
CREATE TABLE IF NOT EXISTS dn_summary_field (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_id      BIGINT NOT NULL COMMENT '汇总表ID',
    field_name      VARCHAR(200) NOT NULL COMMENT '字段名',
    field_type      VARCHAR(50) NOT NULL COMMENT '字段类型',
    field_comment   VARCHAR(500) DEFAULT NULL COMMENT '字段注释',
    is_primary      TINYINT DEFAULT 0 COMMENT '是否主键',
    is_partition    TINYINT DEFAULT 0 COMMENT '是否分区字段',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_summary_id (summary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='汇总表字段';

-- 模型发布历史
CREATE TABLE IF NOT EXISTS dn_model_publish_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    model_type      VARCHAR(30) NOT NULL COMMENT '模型类型（dimension/fact/summary）',
    model_id        BIGINT NOT NULL COMMENT '模型ID',
    model_code      VARCHAR(100) NOT NULL COMMENT '模型编码',
    model_name      VARCHAR(200) NOT NULL COMMENT '模型名称',
    version         INT NOT NULL COMMENT '版本号',
    ddl_content     LONGTEXT COMMENT 'DDL内容',
    publish_status  VARCHAR(20) DEFAULT 'success' COMMENT '发布状态（success/failed）',
    publish_msg     TEXT COMMENT '发布消息',
    published_by    VARCHAR(100) DEFAULT 'default',
    published_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_model (model_type, model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型发布历史';