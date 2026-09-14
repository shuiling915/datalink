-- SQL查询历史表
CREATE TABLE IF NOT EXISTS dl_sql_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    datasource_id BIGINT DEFAULT NULL COMMENT '数据源ID',
    datasource_name VARCHAR(128) DEFAULT NULL COMMENT '数据源名称',
    database_name VARCHAR(128) DEFAULT NULL COMMENT '数据库名',
    table_name VARCHAR(256) DEFAULT NULL COMMENT '涉及的表名(解析SQL得到)',
    sql_text TEXT COMMENT '执行的SQL语句',
    sql_type VARCHAR(32) DEFAULT NULL COMMENT 'SQL类型: SELECT/INSERT/UPDATE/DELETE/CREATE等',
    execution_status VARCHAR(32) DEFAULT NULL COMMENT '执行状态: SUCCESS/FAILED',
    duration_ms BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
    affected_rows BIGINT DEFAULT NULL COMMENT '影响行数',
    error_msg TEXT COMMENT '错误信息',
    row_limit INT DEFAULT NULL COMMENT '返回行数限制',
    client_ip VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    INDEX idx_user_id (user_id),
    INDEX idx_datasource_id (datasource_id),
    INDEX idx_created_at (created_at),
    INDEX idx_sql_type (sql_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL查询历史';

-- SQL收藏表
CREATE TABLE IF NOT EXISTS dl_sql_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    title VARCHAR(256) NOT NULL COMMENT '收藏标题',
    sql_text TEXT NOT NULL COMMENT 'SQL语句',
    datasource_id BIGINT DEFAULT NULL COMMENT '数据源ID',
    database_name VARCHAR(128) DEFAULT NULL COMMENT '数据库名',
    description VARCHAR(512) DEFAULT NULL COMMENT '说明',
    tags VARCHAR(256) DEFAULT NULL COMMENT '标签(逗号分隔)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL收藏';