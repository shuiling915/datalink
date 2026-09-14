-- ============================================================
-- 站内消息与公告表
-- ============================================================

-- 系统公告表
CREATE TABLE IF NOT EXISTS dl_announcement (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL COMMENT '公告标题',
    content      TEXT         NOT NULL COMMENT '公告内容',
    type         VARCHAR(32)  DEFAULT 'notice' COMMENT '类型: notice通知/announcement公告/maintenance维护',
    priority     TINYINT      DEFAULT 0 COMMENT '优先级: 0普通 1重要 2紧急',
    status       TINYINT      DEFAULT 1 COMMENT '1已发布 0草稿',
    publish_time DATETIME     DEFAULT NULL COMMENT '发布时间',
    created_by   VARCHAR(64)  DEFAULT NULL,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_status (status),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 站内消息表
CREATE TABLE IF NOT EXISTS dl_message (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receiver     VARCHAR(64)  NOT NULL COMMENT '接收人用户名',
    title        VARCHAR(200) NOT NULL COMMENT '消息标题',
    content      TEXT         DEFAULT NULL COMMENT '消息内容',
    type         VARCHAR(32)  DEFAULT 'system' COMMENT '类型: system系统/task任务/approval审批/alert告警',
    biz_id       BIGINT       DEFAULT NULL COMMENT '关联业务ID(如任务ID/审批ID)',
    is_read      TINYINT      DEFAULT 0 COMMENT '0未读 1已读',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_receiver (receiver),
    KEY idx_is_read (is_read),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- 初始化公告类型字典
INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('announcement_type', '公告类型', '系统公告分类', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('announcement_type', '通知', 'notice', 1, 1),
('announcement_type', '公告', 'announcement', 2, 1),
('announcement_type', '维护', 'maintenance', 3, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);

-- 初始化消息类型字典
INSERT INTO dl_dict_type (dict_code, dict_name, description, status) VALUES
('message_type', '消息类型', '站内消息分类', 1)
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name);

INSERT INTO dl_dict_item (dict_code, item_label, item_value, sort_order, status) VALUES
('message_type', '系统消息', 'system', 1, 1),
('message_type', '任务通知', 'task', 2, 1),
('message_type', '审批通知', 'approval', 3, 1),
('message_type', '告警消息', 'alert', 4, 1)
ON DUPLICATE KEY UPDATE item_label = VALUES(item_label);