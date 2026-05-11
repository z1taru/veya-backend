package com.veya.backend.tasks;

import com.veya.backend.common.enums.TaskPriority;
import com.veya.backend.common.enums.TaskStatus;
import com.veya.backend.common.exception.ForbiddenException;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.Family;
import com.veya.backend.families.FamilyMember;
import com.veya.backend.families.FamilyMemberRepository;
import com.veya.backend.families.FamilyRepository;
import com.veya.backend.tasks.dto.*;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepo;
    private final TaskStatusHistoryRepository historyRepo;
    private final FamilyMemberRepository memberRepo;
    private final FamilyRepository familyRepo;
    private final UserRepository userRepo;

    // TODO: inject NotificationService here when it's created
    // private final NotificationService notificationService;

    // ── List ──────────────────────────────────────────────────

    public List<TaskDto> getTasks(UUID requesterId,
            TaskStatus status,
            UUID assigneeId,
            UUID creatorId,
            LocalDate dueDate,
            TaskPriority priority) {
        UUID familyId = getActiveFamilyId(requesterId);
        return taskRepo.findByFamilyWithFilters(familyId, status, assigneeId, creatorId, dueDate, priority)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ── Get one ───────────────────────────────────────────────

    public TaskDto getTask(UUID taskId, UUID requesterId) {
        Task task = findTaskById(taskId);
        assertFamilyMember(task.getFamily().getId(), requesterId);
        return toDto(task);
    }

    // ── Create ────────────────────────────────────────────────

    @Transactional
    public TaskDto createTask(CreateTaskRequest req, UUID requesterId) {
        // Determine family: use familyId from request or fall back to requester's
        // active family
        UUID familyId = (req.familyId() != null) ? req.familyId() : getActiveFamilyId(requesterId);
        assertFamilyMember(familyId, requesterId);

        Family family = familyRepo.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));
        User creator = getUser(requesterId);

        // Validate assignee belongs to family
        User assignee = null;
        if (req.assigneeId() != null) {
            assertFamilyMember(familyId, req.assigneeId());
            assignee = getUser(req.assigneeId());
        }

        Task task = Task.builder()
                .family(family)
                .title(req.title())
                .description(req.description())
                .creator(creator)
                .assignee(assignee)
                .status(TaskStatus.PENDING)
                .priority(req.priority() != null ? req.priority() : com.veya.backend.common.enums.TaskPriority.MEDIUM)
                .dueDate(req.dueDate())
                .dueTime(req.dueTime())
                .repeatType(req.repeatType() != null ? req.repeatType() : com.veya.backend.common.enums.RepeatType.NONE)
                .build();

        taskRepo.save(task);

        // Record initial history entry
        saveHistory(task, null, TaskStatus.PENDING, creator, "Task created");

        // TODO: send notification to assignee when NotificationService is available
        // if (assignee != null) {
        // notificationService.notifyTaskAssigned(task, assignee);
        // }

        return toDto(task);
    }

    // ── Update ────────────────────────────────────────────────

    @Transactional
    public TaskDto updateTask(UUID taskId, UpdateTaskRequest req, UUID requesterId) {
        Task task = findTaskById(taskId);
        assertFamilyMember(task.getFamily().getId(), requesterId);

        if (req.title() != null && !req.title().isBlank()) {
            task.setTitle(req.title());
        }
        if (req.description() != null) {
            task.setDescription(req.description());
        }
        if (req.priority() != null) {
            task.setPriority(req.priority());
        }
        if (req.dueDate() != null) {
            task.setDueDate(req.dueDate());
        }
        if (req.dueTime() != null) {
            task.setDueTime(req.dueTime());
        }
        if (req.repeatType() != null) {
            task.setRepeatType(req.repeatType());
        }

        // Reassign — validate new assignee is family member
        if (req.assigneeId() != null) {
            assertFamilyMember(task.getFamily().getId(), req.assigneeId());
            task.setAssignee(getUser(req.assigneeId()));
        }

        return toDto(taskRepo.save(task));
    }

    // ── Update status ─────────────────────────────────────────

    @Transactional
    public TaskDto updateTaskStatus(UUID taskId, UpdateTaskStatusRequest req, UUID requesterId) {
        Task task = findTaskById(taskId);
        assertFamilyMember(task.getFamily().getId(), requesterId);

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(req.status());
        taskRepo.save(task);

        User changer = getUser(requesterId);
        saveHistory(task, oldStatus, req.status(), changer, req.comment());

        // TODO: notify relevant users when NotificationService is available
        // notificationService.notifyTaskStatusChanged(task, oldStatus, req.status(),
        // changer);

        return toDto(task);
    }

    // ── Delete ────────────────────────────────────────────────

    @Transactional
    public void deleteTask(UUID taskId, UUID requesterId) {
        Task task = findTaskById(taskId);
        assertFamilyMember(task.getFamily().getId(), requesterId);
        // Only creator or family owner can delete
        boolean isCreator = task.getCreator().getId().equals(requesterId);
        boolean isOwner = task.getFamily().getOwner() != null
                && task.getFamily().getOwner().getId().equals(requesterId);
        if (!isCreator && !isOwner) {
            throw new ForbiddenException("Only the task creator or family owner can delete this task");
        }
        taskRepo.delete(task);
    }

    // ── History ───────────────────────────────────────────────

    public List<TaskStatusHistoryDto> getHistory(UUID taskId, UUID requesterId) {
        Task task = findTaskById(taskId);
        assertFamilyMember(task.getFamily().getId(), requesterId);
        return historyRepo.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────

    private Task findTaskById(UUID taskId) {
        return taskRepo.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }

    private User getUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Returns the familyId of the user's active membership.
     * Throws if the user has no active membership.
     */
    private UUID getActiveFamilyId(UUID userId) {
        return memberRepo.findActiveByUserId(userId)
                .map(fm -> fm.getFamily().getId())
                .orElseThrow(() -> new ForbiddenException("User has no active family membership"));
    }

    /**
     * Throws ForbiddenException if the user is not an active member of the family.
     */
    private void assertFamilyMember(UUID familyId, UUID userId) {
        boolean isMember = memberRepo.findByFamilyIdAndUserId(familyId, userId)
                .map(fm -> fm.getStatus() == com.veya.backend.common.enums.MemberStatus.ACTIVE)
                .orElse(false);
        if (!isMember) {
            throw new ForbiddenException("You are not a member of this family");
        }
    }

    private void saveHistory(Task task, TaskStatus oldStatus, TaskStatus newStatus,
            User changedBy, String comment) {
        TaskStatusHistory history = TaskStatusHistory.builder()
                .task(task)
                .changedBy(changedBy)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .comment(comment)
                .build();
        historyRepo.save(history);
    }

    private TaskDto toDto(Task t) {
        return new TaskDto(
                t.getId(),
                t.getFamily().getId(),
                t.getTitle(),
                t.getDescription(),
                t.getCreator().getId(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getStatus(),
                t.getPriority(),
                t.getDueDate(),
                t.getDueTime(),
                t.getRepeatType(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    private TaskStatusHistoryDto toHistoryDto(TaskStatusHistory h) {
        return new TaskStatusHistoryDto(
                h.getId(),
                h.getTask().getId(),
                h.getChangedBy().getId(),
                h.getOldStatus(),
                h.getNewStatus(),
                h.getComment(),
                h.getCreatedAt());
    }
}