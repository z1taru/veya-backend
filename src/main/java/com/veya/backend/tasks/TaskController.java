package com.veya.backend.tasks;

import com.veya.backend.common.enums.TaskPriority;
import com.veya.backend.common.enums.TaskStatus;
import com.veya.backend.tasks.dto.CreateTaskRequest;
import com.veya.backend.tasks.dto.TaskDto;
import com.veya.backend.tasks.dto.TaskStatusHistoryDto;
import com.veya.backend.tasks.dto.UpdateTaskRequest;
import com.veya.backend.tasks.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Tasks")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskDto>> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) TaskPriority priority,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.getTasks(uid(principal), status, assigneeId, creatorId, dueDate, priority));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTask(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.getTask(id, uid(principal)));
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, uid(principal)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.updateTask(id, request, uid(principal)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskDto> updateTaskStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request, uid(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        taskService.deleteTask(id, uid(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskStatusHistoryDto>> getHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.getHistory(id, uid(principal)));
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
