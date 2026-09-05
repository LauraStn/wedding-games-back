package com.weddinggames.backend.character;

import com.weddinggames.backend.character.dto.GameCharacterCreateRequest;
import com.weddinggames.backend.character.dto.GameCharacterUpdateRequest;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.ConflictException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.team.TeamMemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameCharacterService {

    private final GameCharacterRepository gameCharacterRepository;
    private final WeddingEventRepository weddingEventRepository;
    private final TeamMemberRepository teamMemberRepository;

    public GameCharacterService(
            GameCharacterRepository gameCharacterRepository,
            WeddingEventRepository weddingEventRepository,
            TeamMemberRepository teamMemberRepository) {
        this.gameCharacterRepository = gameCharacterRepository;
        this.weddingEventRepository = weddingEventRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<GameCharacter> listByEvent(UUID eventId) {
        return gameCharacterRepository.findByEventId(eventId);
    }

    @Transactional(readOnly = true)
    public GameCharacter get(UUID id) {
        return gameCharacterRepository.findById(id).orElseThrow(() -> new NotFoundException("Personnage introuvable."));
    }

    @Transactional
    public GameCharacter create(UUID eventId, GameCharacterCreateRequest request) {
        WeddingEvent event = weddingEventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evenement introuvable."));
        if (gameCharacterRepository.existsByEventIdAndName(eventId, request.name())) {
            throw new ConflictException(
                    "CHARACTER_NAME_TAKEN", "Un personnage porte deja ce nom pour cet evenement.");
        }
        GameCharacter character =
                new GameCharacter(event, request.name(), request.description(), request.avatarUrl());
        character.setGender(request.gender());
        return gameCharacterRepository.save(character);
    }

    @Transactional
    public GameCharacter update(UUID id, GameCharacterUpdateRequest request) {
        GameCharacter character = get(id);
        if (!character.getName().equals(request.name())
                && gameCharacterRepository.existsByEventIdAndName(character.getEvent().getId(), request.name())) {
            throw new ConflictException(
                    "CHARACTER_NAME_TAKEN", "Un personnage porte deja ce nom pour cet evenement.");
        }
        character.setName(request.name());
        character.setDescription(request.description());
        character.setAvatarUrl(request.avatarUrl());
        character.setGender(request.gender());
        return character;
    }

    @Transactional
    public GameCharacter activate(UUID id) {
        GameCharacter character = get(id);
        character.setActive(true);
        return character;
    }

    @Transactional
    public GameCharacter deactivate(UUID id) {
        GameCharacter character = get(id);
        character.setActive(false);
        return character;
    }

    @Transactional
    public void delete(UUID id) {
        if (!gameCharacterRepository.existsById(id)) {
            throw new NotFoundException("Personnage introuvable.");
        }
        if (teamMemberRepository.existsByCharacterId(id)) {
            throw new BusinessRuleViolationException(
                    "CHARACTER_ASSIGNED_TO_TEAM",
                    "Ce personnage est deja attribue a une equipe et ne peut pas etre supprime.");
        }
        gameCharacterRepository.deleteById(id);
    }
}
