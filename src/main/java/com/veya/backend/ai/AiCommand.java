package com.veya.backend.ai;

import com.veya.backend.common.enums.AiCommandStatus;
import com.veya.backend.common.enums.AiParsedType;
import com.veya.backend.families.Family;
import com.veya.backend.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_commands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "parsed_type", nullable = false, columnDefinition = "ai_parsed_type")
    @Builder.Default
    private AiParsedType parsedType = AiParsedType.UNKNOWN;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_payload", columnDefinition = "jsonb")
    private String parsedPayload;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "ai_cmd_status")
    @Builder.Default
    private AiCommandStatus status = AiCommandStatus.PREVIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
