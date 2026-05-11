package com.veya.backend.users;

import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.users.dto.UpdateProfileRequest;
import com.veya.backend.users.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto getMe(UUID userId) {
        return toDto(getUser(userId));
    }

    @Transactional
    public UserDto updateMe(UUID userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return toDto(userRepository.save(user));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
