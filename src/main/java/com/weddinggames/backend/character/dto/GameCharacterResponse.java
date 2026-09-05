package com.weddinggames.backend.character.dto;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.common.Gender;
import java.time.Instant;
import java.util.UUID;

public record GameCharacterResponse(
        UUID id,
        UUID eventId,
        String name,
        String description,
        String avatarUrl,
        boolean active,
        Gender gender,
        Instant createdAt,
        Instant updatedAt) {

    public static GameCharacterResponse from(GameCharacter character) {
        return new GameCharacterResponse(
                character.getId(),
                character.getEvent().getId(),
                character.getName(),
                character.getDescription(),
                character.getAvatarUrl(),
                character.isActive(),
                character.getGender(),
                character.getCreatedAt(),
                character.getUpdatedAt());
    }
}
