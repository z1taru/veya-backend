package com.veya.backend.reminders;

import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskStatus;
import com.veya.backend.reminders.dto.CreateReminderRequest;
import com.veya.backend.reminders.dto.ReminderDto;
import com.veya.backend.reminders.dto.UpdateReminderRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Reminders")
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    @GetMapping
    public ResponseEntity<List<ReminderDto>> getReminders(
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) RepeatType repeatType,
            @RequestParam(required = false) TaskStatus status,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.getReminders(uid(principal), assigneeId, repeatType, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReminderDto> getReminder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.getReminder(id, uid(principal)));
    }

    @PostMapping
    public ResponseEntity<ReminderDto> createReminder(
            @Valid @RequestBody CreateReminderRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reminderService.createReminder(request, uid(principal)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReminderDto> updateReminder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReminderRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.updateReminder(id, request, uid(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        reminderService.deleteReminder(id, uid(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
