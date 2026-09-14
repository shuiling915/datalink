-- ============================================================
-- 数据服务 API 表
-- ============================================================

-- 数据 API 定义表
CREATE TABLE IF NOT EXISTS dl_data_api (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_code        VARCHAR(64)  NOT NULL COMMENT 'API编码(唯一，URL路径)',
    api_name        VARCHAR(128) NOT NULL COMMENT 'API名称',
    description     VARCHAR(512) DEFAULT NULL COMMENT '描述',
    datasource_id   BIGINT       NOT NULL COMMENT '数据源ID',
    sql_template    TEXT         NOT NULL COMMENT 'SQL模板(可用#{param}占位)',
    method          VARCHAR(16)  DEFAULT 'GET' COMMENT 'HTTP方法 GET/POST',
    response_type   VARCHAR(16)  DEFAULT 'json' COMMENT '响应类型 json/csv',
    row_limit       INT          DEFAULT 1000 COMMENT '返回行数上限',
    cache_seconds   INT          DEFAULT 0 COMMENT '缓存秒数(0=不缓存)',
    api_key         VARCHAR(128) DEFAULT NULL COMMENT '调用密钥(为空则无需鉴权)',
    status          TINYINT      DEFAULT 1 COMMENT '1已发布 0未发布',
    call_count      BIGINT       DEFAULT 0 COMMENT '累计调用次数',
    created_by      VARCHAR(64)  DEFAULT NULL,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_api_code (api_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据服务API表';

-- 数据 API 调用日志表
CREATE TABLE IF NOT EXISTS dl_data_api_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    api_id          BIGINT       NOT NULL COMMENT 'API ID',
    api_code        VARCHAR(64)  NOT NULL COMMENT 'API编码',
    caller          VARCHAR(128) DEFAULT NULL COMMENT '调用方标识',
    request_params  TEXT         DEFAULT NULL COMMENT '请求参数JSON',
    response_status VARCHAR(16)  DEFAULT NULL COMMENT 'SUCCESS/FAILED',
    row_count       INT          DEFAULT 0 COMMENT '返回行数',
    duration_ms     INT          DEFAULT 0 COMMENT '耗时(毫秒)',
    error_message   TEXT         DEFAULT NULL COMMENT '错误信息',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_api_id (api_id),
    KEY idx_api_code (api_code),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据API调用日志表';