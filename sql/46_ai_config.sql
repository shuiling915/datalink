-- AI 模型配置表（支持多套配置，API Key AES 加密存储）
CREATE TABLE IF NOT EXISTS dl_ai_config (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL COMMENT '配置名称',
  provider varchar(50) DEFAULT NULL COMMENT 'bailian/anthropic/openai/deepseek/custom',
  base_url varchar(500) DEFAULT NULL COMMENT 'API Base URL',
  model varchar(200) DEFAULT NULL COMMENT '模型名称',
  api_key varchar(1000) DEFAULT NULL COMMENT 'API Key（AES 加密存储）',
  is_default int DEFAULT 0 COMMENT '1=默认',
  status int DEFAULT 1 COMMENT '1=启用 0=禁用',
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 模型配置';