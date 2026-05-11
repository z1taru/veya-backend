package com.veya.backend.notifications;

import com.veya.backend.common.enums.NotificationType;
import com.veya.backend.common.enums.TaskStatus;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.Family;
import com.veya.backend.families.FamilyRepository;
import com.veya.backend.notifications.dto.NotificationDto;
import com.veya.backend.tasks.Task;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final FamilyRepository familyRepo;

    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public NotificationDto markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepo.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        notification.setRead(true);
        return toDto(notificationRepo.save(notification));
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepo.markAllReadByUserId(userId);
    }

    @Transactional
    public NotificationDto createNotification(
            UUID userId,
            UUID familyId,
            NotificationType type,
            String title,
            String body) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Family family = familyId == null ? null : familyRepo.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        Notification notification = Notification.builder()
                .user(user)
                .family(family)
                .type(type)
                .title(title)
                .body(body)
                .build();

        return toDto(notificationRepo.save(notification));
    }

    public void notifyTaskAssigned(Task task) {
        if (task.getAssignee() == null) {
            return;
        }
        createNotification(
                task.getAssignee().getId(),
                task.getFamily().getId(),
                NotificationType.TASK_ASSIGNED,
                "Task assigned",
                task.getTitle());
    }

    public void notifyTaskStatusChanged(Task task, TaskStatus oldStatus, TaskStatus newStatus, UUID changedById) {
        UUID creatorId = task.getCreator().getId();
        if (creatorId.equals(changedById)) {
            return;
        }
        createNotification(
                creatorId,
                task.getFamily().getId(),
                NotificationType.TASK_STATUS_CHANGED,
                "Task status changed",
                task.getTitle() + ": " + oldStatus + " -> " + newStatus);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getUser().getId(),
                notification.getFamily() != null ? notification.getFamily().getId() : null,
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
