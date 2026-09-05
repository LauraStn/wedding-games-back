package com.weddinggames.backend.character;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.character.dto.GameCharacterCreateRequest;
import com.weddinggames.backend.character.dto.GameCharacterUpdateRequest;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.ConflictException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.team.TeamMemberRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the character catalog CRUD rules. */
class GameCharacterServiceTest {

    private GameCharacterRepository gameCharacterRepository;
    private WeddingEventRepository weddingEventRepository;
    private TeamMemberRepository teamMemberRepository;
    private GameCharacterService service;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        gameCharacterRepository = mock(GameCharacterRepository.class);
        weddingEventRepository = mock(WeddingEventRepository.class);
        teamMemberRepository = mock(TeamMemberRepository.class);
        service = new GameCharacterService(gameCharacterRepository, weddingEventRepository, teamMemberRepository);
        eventId = UUID.randomUUID();
    }

    @Test
    void createsACharacterWhenTheNameIsFreeForTheEvent() {
        WeddingEvent event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(gameCharacterRepository.existsByEventIdAndName(eventId, "Sangoku")).thenReturn(false);
        when(gameCharacterRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GameCharacter created =
                service.create(eventId, new GameCharacterCreateRequest("Sangoku", "Super Saiyan", null, null));

        assertThat(created.getName()).isEqualTo("Sangoku");
    }

    @Test
    void rejectsCreationWhenTheNameIsAlreadyTakenForTheEvent() {
        WeddingEvent event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(gameCharacterRepository.existsByEventIdAndName(eventId, "Sangoku")).thenReturn(true);

        assertThatThrownBy(() ->
                        service.create(eventId, new GameCharacterCreateRequest("Sangoku", null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivateSetsActiveToFalse() {
        UUID id = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        GameCharacter character = new GameCharacter(event, "Sangoku", null, null);
        when(gameCharacterRepository.findById(id)).thenReturn(Optional.of(character));

        GameCharacter result = service.deactivate(id);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void updateRejectsRenamingToAnAlreadyTakenName() {
        UUID id = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        GameCharacter character = new GameCharacter(event, "Sangoku", null, null);
        when(gameCharacterRepository.findById(id)).thenReturn(Optional.of(character));
        when(gameCharacterRepository.existsByEventIdAndName(eventId, "Sailor Moon")).thenReturn(true);

        assertThatThrownBy(() ->
                        service.update(id, new GameCharacterUpdateRequest("Sailor Moon", null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deletingACharacterAssignedToATeamIsRefused() {
        UUID id = UUID.randomUUID();
        when(gameCharacterRepository.existsById(id)).thenReturn(true);
        when(teamMemberRepository.existsByCharacterId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(BusinessRuleViolationException.class);

        verify(gameCharacterRepository, never()).deleteById(id);
    }

    @Test
    void deletingAnUnassignedCharacterIsAllowed() {
        UUID id = UUID.randomUUID();
        when(gameCharacterRepository.existsById(id)).thenReturn(true);
        when(teamMemberRepository.existsByCharacterId(id)).thenReturn(false);

        service.delete(id);

        verify(gameCharacterRepository).deleteById(id);
    }

    @Test
    void deletingAnUnknownCharacterIsNotFound() {
        UUID id = UUID.randomUUID();
        when(gameCharacterRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(NotFoundException.class);
    }
}
