-- ============================================================
-- Dataphin 规范定义模块表
-- 设计日期：2026-09-09
-- 说明：复现 Dataphin 的规范定义能力
-- ============================================================

-- 数据域（Data Domain）
CREATE TABLE IF NOT EXISTS dn_data_domain (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_code     VARCHAR(100) NOT NULL COMMENT '数据域编码（唯一）',
    domain_name     VARCHAR(200) NOT NULL COMMENT '数据域名称',
    description     TEXT COMMENT '数据域描述',
    owner           VARCHAR(100) DEFAULT NULL COMMENT '负责人',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_by      VARCHAR(100) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_domain_code (domain_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据域';

-- 业务过程（Business Process）
CREATE TABLE IF NOT EXISTS dn_business_process (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_id       BIGINT NOT NULL COMMENT '所属数据域ID',
    process_code    VARCHAR(100) NOT NULL COMMENT '业务过程编码',
    process_name    VARCHAR(200) NOT NULL COMMENT '业务过程名称',
    description     TEXT COMMENT '业务过程描述',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_by      VARCHAR(100) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_process_code (process_code),
    INDEX idx_domain_id (domain_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务过程';

-- 词根（Word Root）
CREATE TABLE IF NOT EXISTS dn_word_root (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    word_code       VARCHAR(100) NOT NULL COMMENT '词根编码',
    word_name       VARCHAR(200) NOT NULL COMMENT '词根名称',
    word_type       VARCHAR(50) DEFAULT NULL COMMENT '词根类型（时间/状态/类型/金额等）',
    description     TEXT COMMENT '词根描述',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_by      VARCHAR(100) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word_code (word_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='词根';

-- 修饰词（Modifier）
CREATE TABLE IF NOT EXISTS dn_modifier (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    modifier_code   VARCHAR(100) NOT NULL COMMENT '修饰词编码',
    modifier_name   VARCHAR(200) NOT NULL COMMENT '修饰词名称',
    modifier_type   VARCHAR(50) DEFAULT NULL COMMENT '修饰词类型（渠道/地区/用户类型等）',
    description     TEXT COMMENT '修饰词描述',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_by      VARCHAR(100) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_modifier_code (modifier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='修饰词';

-- 时间周期（Time Period）
CREATE TABLE IF NOT EXISTS dn_time_period (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_code     VARCHAR(100) NOT NULL COMMENT '周期编码',
    period_name     VARCHAR(200) NOT NULL COMMENT '周期名称',
    period_type     VARCHAR(50) NOT NULL COMMENT '周期类型（day/week/month/quarter/year）',
    description     TEXT COMMENT '周期描述',
    status          TINYINT DEFAULT 1 COMMENT '1启用 0停用',
    created_by      VARCHAR(100) DEFAULT 'default',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_period_code (period_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间周期';