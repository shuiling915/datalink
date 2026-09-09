-- DataNote 数据开发模块增强
-- 为脚本表增加数据分层字段（使用存储过程兼容低版本MySQL）

-- 为 dn_script 表增加字段
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'task_layer') = 0,
    'ALTER TABLE dn_script ADD COLUMN task_layer VARCHAR(16) DEFAULT ''ODS'' COMMENT ''数据分层: ODS/DWD/DWS/ADS/DIM''',
    'SELECT ''task_layer already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'task_status') = 0,
    'ALTER TABLE dn_script ADD COLUMN task_status VARCHAR(16) DEFAULT ''draft'' COMMENT ''任务状态: draft/online/offline''',
    'SELECT ''task_status already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'owner') = 0,
    'ALTER TABLE dn_script ADD COLUMN owner VARCHAR(64) DEFAULT NULL COMMENT ''负责人''',
    'SELECT ''owner already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_script' AND COLUMN_NAME = 'priority') = 0,
    'ALTER TABLE dn_script ADD COLUMN priority INT DEFAULT 0 COMMENT ''优先级: 0低/1中/2高''',
    'SELECT ''priority already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 dn_sync_task 表增加字段
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = 'datanote' AND TABLE_NAME = 'dn_sync_task' AND COLUMN_NAME = 'task_layer') = 0,
    'ALTER TABLE dn_sync_task ADD COLUMN task_layer VARCHAR(16) DEFAULT ''ODS'' COMMENT ''数据分层: ODS/DWD/DWS/ADS/DIM''',
    'SELECT ''task_layer already exists'' AS msg'
));
PREPARE stmt FROM @s;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 任务执行历史表（增强版，保留原 dn_scheduler_run 兼容）
CREATE TABLE IF NOT EXISTS dn_task_execution (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    script_id     BIGINT       DEFAULT NULL COMMENT '脚本ID',
    sync_task_id  BIGINT       DEFAULT NULL COMMENT '同步任务ID',
    task_type     VARCHAR(16)  NOT NULL COMMENT 'script / syncTask',
    trigger_type  VARCHAR(16)  DEFAULT 'manual' COMMENT 'manual/daily/backfill',
    status        VARCHAR(16)  DEFAULT 'WAITING' COMMENT 'WAITING/RUNNING/SUCCESS/FAILED',
    start_time    DATETIME     DEFAULT NULL,
    end_time      DATETIME     DEFAULT NULL,
    duration      INT          DEFAULT 0 COMMENT '执行耗时(秒)',
    read_count    BIGINT       DEFAULT 0,
    write_count   BIGINT       DEFAULT 0,
    error_count   BIGINT       DEFAULT 0,
    log           LONGTEXT     DEFAULT NULL COMMENT '执行日志',
    executor      VARCHAR(64)  DEFAULT NULL COMMENT '执行人',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_script (script_id),
    INDEX idx_sync_task (sync_task_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行历史';