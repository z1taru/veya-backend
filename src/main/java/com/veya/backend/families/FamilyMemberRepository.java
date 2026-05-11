package com.veya.backend.families;

import com.veya.backend.common.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    List<FamilyMember> findByFamilyIdAndStatus(UUID familyId, MemberStatus status);

    Optional<FamilyMember> findByFamilyIdAndUserId(UUID familyId, UUID userId);

    boolean existsByFamilyIdAndUserId(UUID familyId, UUID userId);

    @Query("SELECT fm FROM FamilyMember fm WHERE fm.user.id = :userId AND fm.status = 'ACTIVE'")
    Optional<FamilyMember> findActiveByUserId(UUID userId);
}