package com.shengong.agentruntime.repository;

import com.shengong.agentruntime.entity.TaskExecutionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 任务执行记录 Repository
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, Long> {

    /**
     * 根据 taskId 查询
     */
    Optional<TaskExecutionEntity> findByTaskId(String taskId);

    /**
     * 根据 traceId 查询所有记录
     */
    List<TaskExecutionEntity> findByTraceId(String traceId);

    /**
     * 根据 userId 查询
     */
    Page<TaskExecutionEntity> findByUserId(String userId, Pageable pageable);

    /**
     * 根据状态查询
     */
    List<TaskExecutionEntity> findByStatus(String status);

    /**
     * 根据任务类型查询
     */
    Page<TaskExecutionEntity> findByTaskType(String taskType, Pageable pageable);

    /**
     * 根据业务域查询
     */
    Page<TaskExecutionEntity> findByDomain(String domain, Pageable pageable);

    /**
     * 根据时间范围查询
     */
    @Query("SELECT t FROM TaskExecutionEntity t WHERE t.startedAt >= :startTime AND t.startedAt <= :endTime")
    List<TaskExecutionEntity> findByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据用户和时间范围查询
     */
    @Query("SELECT t FROM TaskExecutionEntity t WHERE t.userId = :userId " +
            "AND t.startedAt >= :startTime AND t.startedAt <= :endTime")
    List<TaskExecutionEntity> findByUserIdAndTimeRange(
            @Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计某个时间段内的执行次数
     */
    @Query("SELECT COUNT(t) FROM TaskExecutionEntity t WHERE t.startedAt >= :startTime AND t.startedAt <= :endTime")
    long countByTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计失败任务数量
     */
    long countByStatus(String status);

    /**
     * 查询最近 N 条记录
     */
    List<TaskExecutionEntity> findTop10ByOrderByStartedAtDesc();

    /**
     * 根据 Agent 名称统计执行次数
     */
    @Query("SELECT t.agentName, COUNT(t) FROM TaskExecutionEntity t GROUP BY t.agentName")
    List<Object[]> countByAgentName();
}
