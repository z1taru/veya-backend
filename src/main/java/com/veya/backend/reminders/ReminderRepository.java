package com.veya.backend.reminders;

import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    @Query("""
            SELECT r FROM Reminder r
            WHERE r.family.id = :familyId
              AND r.active = true
              AND (:assigneeId IS NULL OR r.assignee.id = :assigneeId)
              AND (:repeatType IS NULL OR r.repeatType = :repeatType)
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.reminderDate ASC NULLS LAST, r.reminderTime ASC NULLS LAST, r.createdAt DESC
            """)
    List<Reminder> findByFamilyWithFilters(
            UUID familyId,
            UUID assigneeId,
            RepeatType repeatType,
            TaskStatus status);
}
