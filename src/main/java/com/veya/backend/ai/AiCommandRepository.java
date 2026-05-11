package com.veya.backend.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiCommandRepository extends JpaRepository<AiCommand, UUID> {
    Optional<AiCommand> findByIdAndUserId(UUID id, UUID userId);
}
