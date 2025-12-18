package com.shengong.agentruntime.repository;

import com.shengong.agentruntime.entity.MultiAgentExecutionEntity;
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
 * 多Agent协同执行记录Repository
 *
 * @author 神工团队
 * @since 1.0.0
 */
@Repository
public interface MultiAgentExecutionRepository extends JpaRepository<MultiAgentExecutionEntity, Long> {

    /**
     * 根据父任务ID查询
     *
     * @param parentTaskId 父任务ID
     * @return 执行记录
     */
    Optional<MultiAgentExecutionEntity> findByParentTaskId(String parentTaskId);

    /**
     * 根据用户ID分页查询
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 执行记录分页结果
     */
    Page<MultiAgentExecutionEntity> findByUserId(String userId, Pageable pageable);

    /**
     * 根据状态查询
     *
     * @param status 状态
     * @return 执行记录列表
     */
    List<MultiAgentExecutionEntity> findByStatus(String status);

    /**
     * 根据用户ID和状态查询
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 执行记录列表
     */
    List<MultiAgentExecutionEntity> findByUserIdAndStatus(String userId, String status);

    /**
     * 查询指定时间范围内的记录
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 执行记录列表
     */
    @Query("SELECT m FROM MultiAgentExecutionEntity m WHERE m.createdAt >= :start AND m.createdAt <= :end ORDER BY m.createdAt DESC")
    List<MultiAgentExecutionEntity> findByCreatedAtBetween(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    /**
     * 统计用户的多Agent任务数量
     *
     * @param userId 用户ID
     * @return 任务数量
     */
    long countByUserId(String userId);

    /**
     * 统计指定状态的任务数量
     *
     * @param status 状态
     * @return 任务数量
     */
    long countByStatus(String status);

    /**
     * 删除指定时间之前的记录
     *
     * @param before 时间界限
     * @return 删除的记录数
     */
    @Query("DELETE FROM MultiAgentExecutionEntity m WHERE m.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
