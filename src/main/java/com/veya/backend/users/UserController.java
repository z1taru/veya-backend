package com.veya.backend.users;

import com.veya.backend.users.dto.UpdateProfileRequest;
import com.veya.backend.users.dto.UserDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getMe(uid(principal)));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDto> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.updateMe(uid(principal), request));
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
