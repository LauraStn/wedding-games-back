package com.weddinggames.backend.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.character.GameCharacterRepository;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.ExclusionType;
import com.weddinggames.backend.exclusion.PairingExclusion;
import com.weddinggames.backend.exclusion.PairingExclusionRepository;
import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyParticipant;
import com.weddinggames.backend.lobby.LobbyParticipantRepository;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.matchmaking.dto.TeamResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import com.weddinggames.backend.team.TeamRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit test (Mockito, no Spring context) for the matchmaking orchestration around the pure algorithm. */
class MatchmakingServiceTest {

    private WeddingEventRepository weddingEventRepository;
    private ParticipantRepository participantRepository;
    private LobbyRepository lobbyRepository;
    private LobbyParticipantRepository lobbyParticipantRepository;
    private PairingExclusionRepository pairingExclusionRepository;
    private TeamRepository teamRepository;
    private TeamMemberRepository teamMemberRepository;
    private GameCharacterRepository gameCharacterRepository;
    private MatchmakingService service;

    private WeddingEvent event;
    private UUID eventId;
    private Lobby lobby;

    @BeforeEach
    void setUp() {
        weddingEventRepository = mock(WeddingEventRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        lobbyRepository = mock(LobbyRepository.class);
        lobbyParticipantRepository = mock(LobbyParticipantRepository.class);
        pairingExclusionRepository = mock(PairingExclusionRepository.class);
        teamRepository = mock(TeamRepository.class);
        teamMemberRepository = mock(TeamMemberRepository.class);
        gameCharacterRepository = mock(GameCharacterRepository.class);
        service = new MatchmakingService(
                weddingEventRepository,
                participantRepository,
                lobbyRepository,
                lobbyParticipantRepository,
                pairingExclusionRepository,
                teamRepository,
                teamMemberRepository,
                gameCharacterRepository,
                new MatchmakingAlgorithm(new Random(42)));

        eventId = UUID.randomUUID();
        event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        lobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of());
        when(teamMemberRepository.findByTeamIdIn(List.of())).thenReturn(List.of());
        when(pairingExclusionRepository.findByEventId(eventId)).thenReturn(List.of());
        when(teamRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Participant mockParticipant(String displayName) {
        Participant participant = mock(Participant.class);
        when(participant.getId()).thenReturn(UUID.randomUUID());
        when(participant.getDisplayName()).thenReturn(displayName);
        return participant;
    }

    private LobbyParticipant connected(Participant participant) {
        return new LobbyParticipant(lobby, participant, Instant.now());
    }

    private GameCharacter mockCharacter(String name) {
        GameCharacter character = mock(GameCharacter.class);
        when(character.getId()).thenReturn(UUID.randomUUID());
        when(character.getName()).thenReturn(name);
        when(character.isActive()).thenReturn(true);
        return character;
    }

    @Test
    void launchCreatesTeamsAndAssignsADistinctCharacterToEveryParticipant() {
        List<Participant> participants =
                List.of(mockParticipant("A"), mockParticipant("B"), mockParticipant("C"), mockParticipant("D"));
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId()))
                .thenReturn(participants.stream().map(this::connected).toList());
        when(participantRepository.findAllById(any())).thenReturn(participants);
        List<GameCharacter> characters = List.of(
                mockCharacter("C1"), mockCharacter("C2"), mockCharacter("C3"), mockCharacter("C4"), mockCharacter("C5"));
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(characters);

        List<TeamResponse> result = service.launch(eventId);

        assertThat(result).hasSize(2);
        List<UUID> assignedCharacterIds = result.stream()
                .flatMap(team -> team.members().stream())
                .map(member -> member.characterId())
                .toList();
        assertThat(assignedCharacterIds).hasSize(4);
        assertThat(new java.util.HashSet<>(assignedCharacterIds)).hasSize(4);
        List<UUID> assignedParticipantIds = result.stream()
                .flatMap(team -> team.members().stream())
                .map(member -> member.participantId())
                .toList();
        assertThat(assignedParticipantIds)
                .containsExactlyInAnyOrderElementsOf(participants.stream().map(Participant::getId).toList());
    }

    @Test
    void launchFailsWhenFewerThanTwoParticipantsArePresent() {
        Participant alice = mockParticipant("Alice");
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId())).thenReturn(List.of(connected(alice)));

        assertThatThrownBy(() -> service.launch(eventId)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void launchIgnoresDisconnectedAndLateParticipants() {
        Participant alice = mockParticipant("Alice");
        Participant bob = mockParticipant("Bob");
        Participant lateGuest = mockParticipant("Late");
        LobbyParticipant lateEntry = connected(lateGuest);
        lateEntry.markLate();
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId()))
                .thenReturn(List.of(connected(alice), connected(bob), lateEntry));
        when(participantRepository.findAllById(any())).thenAnswer(invocation -> List.of(alice, bob));
        List<GameCharacter> characters = List.of(mockCharacter("C1"), mockCharacter("C2"));
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(characters);

        List<TeamResponse> result = service.launch(eventId);

        List<UUID> participantIds =
                result.stream().flatMap(t -> t.members().stream()).map(m -> m.participantId()).toList();
        assertThat(participantIds).containsExactlyInAnyOrder(alice.getId(), bob.getId());
    }

    @Test
    void launchFailsWhenTheCharacterCatalogIsSmallerThanThePresentGuestList() {
        List<Participant> participants = List.of(mockParticipant("A"), mockParticipant("B"));
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId()))
                .thenReturn(participants.stream().map(this::connected).toList());
        when(participantRepository.findAllById(any())).thenReturn(participants);
        List<GameCharacter> characters = List.of(mockCharacter("C1"));
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(characters);

        assertThatThrownBy(() -> service.launch(eventId)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void launchFailsWithAClearMessageWhenNoArrangementRespectsHardExclusions() {
        Participant jessika = mockParticipant("Jessika Dijoux");
        Participant sandrine = mockParticipant("Sandrine Santin");
        Participant patrick = mockParticipant("Patrick Santin");
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId()))
                .thenReturn(List.of(connected(jessika), connected(sandrine), connected(patrick)));
        when(participantRepository.findAllById(any())).thenReturn(List.of(jessika, sandrine, patrick));

        PairingExclusion exclusionOne = mock(PairingExclusion.class);
        when(exclusionOne.getExclusionType()).thenReturn(ExclusionType.HARD);
        when(exclusionOne.getParticipantA()).thenReturn(jessika);
        when(exclusionOne.getParticipantB()).thenReturn(sandrine);
        PairingExclusion exclusionTwo = mock(PairingExclusion.class);
        when(exclusionTwo.getExclusionType()).thenReturn(ExclusionType.HARD);
        when(exclusionTwo.getParticipantA()).thenReturn(jessika);
        when(exclusionTwo.getParticipantB()).thenReturn(patrick);
        when(pairingExclusionRepository.findByEventId(eventId)).thenReturn(List.of(exclusionOne, exclusionTwo));

        assertThatThrownBy(() -> service.launch(eventId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Jessika Dijoux")
                .hasMessageContaining("Sandrine Santin");
    }

    @Test
    void relaunchingClearsThePreviousTeamsBeforeRegenerating() {
        Team oldTeam = mock(Team.class);
        UUID oldTeamId = UUID.randomUUID();
        when(oldTeam.getId()).thenReturn(oldTeamId);
        TeamMember oldMember = mock(TeamMember.class);
        when(oldMember.getTeam()).thenReturn(oldTeam);
        Participant oldMemberParticipant = mockParticipant("Old member");
        when(oldMember.getParticipant()).thenReturn(oldMemberParticipant);
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(oldTeam));
        when(teamMemberRepository.findByTeamIdIn(List.of(oldTeamId))).thenReturn(List.of(oldMember));

        List<Participant> participants = List.of(mockParticipant("A"), mockParticipant("B"));
        when(lobbyParticipantRepository.findByLobbyId(lobby.getId()))
                .thenReturn(participants.stream().map(this::connected).toList());
        when(participantRepository.findAllById(any())).thenReturn(participants);
        List<GameCharacter> characters = List.of(mockCharacter("C1"), mockCharacter("C2"));
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(characters);

        service.launch(eventId);

        verify(teamMemberRepository).deleteAll(List.of(oldMember));
        verify(teamRepository).deleteAll(List.of(oldTeam));
    }

    @Test
    void listTeamsReturnsTheCurrentlyPersistedTeamsWithoutRegenerating() {
        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(team));
        when(teamMemberRepository.findByTeamIdIn(List.of(teamId))).thenReturn(List.of());

        List<TeamResponse> result = service.listTeams(eventId);

        assertThat(result).hasSize(1);
        verify(teamRepository, never()).save(any());
    }
}
