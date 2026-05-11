package com.veya.backend.families;

import com.veya.backend.families.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Families")
@RestController
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    // ── Family ────────────────────────────────────────────────

    @Operation(summary = "Get current user's family")
    @GetMapping("/api/families/current")
    public ResponseEntity<FamilyDto> getCurrent(@AuthenticationPrincipal UserDetails p) {
        return ResponseEntity.ok(familyService.getCurrentFamily(uid(p)));
    }

    @Operation(summary = "Update family name")
    @PatchMapping("/api/families/{familyId}")
    public ResponseEntity<FamilyDto> updateFamily(
            @PathVariable UUID familyId,
            @Valid @RequestBody UpdateFamilyRequest req,
            @AuthenticationPrincipal UserDetails p) {
        return ResponseEntity.ok(familyService.updateFamily(familyId, req.name(), uid(p)));
    }

    @Operation(summary = "List active family members")
    @GetMapping("/api/families/{familyId}/members")
    public ResponseEntity<List<FamilyMemberDto>> getMembers(
            @PathVariable UUID familyId,
            @AuthenticationPrincipal UserDetails p) {
        return ResponseEntity.ok(familyService.getMembers(familyId, uid(p)));
    }

    // ── Member management ─────────────────────────────────────

    @Operation(summary = "Invite user by email")
    @PostMapping("/api/families/{familyId}/members/invite")
    public ResponseEntity<InviteDto> invite(
            @PathVariable UUID familyId,
            @Valid @RequestBody InviteRequest req,
            @AuthenticationPrincipal UserDetails p) {
        FamilyInvite invite = familyService.invite(familyId, req.email(), req.role(), uid(p));
        return ResponseEntity.status(HttpStatus.CREATED).body(toInviteDto(invite));
    }

    @Operation(summary = "Update member role")
    @PatchMapping("/api/families/{familyId}/members/{memberId}/role")
    public ResponseEntity<FamilyMemberDto> updateRole(
            @PathVariable UUID familyId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest req,
            @AuthenticationPrincipal UserDetails p) {
        return ResponseEntity.ok(familyService.updateMemberRole(familyId, memberId, req.role(), uid(p)));
    }

    @Operation(summary = "Remove member")
    @DeleteMapping("/api/families/{familyId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID familyId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserDetails p) {
        familyService.removeMember(familyId, memberId, uid(p));
        return ResponseEntity.noContent().build();
    }

    // ── Invites ───────────────────────────────────────────────

    @Operation(summary = "Get invite by token")
    @GetMapping("/api/invites/{token}")
    public ResponseEntity<InviteDto> getInvite(@PathVariable String token) {
        return ResponseEntity.ok(toInviteDto(familyService.getInviteByToken(token)));
    }

    @Operation(summary = "Accept invite")
    @PostMapping("/api/invites/{token}/accept")
    public ResponseEntity<FamilyMemberDto> acceptInvite(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails p) {
        FamilyMember member = familyService.acceptInvite(token, uid(p));
        return ResponseEntity.ok(toMemberDto(member));
    }

    @Operation(summary = "Cancel invite")
    @PostMapping("/api/invites/{token}/cancel")
    public ResponseEntity<Void> cancelInvite(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetails p) {
        familyService.cancelInvite(token, uid(p));
        return ResponseEntity.noContent().build();
    }

    // ── helpers ───────────────────────────────────────────────

    private UUID uid(UserDetails p) {
        return UUID.fromString(p.getUsername());
    }

    private InviteDto toInviteDto(FamilyInvite i) {
        return new InviteDto(i.getId(), i.getFamily().getId(), i.getFamily().getName(),
                i.getEmail(), i.getRole(), i.getStatus(), i.getExpiresAt(), i.getCreatedAt());
    }

    private FamilyMemberDto toMemberDto(FamilyMember m) {
        return new FamilyMemberDto(m.getId(), m.getUser().getId(),
                m.getUser().getFullName(), m.getUser().getEmail(),
                m.getUser().getAvatarUrl(), m.getRole(), m.getStatus(), m.getJoinedAt());
    }
}