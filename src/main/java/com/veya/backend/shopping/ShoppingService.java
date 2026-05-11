package com.veya.backend.shopping;

import com.veya.backend.common.exception.BadRequestException;
import com.veya.backend.common.exception.ResourceNotFoundException;
import com.veya.backend.families.Family;
import com.veya.backend.families.FamilyService;
import com.veya.backend.shopping.dto.CreateShoppingItemRequest;
import com.veya.backend.shopping.dto.ShoppingItemDto;
import com.veya.backend.shopping.dto.UpdateShoppingItemRequest;
import com.veya.backend.users.User;
import com.veya.backend.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShoppingService {

    private final ShoppingItemRepository shoppingRepo;
    private final FamilyService familyService;
    private final UserRepository userRepo;

    public List<ShoppingItemDto> getItems(UUID requesterId) {
        UUID familyId = familyService.getActiveFamilyId(requesterId);
        return shoppingRepo.findByFamilyIdOrderByCompletedAscCreatedAtDesc(familyId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ShoppingItemDto createItem(CreateShoppingItemRequest request, UUID requesterId) {
        UUID familyId = familyService.getActiveFamilyId(requesterId);
        Family family = familyService.getFamilyEntity(familyId);
        User user = getUser(requesterId);

        ShoppingItem item = ShoppingItem.builder()
                .family(family)
                .name(request.name())
                .quantity(request.quantity())
                .category(request.category())
                .addedBy(user)
                .build();

        return toDto(shoppingRepo.save(item));
    }

    @Transactional
    public ShoppingItemDto updateItem(UUID itemId, UpdateShoppingItemRequest request, UUID requesterId) {
        ShoppingItem item = getFamilyItem(itemId, requesterId);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Shopping item name cannot be blank");
            }
            item.setName(request.name());
        }
        if (request.quantity() != null) {
            item.setQuantity(request.quantity());
        }
        if (request.category() != null) {
            item.setCategory(request.category());
        }

        return toDto(shoppingRepo.save(item));
    }

    @Transactional
    public ShoppingItemDto toggleItem(UUID itemId, UUID requesterId) {
        ShoppingItem item = getFamilyItem(itemId, requesterId);
        boolean completed = !item.isCompleted();
        item.setCompleted(completed);
        item.setCompletedBy(completed ? getUser(requesterId) : null);
        return toDto(shoppingRepo.save(item));
    }

    @Transactional
    public void deleteItem(UUID itemId, UUID requesterId) {
        ShoppingItem item = getFamilyItem(itemId, requesterId);
        shoppingRepo.delete(item);
    }

    @Transactional
    public long deleteCompleted(UUID requesterId) {
        UUID familyId = familyService.getActiveFamilyId(requesterId);
        return shoppingRepo.deleteByFamilyIdAndCompletedTrue(familyId);
    }

    private ShoppingItem getFamilyItem(UUID itemId, UUID requesterId) {
        UUID familyId = familyService.getActiveFamilyId(requesterId);
        return shoppingRepo.findByIdAndFamilyId(itemId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item", itemId));
    }

    private User getUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private ShoppingItemDto toDto(ShoppingItem item) {
        return new ShoppingItemDto(
                item.getId(),
                item.getFamily().getId(),
                item.getName(),
                item.getQuantity(),
                item.getCategory(),
                item.getAddedBy().getId(),
                item.isCompleted(),
                item.getCompletedBy() != null ? item.getCompletedBy().getId() : null,
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
