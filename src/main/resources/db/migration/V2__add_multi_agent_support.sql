-- 为多智能体协同功能添加数据库支持
-- Version: 2
-- Description: Add multi-agent collaboration support

-- =====================================================
-- 1. 扩展task_execution表,添加多Agent相关字段
-- =====================================================

-- 添加父任务ID字段
ALTER TABLE task_execution
ADD COLUMN parent_task_id VARCHAR(100) COMMENT '父任务ID(如果是子任务)';

-- 添加子任务标识字段
ALTER TABLE task_execution
ADD COLUMN is_sub_task BOOLEAN DEFAULT FALSE COMMENT '是否是子任务';

-- 添加子任务执行顺序字段
ALTER TABLE task_execution
ADD COLUMN task_order INT COMMENT '子任务执行顺序';

-- 为parent_task_id添加索引以提高查询效率
CREATE INDEX idx_parent_task_id ON task_execution(parent_task_id);

-- =====================================================
-- 2. 创建multi_agent_execution表
-- =====================================================

CREATE TABLE multi_agent_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_task_id VARCHAR(100) NOT NULL UNIQUE COMMENT '父任务ID(多Agent任务的唯一标识)',
    execution_plan_json TEXT COMMENT '执行计划JSON',
    execution_mode VARCHAR(50) COMMENT '执行模式: SEQUENTIAL, PARALLEL, HIERARCHICAL',
    total_latency_ms INT COMMENT '总执行时长(毫秒)',
    success_count INT COMMENT '成功的子任务数',
    failure_count INT COMMENT '失败的子任务数',
    status VARCHAR(20) NOT NULL COMMENT '执行状态: ok, error, partial',
    user_id VARCHAR(100) COMMENT '用户ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    completed_at DATETIME COMMENT '完成时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_parent_task_id (parent_task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多Agent协同执行记录表';

-- =====================================================
-- 3. 创建视图,方便查询多Agent任务及其子任务
-- =====================================================

CREATE OR REPLACE VIEW v_multi_agent_task_detail AS
SELECT
    m.id AS multi_agent_id,
    m.parent_task_id,
    m.execution_mode,
    m.status AS multi_agent_status,
    m.success_count,
    m.failure_count,
    m.total_latency_ms,
    m.user_id,
    m.created_at AS multi_agent_created_at,
    m.completed_at AS multi_agent_completed_at,
    t.id AS sub_task_id,
    t.task_id AS sub_task_task_id,
    t.agent_name AS sub_task_agent,
    t.task_type AS sub_task_type,
    t.status AS sub_task_status,
    t.task_order,
    t.latency_ms AS sub_task_latency_ms,
    t.started_at AS sub_task_started_at,
    t.completed_at AS sub_task_completed_at
FROM
    multi_agent_execution m
LEFT JOIN
    task_execution t ON m.parent_task_id = t.parent_task_id AND t.is_sub_task = TRUE
ORDER BY
    m.created_at DESC, t.task_order ASC;

-- =====================================================
-- 4. 数据完整性说明
-- =====================================================

-- 说明:
-- 1. parent_task_id在multi_agent_execution表中是唯一的,标识一个多Agent任务
-- 2. task_execution表中的子任务通过parent_task_id关联到multi_agent_execution
-- 3. is_sub_task字段标识该记录是否为子任务
-- 4. task_order字段记录子任务的执行顺序,方便排序和追踪

-- =====================================================
-- 5. 示例查询
-- =====================================================

-- 查询某个多Agent任务的所有子任务
-- SELECT * FROM task_execution
-- WHERE parent_task_id = 'your-parent-task-id'
-- AND is_sub_task = TRUE
-- ORDER BY task_order;

-- 查询某个用户的所有多Agent任务
-- SELECT * FROM multi_agent_execution
-- WHERE user_id = 'your-user-id'
-- ORDER BY created_at DESC;

-- 统计多Agent任务的成功率
-- SELECT
--     status,
--     COUNT(*) as count,
--     AVG(total_latency_ms) as avg_latency,
--     AVG(success_count) as avg_success,
--     AVG(failure_count) as avg_failure
-- FROM multi_agent_execution
-- GROUP BY status;
