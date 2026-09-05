package com.weddinggames.backend.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.character.GameCharacterRepository;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.PairingConstraintService;
import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyConnectionStatus;
import com.weddinggames.backend.lobby.LobbyParticipant;
import com.weddinggames.backend.lobby.LobbyParticipantRepository;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.matchmaking.dto.LatecomerOptionsResponse;
import com.weddinggames.backend.matchmaking.dto.TeamResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import com.weddinggames.backend.team.TeamRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit test (Mockito, no Spring context) for manual latecomer integration. */
class LatecomerIntegrationServiceTest {

    private WeddingEventRepository weddingEventRepository;
    private ParticipantRepository participantRepository;
    private LobbyRepository lobbyRepository;
    private LobbyParticipantRepository lobbyParticipantRepository;
    private PairingConstraintService pairingConstraintService;
    private TeamRepository teamRepository;
    private TeamMemberRepository teamMemberRepository;
    private GameCharacterRepository gameCharacterRepository;
    private LatecomerIntegrationService service;

    private UUID eventId;
    private WeddingEvent event;
    private Lobby lobby;

    @BeforeEach
    void setUp() {
        weddingEventRepository = mock(WeddingEventRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        lobbyRepository = mock(LobbyRepository.class);
        lobbyParticipantRepository = mock(LobbyParticipantRepository.class);
        pairingConstraintService = mock(PairingConstraintService.class);
        teamRepository = mock(TeamRepository.class);
        teamMemberRepository = mock(TeamMemberRepository.class);
        gameCharacterRepository = mock(GameCharacterRepository.class);
        service = new LatecomerIntegrationService(
                weddingEventRepository,
                participantRepository,
                lobbyRepository,
                lobbyParticipantRepository,
                pairingConstraintService,
                teamRepository,
                teamMemberRepository,
                gameCharacterRepository);

        eventId = UUID.randomUUID();
        event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        lobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lobby));
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of());
        when(teamMemberRepository.findByTeamIdIn(List.of())).thenReturn(List.of());
    }

    private Participant mockParticipant(String displayName) {
        Participant participant = mock(Participant.class);
        when(participant.getId()).thenReturn(UUID.randomUUID());
        when(participant.getDisplayName()).thenReturn(displayName);
        when(participant.getEvent()).thenReturn(event);
        return participant;
    }

    private void stubLate(Participant participant) {
        when(participantRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        LobbyParticipant entry = new LobbyParticipant(lobby, participant, Instant.now());
        entry.markLate();
        when(lobbyParticipantRepository.findByLobbyIdAndParticipantId(lobby.getId(), participant.getId()))
                .thenReturn(Optional.of(entry));
        when(teamMemberRepository.existsByParticipantId(participant.getId())).thenReturn(false);
    }

    private GameCharacter mockCharacter() {
        GameCharacter character = mock(GameCharacter.class);
        when(character.getId()).thenReturn(UUID.randomUUID());
        when(character.isActive()).thenReturn(true);
        return character;
    }

    @Test
    void rejectsAParticipantAlreadyOnATeam() {
        Participant alice = mockParticipant("Alice");
        when(participantRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(teamMemberRepository.existsByParticipantId(alice.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.getOptions(eventId, alice.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsAParticipantNotMarkedLate() {
        Participant alice = mockParticipant("Alice");
        when(participantRepository.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(teamMemberRepository.existsByParticipantId(alice.getId())).thenReturn(false);
        when(lobbyParticipantRepository.findByLobbyIdAndParticipantId(lobby.getId(), alice.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOptions(eventId, alice.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getOptionsListsOnlyBinomeTeamsWithoutHardExclusion() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);

        Team pairTeam = mock(Team.class);
        UUID pairTeamId = UUID.randomUUID();
        when(pairTeam.getId()).thenReturn(pairTeamId);
        Participant pairMemberA = mockParticipant("A");
        Participant pairMemberB = mockParticipant("B");
        TeamMember pairA = new TeamMember(pairTeam, pairMemberA);
        TeamMember pairB = new TeamMember(pairTeam, pairMemberB);

        Team trioTeam = mock(Team.class);
        UUID trioTeamId = UUID.randomUUID();
        when(trioTeam.getId()).thenReturn(trioTeamId);
        TeamMember trioA = new TeamMember(trioTeam, mockParticipant("C"));
        TeamMember trioB = new TeamMember(trioTeam, mockParticipant("D"));
        TeamMember trioC = new TeamMember(trioTeam, mockParticipant("E"));

        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(pairTeam, trioTeam));
        when(teamMemberRepository.findByTeamIdIn(List.of(pairTeamId, trioTeamId)))
                .thenReturn(List.of(pairA, pairB, trioA, trioB, trioC));
        UUID latecomerId = latecomer.getId();
        when(pairingConstraintService.hasHardExclusion(eq(eventId), eq(latecomerId), any())).thenReturn(false);

        LatecomerOptionsResponse options = service.getOptions(eventId, latecomer.getId());

        assertThat(options.compatibleTeams()).hasSize(1);
        assertThat(options.compatibleTeams().get(0).id()).isEqualTo(pairTeamId);
    }

    @Test
    void getOptionsExcludesATeamWithAHardExclusion() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);

        Team pairTeam = mock(Team.class);
        UUID pairTeamId = UUID.randomUUID();
        when(pairTeam.getId()).thenReturn(pairTeamId);
        Participant excluded = mockParticipant("Excluded");
        TeamMember memberA = new TeamMember(pairTeam, excluded);
        TeamMember memberB = new TeamMember(pairTeam, mockParticipant("B"));

        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(pairTeam));
        when(teamMemberRepository.findByTeamIdIn(List.of(pairTeamId))).thenReturn(List.of(memberA, memberB));
        when(pairingConstraintService.hasHardExclusion(eventId, latecomer.getId(), excluded.getId()))
                .thenReturn(true);

        LatecomerOptionsResponse options = service.getOptions(eventId, latecomer.getId());

        assertThat(options.compatibleTeams()).isEmpty();
    }

    @Test
    void joinExistingTeamAddsAThirdMemberWithACharacter() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);

        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        when(team.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        TeamMember existingA = new TeamMember(team, mockParticipant("A"));
        TeamMember existingB = new TeamMember(team, mockParticipant("B"));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(existingA, existingB));
        UUID latecomerId = latecomer.getId();
        when(pairingConstraintService.hasHardExclusion(eq(eventId), eq(latecomerId), any())).thenReturn(false);

        GameCharacter freeCharacter = mockCharacter();
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(List.of(freeCharacter));
        when(teamMemberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = service.joinExistingTeam(eventId, latecomer.getId(), teamId);

        assertThat(response.members()).hasSize(3);
        assertThat(response.members().stream().map(m -> m.participantId()))
                .contains(latecomer.getId());
    }

    @Test
    void joinExistingTeamRejectsATeamThatIsAlreadyATrio() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);
        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        when(team.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        TeamMember memberA = new TeamMember(team, mockParticipant("A"));
        TeamMember memberB = new TeamMember(team, mockParticipant("B"));
        TeamMember memberC = new TeamMember(team, mockParticipant("C"));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(memberA, memberB, memberC));

        assertThatThrownBy(() -> service.joinExistingTeam(eventId, latecomer.getId(), teamId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void joinExistingTeamRejectsWhenHardExcludedFromAnExistingMember() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);
        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        when(team.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        Participant excluded = mockParticipant("Excluded");
        Participant otherMember = mockParticipant("B");
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamId(teamId))
                .thenReturn(List.of(new TeamMember(team, excluded), new TeamMember(team, otherMember)));
        when(pairingConstraintService.hasHardExclusion(eventId, latecomer.getId(), excluded.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.joinExistingTeam(eventId, latecomer.getId(), teamId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void pairTwoLatecomersCreatesANewTeamWithTwoDistinctCharacters() {
        Participant alice = mockParticipant("Alice");
        Participant bob = mockParticipant("Bob");
        stubLate(alice);
        stubLate(bob);
        when(pairingConstraintService.hasHardExclusion(eventId, alice.getId(), bob.getId())).thenReturn(false);

        GameCharacter characterOne = mockCharacter();
        GameCharacter characterTwo = mockCharacter();
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(List.of(characterOne, characterTwo));
        when(teamRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = service.pairTwoLatecomers(eventId, alice.getId(), bob.getId());

        assertThat(response.members()).hasSize(2);
        List<UUID> characterIds =
                response.members().stream().map(m -> m.characterId()).toList();
        assertThat(characterIds).doesNotHaveDuplicates();
        assertThat(characterIds).containsExactlyInAnyOrder(characterOne.getId(), characterTwo.getId());
    }

    @Test
    void pairTwoLatecomersRejectsPairingAParticipantWithThemself() {
        Participant alice = mockParticipant("Alice");

        assertThatThrownBy(() -> service.pairTwoLatecomers(eventId, alice.getId(), alice.getId()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void pairTwoLatecomersRejectsAHardExclusionBetweenThem() {
        Participant alice = mockParticipant("Alice");
        Participant bob = mockParticipant("Bob");
        stubLate(alice);
        stubLate(bob);
        when(pairingConstraintService.hasHardExclusion(eventId, alice.getId(), bob.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.pairTwoLatecomers(eventId, alice.getId(), bob.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void joinExistingTeamFailsWhenTheTeamBelongsToAnotherEvent() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);
        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        WeddingEvent otherEvent = mock(WeddingEvent.class);
        when(otherEvent.getId()).thenReturn(UUID.randomUUID());
        when(team.getEvent()).thenReturn(otherEvent);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> service.joinExistingTeam(eventId, latecomer.getId(), teamId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void failsExplicitlyWhenNoActiveCharacterIsLeft() {
        Participant latecomer = mockParticipant("Late Guy");
        stubLate(latecomer);
        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        when(team.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(eventId);
        TeamMember memberA = new TeamMember(team, mockParticipant("A"));
        TeamMember memberB = new TeamMember(team, mockParticipant("B"));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(memberA, memberB));
        UUID latecomerId = latecomer.getId();
        when(pairingConstraintService.hasHardExclusion(eq(eventId), eq(latecomerId), any())).thenReturn(false);
        when(gameCharacterRepository.findByEventId(eventId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.joinExistingTeam(eventId, latecomer.getId(), teamId))
                .isInstanceOf(InvalidRequestException.class);
    }
}
