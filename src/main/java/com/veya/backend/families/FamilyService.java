package com.veya.backend.families;

import com.veya.backend.common.enums.InviteStatus;
import com.veya.backend.common.enums.FamilyMemberStatus;
import com.veya.backend.common.enums.FamilyRole;
import com.veya.backend.common.exception.ConflictException;
import com.veya.backend.common.exception.ForbiddenException;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.dto.FamilyDto;
import com.veya.backend.families.dto.FamilyMemberDto;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepo;
    private final FamilyMemberRepository memberRepo;
    private final FamilyInviteRepository inviteRepo;
    private final UserRepository userRepo;

    // ── Current family ────────────────────────────────────────

    public FamilyDto getCurrentFamily(UUID userId) {
        FamilyMember membership = getActiveMembership(userId);
        return toDto(membership.getFamily());
    }

    @Transactional
    public FamilyDto updateFamily(UUID familyId, String newName, UUID requesterId) {
        Family family = getFamily(familyId);
        assertOwner(family, requesterId);
        family.setName(newName);
        return toDto(familyRepo.save(family));
    }

    public List<FamilyMemberDto> getMembers(UUID familyId, UUID requesterId) {
        assertMember(familyId, requesterId);
        return memberRepo.findByFamilyIdAndStatus(familyId, FamilyMemberStatus.ACTIVE)
                .stream().map(this::toMemberDto).toList();
    }

    // ── Invites ───────────────────────────────────────────────

    @Transactional
    public FamilyInvite invite(UUID familyId, String email, FamilyRole role, UUID inviterId) {
        assertOwnerOrParent(familyId, inviterId);

        // Check if user already a member
        userRepo.findByEmail(email).ifPresent(u -> {
            if (memberRepo.existsByFamilyIdAndUserId(familyId, u.getId())) {
                throw new ConflictException("User is already a family member");
            }
        });

        FamilyInvite invite = FamilyInvite.builder()
                .family(getFamily(familyId))
                .email(email)
                .invitedBy(getUser(inviterId))
                .role(role)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600)) // 7 days
                .build();

        return inviteRepo.save(invite);
    }

    @Transactional
    public FamilyMember acceptInvite(String token, UUID userId) {
        FamilyInvite invite = inviteRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (invite.getStatus() != InviteStatus.PENDING || invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Invite is no longer valid");
        }

        invite.setStatus(InviteStatus.ACCEPTED);
        inviteRepo.save(invite);

        User user = getUser(userId);
        if (memberRepo.existsByFamilyIdAndUserId(invite.getFamily().getId(), userId)) {
            throw new ConflictException("Already a family member");
        }

        FamilyMember member = FamilyMember.builder()
                .family(invite.getFamily())
                .user(user)
                .role(invite.getRole())
                .status(FamilyMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        return memberRepo.save(member);
    }

    @Transactional
    public void cancelInvite(String token, UUID requesterId) {
        FamilyInvite invite = inviteRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        assertMember(invite.getFamily().getId(), requesterId);
        invite.setStatus(InviteStatus.CANCELLED);
        inviteRepo.save(invite);
    }

    public FamilyInvite getInviteByToken(String token) {
        return inviteRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
    }

    // ── Member management ─────────────────────────────────────

    @Transactional
    public FamilyMemberDto updateMemberRole(UUID familyId, UUID memberId, FamilyRole role, UUID requesterId) {
        Family family = getFamily(familyId);
        assertOwner(family, requesterId);

        FamilyMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ForbiddenException("Member does not belong to this family");
        }
        if (member.getRole() == FamilyRole.OWNER) {
            throw new ForbiddenException("Cannot change owner role");
        }
        if (role == FamilyRole.OWNER) {
            throw new ForbiddenException("Cannot assign owner role");
        }

        member.setRole(role);
        return toMemberDto(memberRepo.save(member));
    }

    @Transactional
    public void removeMember(UUID familyId, UUID memberId, UUID requesterId) {
        Family family = getFamily(familyId);
        assertOwner(family, requesterId);

        FamilyMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new ForbiddenException("Member does not belong to this family");
        }
        if (member.getRole() == FamilyRole.OWNER) {
            throw new ForbiddenException("Cannot remove owner");
        }

        member.setStatus(FamilyMemberStatus.REMOVED);
        memberRepo.save(member);
    }

    // ── Helpers ───────────────────────────────────────────────

    public FamilyMember getActiveMembership(UUID userId) {
        return memberRepo.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active family membership for user"));
    }

    /** Throws if user is not an active member of the family */
    public FamilyMember assertMember(UUID familyId, UUID userId) {
        return memberRepo.findByFamilyIdAndUserId(familyId, userId)
                .filter(m -> m.getStatus() == FamilyMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this family"));
    }

    public FamilyMember assertOwnerOrParent(UUID familyId, UUID userId) {
        FamilyMember membership = assertMember(familyId, userId);
        if (membership.getRole() != FamilyRole.OWNER && membership.getRole() != FamilyRole.PARENT) {
            throw new ForbiddenException("Only owner or parent can perform this action");
        }
        return membership;
    }

    public boolean isOwnerOrParent(UUID familyId, UUID userId) {
        return memberRepo.findByFamilyIdAndUserId(familyId, userId)
                .filter(m -> m.getStatus() == FamilyMemberStatus.ACTIVE)
                .map(m -> m.getRole() == FamilyRole.OWNER || m.getRole() == FamilyRole.PARENT)
                .orElse(false);
    }

    public UUID getActiveFamilyId(UUID userId) {
        return getActiveMembership(userId).getFamily().getId();
    }

    public Family getFamilyEntity(UUID id) {
        return getFamily(id);
    }

    /** Throws if user is not the owner */
    public void assertOwner(Family family, UUID userId) {
        if (!family.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Only the family owner can perform this action");
        }
    }

    private Family getFamily(UUID id) {
        return familyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family", id));
    }

    private User getUser(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private FamilyDto toDto(Family f) {
        return new FamilyDto(f.getId(), f.getName(), f.getOwner().getId(), f.getCreatedAt());
    }

    private FamilyMemberDto toMemberDto(FamilyMember m) {
        return new FamilyMemberDto(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getFullName(),
                m.getUser().getEmail(),
                m.getUser().getAvatarUrl(),
                m.getRole(),
                m.getStatus(),
                m.getJoinedAt());
    }
}
