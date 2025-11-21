package com.shengong.agentruntime.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务执行记录实体
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Data
@Entity
@Table(name = "task_execution", indexes = {
        @Index(name = "idx_trace_id", columnList = "traceId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_started_at", columnList = "startedAt"),
        @Index(name = "idx_task_type", columnList = "taskType"),
        @Index(name = "idx_domain", columnList = "domain")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, length = 100)
    private String taskId;

    @Column(name = "task_type", nullable = false, length = 100)
    private String taskType;

    @Column(name = "domain", nullable = false, length = 50)
    private String domain;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "trace_id", nullable = false, length = 100)
    private String traceId;

    /**
     * 执行状态: ok, error, partial
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /**
     * 任务输入 payload (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSON")
    private Map<String, Object> payload;

    /**
     * 执行结果 (JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "JSON")
    private Map<String, Object> result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (startedAt == null) {
            startedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
