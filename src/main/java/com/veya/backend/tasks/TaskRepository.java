package com.veya.backend.tasks;

import com.veya.backend.common.enums.TaskPriority;
import com.veya.backend.common.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("""
            SELECT t FROM Task t
            WHERE t.family.id = :familyId
              AND (:status IS NULL OR t.status = :status)
              AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
              AND (:creatorId IS NULL OR t.creator.id = :creatorId)
              AND (:dueDate IS NULL OR t.dueDate = :dueDate)
              AND (:priority IS NULL OR t.priority = :priority)
            ORDER BY t.createdAt DESC
            """)
    List<Task> findByFamilyWithFilters(
            UUID familyId,
            TaskStatus status,
            UUID assigneeId,
            UUID creatorId,
            LocalDate dueDate,
            TaskPriority priority);
}