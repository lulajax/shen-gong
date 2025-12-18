package com.shengong.agentruntime.entity;

import com.shengong.agentruntime.converter.JsonMapConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 多Agent协同执行记录实体
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "multi_agent_execution", indexes = {
        @Index(name = "idx_parent_task_id", columnList = "parentTaskId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiAgentExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 父任务ID (多Agent任务的唯一标识)
     */
    @Column(name = "parent_task_id", nullable = false, unique = true, length = 100)
    private String parentTaskId;

    /**
     * 执行计划 (JSON格式)
     * 存储完整的ExecutionPlan序列化结果
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "execution_plan_json", columnDefinition = "TEXT")
    private Map<String, Object> executionPlanJson;

    /**
     * 执行模式: SEQUENTIAL, PARALLEL, HIERARCHICAL
     */
    @Column(name = "execution_mode", length = 50)
    private String executionMode;

    /**
     * 总执行时长(毫秒)
     */
    @Column(name = "total_latency_ms")
    private Integer totalLatencyMs;

    /**
     * 成功的子任务数
     */
    @Column(name = "success_count")
    private Integer successCount;

    /**
     * 失败的子任务数
     */
    @Column(name = "failure_count")
    private Integer failureCount;

    /**
     * 执行状态: ok, error, partial
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 用户ID
     */
    @Column(name = "user_id", length = 100)
    private String userId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
