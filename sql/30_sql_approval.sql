-- SQL 审批流程表
CREATE TABLE IF NOT EXISTS dl_sql_approval (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(256) DEFAULT NULL COMMENT '申请标题',
    sql_text      TEXT         NOT NULL COMMENT '待审批SQL',
    sql_type      VARCHAR(32)  NOT NULL COMMENT 'SQL类型: SELECT/INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/CREATE',
    datasource_id BIGINT       DEFAULT NULL COMMENT '数据源ID',
    database_name VARCHAR(100) DEFAULT NULL COMMENT '数据库名',
    applicant     VARCHAR(64)  NOT NULL COMMENT '申请人',
    applicant_remark VARCHAR(512) DEFAULT NULL COMMENT '申请说明',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/EXECUTED',
    approver      VARCHAR(64)  DEFAULT NULL COMMENT '审批人',
    approve_remark VARCHAR(512) DEFAULT NULL COMMENT '审批意见',
    approve_time  DATETIME     DEFAULT NULL COMMENT '审批时间',
    executor      VARCHAR(64)  DEFAULT NULL COMMENT '执行人',
    execute_time  DATETIME     DEFAULT NULL COMMENT '执行时间',
    execute_result TEXT        DEFAULT NULL COMMENT '执行结果',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status (status),
    KEY idx_applicant (applicant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL审批流程';