package com.veya.backend.shopping;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, UUID> {
    List<ShoppingItem> findByFamilyIdOrderByCompletedAscCreatedAtDesc(UUID familyId);

    Optional<ShoppingItem> findByIdAndFamilyId(UUID id, UUID familyId);

    long deleteByFamilyIdAndCompletedTrue(UUID familyId);
}
