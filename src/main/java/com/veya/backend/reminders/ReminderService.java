package com.veya.backend.reminders;

import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskStatus;
import com.veya.backend.common.exception.BadRequestException;
import com.veya.backend.common.exception.ForbiddenException;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.Family;
import com.veya.backend.families.FamilyService;
import com.veya.backend.reminders.dto.CreateReminderRequest;
import com.veya.backend.reminders.dto.ReminderDto;
import com.veya.backend.reminders.dto.UpdateReminderRequest;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepo;
    private final FamilyService familyService;
    private final UserRepository userRepo;

    public List<ReminderDto> getReminders(UUID requesterId, UUID assigneeId, RepeatType repeatType, TaskStatus status) {
        UUID familyId = familyService.getActiveFamilyId(requesterId);
        if (assigneeId != null) {
            familyService.assertMember(familyId, assigneeId);
        }
        return reminderRepo.findByFamilyWithFilters(familyId, assigneeId, repeatType, status)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ReminderDto getReminder(UUID reminderId, UUID requesterId) {
        Reminder reminder = findReminder(reminderId);
        familyService.assertMember(reminder.getFamily().getId(), requesterId);
        return toDto(reminder);
    }

    @Transactional
    public ReminderDto createReminder(CreateReminderRequest request, UUID requesterId) {
        UUID familyId = familyService.getActiveFamilyId(requesterId);
        Family family = familyService.getFamilyEntity(familyId);
        User creator = getUser(requesterId);

        User assignee = null;
        if (request.assigneeId() != null) {
            familyService.assertMember(familyId, request.assigneeId());
            assignee = getUser(request.assigneeId());
        }

        Reminder reminder = Reminder.builder()
                .family(family)
                .title(request.title())
                .description(request.description())
                .creator(creator)
                .assignee(assignee)
                .reminderDate(request.reminderDate())
                .reminderTime(request.reminderTime())
                .repeatType(request.repeatType() != null ? request.repeatType() : RepeatType.NONE)
                .status(TaskStatus.PENDING)
                .build();

        return toDto(reminderRepo.save(reminder));
    }

    @Transactional
    public ReminderDto updateReminder(UUID reminderId, UpdateReminderRequest request, UUID requesterId) {
        Reminder reminder = findReminder(reminderId);
        familyService.assertMember(reminder.getFamily().getId(), requesterId);
        assertCanManage(reminder, requesterId);

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Reminder title cannot be blank");
            }
            reminder.setTitle(request.title());
        }
        if (request.description() != null) {
            reminder.setDescription(request.description());
        }
        if (request.assigneeId() != null) {
            familyService.assertMember(reminder.getFamily().getId(), request.assigneeId());
            reminder.setAssignee(getUser(request.assigneeId()));
        }
        if (request.reminderDate() != null) {
            reminder.setReminderDate(request.reminderDate());
        }
        if (request.reminderTime() != null) {
            reminder.setReminderTime(request.reminderTime());
        }
        if (request.repeatType() != null) {
            reminder.setRepeatType(request.repeatType());
        }
        if (request.status() != null) {
            reminder.setStatus(request.status());
        }

        return toDto(reminderRepo.save(reminder));
    }

    @Transactional
    public void deleteReminder(UUID reminderId, UUID requesterId) {
        Reminder reminder = findReminder(reminderId);
        familyService.assertMember(reminder.getFamily().getId(), requesterId);
        assertCanManage(reminder, requesterId);
        reminder.setActive(false);
        reminderRepo.save(reminder);
    }

    private Reminder findReminder(UUID reminderId) {
        return reminderRepo.findById(reminderId)
                .filter(Reminder::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", reminderId));
    }

    private User getUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void assertCanManage(Reminder reminder, UUID requesterId) {
        boolean isCreator = reminder.getCreator().getId().equals(requesterId);
        boolean isOwnerOrParent = familyService.isOwnerOrParent(reminder.getFamily().getId(), requesterId);
        if (!isCreator && !isOwnerOrParent) {
            throw new ForbiddenException("Only the reminder creator, family owner, or parent can manage this reminder");
        }
    }

    private ReminderDto toDto(Reminder reminder) {
        return new ReminderDto(
                reminder.getId(),
                reminder.getFamily().getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getCreator().getId(),
                reminder.getAssignee() != null ? reminder.getAssignee().getId() : null,
                reminder.getReminderDate(),
                reminder.getReminderTime(),
                reminder.getRepeatType(),
                reminder.getStatus(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt());
    }
}
