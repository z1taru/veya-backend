package com.veya.backend.auth;

import com.veya.backend.auth.dto.*;
import com.veya.backend.auth.security.JwtService;
import com.veya.backend.common.config.JwtProperties;
import com.veya.backend.common.exception.ConflictException;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.Family;
import com.veya.backend.families.FamilyMember;
import com.veya.backend.families.FamilyMemberRepository;
import com.veya.backend.families.FamilyRepository;
import com.veya.backend.common.enums.MemberRole;
import com.veya.backend.common.enums.MemberStatus;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import com.veya.backend.users.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final FamilyRepository familyRepo;
    private final FamilyMemberRepository memberRepo;
    private final RefreshTokenRepository refreshRepo;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered: " + req.email());
        }

        // Create user
        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();
        userRepo.save(user);

        // Create family and owner membership
        Family family = Family.builder()
                .name(req.familyName())
                .owner(user)
                .build();
        familyRepo.save(family);

        FamilyMember membership = FamilyMember.builder()
                .family(family)
                .user(user)
                .role(MemberRole.OWNER)
                .status(MemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
        memberRepo.save(membership);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String hash = hash(req.refreshToken());
        RefreshToken stored = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshRepo.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(java.util.UUID userId) {
        refreshRepo.revokeAllByUserId(userId);
    }

    public UserDto me(java.util.UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toDto(user);
    }

    // ── private helpers ───────────────────────────────────────

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken();

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtProps.getRefreshExpirationMs()))
                .build();
        refreshRepo.save(rt);

        return new AuthResponse(accessToken, refreshToken, toDto(user));
    }

    private String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(),
                u.getAvatarUrl(), u.getStatus(), u.getCreatedAt());
    }
}