package com.veya.backend.shopping;

import com.veya.backend.shopping.dto.CreateShoppingItemRequest;
import com.veya.backend.shopping.dto.ShoppingItemDto;
import com.veya.backend.shopping.dto.UpdateShoppingItemRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Shopping")
@RestController
@RequestMapping("/api/shopping")
@RequiredArgsConstructor
public class ShoppingController {

    private final ShoppingService shoppingService;

    @GetMapping
    public ResponseEntity<List<ShoppingItemDto>> getItems(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(shoppingService.getItems(uid(principal)));
    }

    @PostMapping
    public ResponseEntity<ShoppingItemDto> createItem(
            @Valid @RequestBody CreateShoppingItemRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shoppingService.createItem(request, uid(principal)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ShoppingItemDto> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShoppingItemRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(shoppingService.updateItem(id, request, uid(principal)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ShoppingItemDto> toggleItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(shoppingService.toggleItem(id, uid(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable UUID id, @AuthenticationPrincipal UserDetails principal) {
        shoppingService.deleteItem(id, uid(principal));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/completed")
    public ResponseEntity<Void> deleteCompleted(@AuthenticationPrincipal UserDetails principal) {
        shoppingService.deleteCompleted(uid(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
