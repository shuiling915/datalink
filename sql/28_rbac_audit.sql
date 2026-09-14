-- ============================================================
-- RBAC 权限体系 + 操作审计日志
-- ============================================================

-- 用户表（如不存在则创建）
CREATE TABLE IF NOT EXISTS dl_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL COMMENT '登录名',
    password    VARCHAR(128) NOT NULL COMMENT 'bcrypt加密密码',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    role        VARCHAR(32)  DEFAULT 'USER' COMMENT '角色（兜底字段）',
    status      TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS dl_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(64)  NOT NULL COMMENT '角色编码，如 ADMIN/DEVELOPER/VIEWER',
    role_name   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS dl_permission (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    perm_code       VARCHAR(128) NOT NULL COMMENT '权限编码，如 script:create / datasource:delete',
    perm_name       VARCHAR(128) NOT NULL COMMENT '权限名称',
    perm_group      VARCHAR(64)  DEFAULT NULL COMMENT '权限分组',
    description     VARCHAR(255) DEFAULT NULL COMMENT '描述',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS dl_user_role (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS dl_role_permission (
    id            BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT   NOT NULL COMMENT '角色ID',
    permission_id BIGINT   NOT NULL COMMENT '权限ID',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 操作审计日志表
CREATE TABLE IF NOT EXISTS dl_audit_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       DEFAULT NULL COMMENT '操作人ID',
    username     VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名',
    module       VARCHAR(64)  DEFAULT NULL COMMENT '模块',
    operation    VARCHAR(128) DEFAULT NULL COMMENT '操作描述',
    method       VARCHAR(16)  DEFAULT NULL COMMENT 'HTTP方法',
    request_uri  VARCHAR(512) DEFAULT NULL COMMENT '请求URI',
    request_params TEXT       DEFAULT NULL COMMENT '请求参数',
    ip_address   VARCHAR(64)  DEFAULT NULL COMMENT 'IP地址',
    status       TINYINT      DEFAULT NULL COMMENT '1成功 0失败',
    error_msg    VARCHAR(1024) DEFAULT NULL COMMENT '错误信息',
    cost_ms      BIGINT       DEFAULT NULL COMMENT '耗时(ms)',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- 初始化默认角色
INSERT INTO dl_role (role_code, role_name, description, status) VALUES
('ADMIN', '管理员', '拥有所有权限', 1),
('DEVELOPER', '开发者', '数据开发与运维权限', 1),
('VIEWER', '只读用户', '仅查看权限', 1)
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name);

-- 初始化默认权限
INSERT INTO dl_permission (perm_code, perm_name, perm_group) VALUES
('script:create', '创建脚本', '数据开发'),
('script:edit', '编辑脚本', '数据开发'),
('script:delete', '删除脚本', '数据开发'),
('script:execute', '执行脚本', '数据开发'),
('script:publish', '发布脚本', '数据开发'),
('datasource:create', '创建数据源', '数据源'),
('datasource:edit', '编辑数据源', '数据源'),
('datasource:delete', '删除数据源', '数据源'),
('datasource:view', '查看数据源', '数据源'),
('sync:create', '创建同步任务', '数据集成'),
('sync:edit', '编辑同步任务', '数据集成'),
('sync:delete', '删除同步任务', '数据集成'),
('sync:execute', '执行同步任务', '数据集成'),
('quality:create', '创建质量规则', '数据质量'),
('quality:execute', '执行质量检查', '数据质量'),
('model:create', '创建模型', '数据建模'),
('model:edit', '编辑模型', '数据建模'),
('model:publish', '发布模型', '数据建模'),
('user:manage', '用户管理', '系统管理'),
('role:manage', '角色管理', '系统管理'),
('system:config', '系统配置', '系统管理'),
('sql:approve', 'SQL审批', '数据开发')
ON DUPLICATE KEY UPDATE perm_name=VALUES(perm_name);

-- 管理员拥有所有权限
INSERT INTO dl_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM dl_role r, dl_permission p WHERE r.role_code = 'ADMIN';

-- 开发者权限
INSERT INTO dl_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM dl_role r, dl_permission p
WHERE r.role_code = 'DEVELOPER' AND p.perm_code NOT IN ('user:manage', 'role:manage', 'system:config');

-- 只读用户权限
INSERT INTO dl_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM dl_role r, dl_permission p
WHERE r.role_code = 'VIEWER' AND p.perm_code LIKE '%:view';

-- 给已有用户分配默认角色（如果用户表存在）
-- ADMIN 角色用户分配管理员角色，其他分配开发者角色
INSERT IGNORE INTO dl_user_role (user_id, role_id)
SELECT u.id, r.id FROM dl_user u, dl_role r
WHERE (u.role = 'ADMIN' AND r.role_code = 'ADMIN')
   OR (u.role != 'ADMIN' AND r.role_code = 'DEVELOPER');