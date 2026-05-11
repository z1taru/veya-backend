package com.veya.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veya.backend.ai.dto.AiCreateResponse;
import com.veya.backend.ai.dto.AiParseRequest;
import com.veya.backend.ai.dto.AiParseResponse;
import com.veya.backend.common.enums.AiCommandStatus;
import com.veya.backend.common.enums.AiParsedType;
import com.veya.backend.common.enums.RepeatType;
import com.veya.backend.common.enums.TaskPriority;
import com.veya.backend.common.exception.BadRequestException;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.Family;
import com.veya.backend.families.FamilyMember;
import com.veya.backend.families.FamilyMemberRepository;
import com.veya.backend.families.FamilyService;
import com.veya.backend.reminders.ReminderService;
import com.veya.backend.reminders.dto.CreateReminderRequest;
import com.veya.backend.shopping.ShoppingService;
import com.veya.backend.shopping.dto.CreateShoppingItemRequest;
import com.veya.backend.shopping.dto.ShoppingItemDto;
import com.veya.backend.tasks.TaskService;
import com.veya.backend.tasks.dto.CreateTaskRequest;
import com.veya.backend.tasks.dto.TaskDto;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiCommandService {

    private static final List<String> SHOPPING_ITEMS = List.of("молоко", "хлеб", "яйца", "рис", "фрукты", "лекарства");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");

    private final AiCommandRepository aiCommandRepo;
    private final FamilyService familyService;
    private final FamilyMemberRepository familyMemberRepo;
    private final UserRepository userRepo;
    private final ShoppingService shoppingService;
    private final TaskService taskService;
    private final ReminderService reminderService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiParseResponse parse(AiParseRequest request, UUID userId) {
        UUID familyId = familyService.getActiveFamilyId(userId);
        Family family = familyService.getFamilyEntity(familyId);
        User user = getUser(userId);

        AiParsedType parsedType = detectType(request.text());
        Map<String, Object> payload = buildPayload(request.text(), parsedType, familyId);
        String payloadJson = writeJson(payload);

        AiCommand command = AiCommand.builder()
                .family(family)
                .user(user)
                .rawText(request.text())
                .parsedType(parsedType)
                .parsedPayload(payloadJson)
                .status(AiCommandStatus.PREVIEW)
                .build();

        AiCommand saved = aiCommandRepo.save(command);
        return new AiParseResponse(saved.getId(), saved.getParsedType(), saved.getParsedPayload(), saved.getStatus());
    }

    @Transactional
    public AiCreateResponse create(UUID commandId, UUID userId) {
        AiCommand command = aiCommandRepo.findByIdAndUserId(commandId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("AI command", commandId));
        familyService.assertMember(command.getFamily().getId(), userId);

        if (command.getStatus() == AiCommandStatus.CREATED) {
            throw new BadRequestException("AI command is already created");
        }

        JsonNode payload = readJson(command.getParsedPayload());
        List<UUID> createdIds = switch (command.getParsedType()) {
            case SHOPPING -> createShopping(payload, userId);
            case TASK -> createTask(payload, userId);
            case REMINDER -> createReminder(payload, userId);
            case UNKNOWN -> throw new BadRequestException("AI command parsed type is UNKNOWN");
        };

        command.setStatus(AiCommandStatus.CREATED);
        aiCommandRepo.save(command);
        return new AiCreateResponse(command.getId(), command.getParsedType(), createdIds, command.getStatus());
    }

    private AiParsedType detectType(String text) {
        String normalized = normalize(text);
        if (normalized.contains("добавь")) {
            return AiParsedType.SHOPPING;
        }
        if (normalized.contains("напомни") && hasRepeatWords(normalized)) {
            return AiParsedType.REMINDER;
        }
        if (normalized.contains("напомни")) {
            return AiParsedType.TASK;
        }
        return AiParsedType.UNKNOWN;
    }

    private Map<String, Object> buildPayload(String text, AiParsedType parsedType, UUID familyId) {
        String normalized = normalize(text);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rawText", text);

        UUID assigneeId = detectAssigneeId(normalized, familyId);
        if (assigneeId != null) {
            payload.put("assigneeId", assigneeId.toString());
        }

        LocalTime time = detectTime(normalized);
        if (time != null) {
            payload.put("time", time.toString());
        }

        if (parsedType == AiParsedType.SHOPPING) {
            List<String> items = SHOPPING_ITEMS.stream()
                    .filter(normalized::contains)
                    .toList();
            payload.put("items", items.isEmpty() ? List.of(cleanTitle(text)) : items);
        } else {
            payload.put("title", cleanTitle(text));
            payload.put("repeatType", detectRepeatType(normalized).name());
        }

        return payload;
    }

    private List<UUID> createShopping(JsonNode payload, UUID userId) {
        List<UUID> ids = new ArrayList<>();
        JsonNode items = payload.path("items");
        if (!items.isArray() || items.isEmpty()) {
            throw new BadRequestException("No shopping items found in AI payload");
        }
        for (JsonNode item : items) {
            ShoppingItemDto created = shoppingService.createItem(
                    new CreateShoppingItemRequest(item.asText(), null, null),
                    userId);
            ids.add(created.id());
        }
        return ids;
    }

    private List<UUID> createTask(JsonNode payload, UUID userId) {
        TaskDto task = taskService.createTask(
                new CreateTaskRequest(
                        payload.path("title").asText("Reminder"),
                        null,
                        optionalUuid(payload, "assigneeId"),
                        null,
                        TaskPriority.MEDIUM,
                        null,
                        optionalTime(payload, "time"),
                        RepeatType.NONE),
                userId);
        return List.of(task.id());
    }

    private List<UUID> createReminder(JsonNode payload, UUID userId) {
        var reminder = reminderService.createReminder(
                new CreateReminderRequest(
                        payload.path("title").asText("Reminder"),
                        null,
                        optionalUuid(payload, "assigneeId"),
                        null,
                        optionalTime(payload, "time"),
                        parseRepeatType(payload.path("repeatType").asText(null))),
                userId);
        return List.of(reminder.id());
    }

    private UUID detectAssigneeId(String normalized, UUID familyId) {
        return familyMemberRepo.findByFamilyIdAndStatus(familyId, com.veya.backend.common.enums.FamilyMemberStatus.ACTIVE)
                .stream()
                .filter(member -> nameMatches(normalized, member))
                .map(member -> member.getUser().getId())
                .findFirst()
                .orElse(null);
    }

    private boolean nameMatches(String normalized, FamilyMember member) {
        String fullName = normalize(member.getUser().getFullName());
        if (fullName.isBlank() && member.getUser().getEmail() == null) {
            return false;
        }
        for (String part : fullName.split("\\s+")) {
            if (part.length() > 2 && normalized.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private LocalTime detectTime(String normalized) {
        Matcher matcher = TIME_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        return LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    private boolean hasRepeatWords(String normalized) {
        return normalized.contains("каждый день")
                || normalized.contains("каждую пятницу")
                || normalized.contains("каждый месяц")
                || normalized.contains("каждую неделю");
    }

    private RepeatType detectRepeatType(String normalized) {
        if (normalized.contains("каждый день")) {
            return RepeatType.DAILY;
        }
        if (normalized.contains("каждую пятницу") || normalized.contains("каждую неделю")) {
            return RepeatType.WEEKLY;
        }
        if (normalized.contains("каждый месяц")) {
            return RepeatType.MONTHLY;
        }
        return RepeatType.NONE;
    }

    private RepeatType parseRepeatType(String value) {
        if (value == null || value.isBlank()) {
            return RepeatType.NONE;
        }
        return RepeatType.valueOf(value);
    }

    private UUID optionalUuid(JsonNode payload, String field) {
        return payload.hasNonNull(field) ? UUID.fromString(payload.get(field).asText()) : null;
    }

    private LocalTime optionalTime(JsonNode payload, String field) {
        return payload.hasNonNull(field) ? LocalTime.parse(payload.get(field).asText()) : null;
    }

    private String cleanTitle(String text) {
        return text.replaceFirst("(?iu)^\\s*(добавь|напомни)\\s*", "").trim();
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Could not build AI payload");
        }
    }

    private JsonNode readJson(String payload) {
        try {
            return objectMapper.readTree(payload == null ? "{}" : payload);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid AI payload");
        }
    }

    private User getUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
