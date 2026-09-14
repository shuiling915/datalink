-- 实时开发任务表
CREATE TABLE IF NOT EXISTS dl_realtime_task (
  id bigint NOT NULL AUTO_INCREMENT,
  folder_id bigint DEFAULT NULL,
  task_name varchar(200) NOT NULL,
  task_type varchar(50) DEFAULT 'flink_sql',
  content longtext,
  description varchar(500) DEFAULT NULL,
  status varchar(20) DEFAULT 'draft',
  parallelism int DEFAULT 1,
  checkpoint_interval_sec int DEFAULT 60,
  checkpoint_path varchar(500) DEFAULT NULL,
  kafka_bootstrap_servers varchar(500) DEFAULT NULL,
  source_topic varchar(200) DEFAULT NULL,
  sink_table varchar(200) DEFAULT NULL,
  properties text,
  job_id varchar(100) DEFAULT NULL,
  application_id varchar(100) DEFAULT NULL,
  last_error text,
  last_start_time datetime DEFAULT NULL,
  last_stop_time datetime DEFAULT NULL,
  created_by varchar(50) DEFAULT NULL,
  updated_by varchar(50) DEFAULT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时开发任务';

-- 项目空间表
CREATE TABLE IF NOT EXISTS dl_project (
  id bigint NOT NULL AUTO_INCREMENT,
  project_code varchar(100) DEFAULT NULL,
  project_name varchar(200) NOT NULL,
  description varchar(500) DEFAULT NULL,
  owner varchar(50) DEFAULT NULL,
  members text,
  status varchar(20) DEFAULT 'active',
  resource_quota varchar(500) DEFAULT NULL,
  env_config text,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_code (project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目空间';