-- 神工  数据库表结构

-- 任务执行记录表
CREATE TABLE IF NOT EXISTS task_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(100) NOT NULL,
    task_type VARCHAR(100) NOT NULL,
    domain VARCHAR(50) NOT NULL,
    agent_name VARCHAR(100),
    trace_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'ok, error, partial',
    summary TEXT,
    payload JSON,
    result JSON,
    error_message TEXT,
    user_id VARCHAR(100),
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    latency_ms INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id),
    INDEX idx_started_at (started_at),
    INDEX idx_task_type (task_type),
    INDEX idx_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行记录表';

-- Agent 配置表
CREATE TABLE IF NOT EXISTS agent_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT 'Agent 名称',
    type VARCHAR(50) NOT NULL COMMENT 'Agent 类型',
    domain VARCHAR(50) COMMENT '业务域',
    description TEXT COMMENT '描述',
    config JSON COMMENT '配置 JSON',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    version VARCHAR(20) DEFAULT '1.0.0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (enabled),
    INDEX idx_domain (domain)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 配置表';

-- Tool 配置表
CREATE TABLE IF NOT EXISTS tool_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT 'Tool 名称',
    category VARCHAR(50) COMMENT 'Tool 类别',
    description TEXT COMMENT '描述',
    config JSON COMMENT '配置 JSON',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_enabled (enabled),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tool 配置表';
