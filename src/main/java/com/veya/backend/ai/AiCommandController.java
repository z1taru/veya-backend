package com.veya.backend.ai;

import com.veya.backend.ai.dto.AiCreateResponse;
import com.veya.backend.ai.dto.AiParseRequest;
import com.veya.backend.ai.dto.AiParseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "AI")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiCommandController {

    private final AiCommandService aiCommandService;

    @PostMapping("/parse")
    public ResponseEntity<AiParseResponse> parse(
            @Valid @RequestBody AiParseRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(aiCommandService.parse(request, uid(principal)));
    }

    @PostMapping("/commands/{id}/create")
    public ResponseEntity<AiCreateResponse> create(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(aiCommandService.create(id, uid(principal)));
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
