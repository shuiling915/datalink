-- 告警通知渠道配置表
CREATE TABLE IF NOT EXISTS dl_alert_channel (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    channel_name VARCHAR(64)  NOT NULL COMMENT '渠道名称',
    channel_type VARCHAR(32)  NOT NULL COMMENT '渠道类型: dingtalk/wechat/email/webhook',
    config       TEXT         DEFAULT NULL COMMENT '渠道配置JSON',
    enabled      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警通知渠道';