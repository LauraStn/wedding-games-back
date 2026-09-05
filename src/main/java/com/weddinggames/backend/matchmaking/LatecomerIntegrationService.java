package com.weddinggames.backend.matchmaking;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.character.GameCharacterRepository;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.PairingConstraintService;
import com.weddinggames.backend.lobby.LobbyConnectionStatus;
import com.weddinggames.backend.lobby.LobbyParticipantRepository;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.matchmaking.dto.LatecomerCandidateResponse;
import com.weddinggames.backend.matchmaking.dto.LatecomerOptionsResponse;
import com.weddinggames.backend.matchmaking.dto.TeamResponse;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import com.weddinggames.backend.team.TeamRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual, staff-triggered follow-up to a matchmaking launch: folds a latecomer (someone marked
 * LATE in the lobby, who arrived after teams were generated) into the existing arrangement,
 * either as a third member of an existing binôme or paired with another latecomer into a brand
 * new one. Never violates a HARD exclusion, exactly like the original launch.
 *
 * <p>Note: does NOT touch scoring. "The new member inherits the team's score per the configured
 * scoring rule" (per the story) has no home yet - there is no scoring engine, no per-team score
 * aggregation, and no such configuration field anywhere in the app today (see ASST-38/39 and the
 * rest of the game engine, still schema-only). Wiring that up here would be inventing a rule
 * nobody has specified. Revisit once the scoring system itself exists.
 */
@Service
public class LatecomerIntegrationService {

    private final WeddingEventRepository weddingEventRepository;
    private final ParticipantRepository participantRepository;
    private final LobbyRepository lobbyRepository;
    private final LobbyParticipantRepository lobbyParticipantRepository;
    private final PairingConstraintService pairingConstraintService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GameCharacterRepository gameCharacterRepository;

    public LatecomerIntegrationService(
            WeddingEventRepository weddingEventRepository,
            ParticipantRepository participantRepository,
            LobbyRepository lobbyRepository,
            LobbyParticipantRepository lobbyParticipantRepository,
            PairingConstraintService pairingConstraintService,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            GameCharacterRepository gameCharacterRepository) {
        this.weddingEventRepository = weddingEventRepository;
        this.participantRepository = participantRepository;
        this.lobbyRepository = lobbyRepository;
        this.lobbyParticipantRepository = lobbyParticipantRepository;
        this.pairingConstraintService = pairingConstraintService;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.gameCharacterRepository = gameCharacterRepository;
    }

    @Transactional(readOnly = true)
    public LatecomerOptionsResponse getOptions(UUID eventId, UUID participantId) {
        requireLatecomerNotYetTeamed(eventId, participantId);

        List<Team> teams = teamRepository.findByEventId(eventId);
        Map<UUID, List<TeamMember>> membersByTeamId = teamMemberRepository
                .findByTeamIdIn(teams.stream().map(Team::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(member -> member.getTeam().getId()));

        List<TeamResponse> compatibleTeams = new ArrayList<>();
        for (Team team : teams) {
            List<TeamMember> members = membersByTeamId.getOrDefault(team.getId(), List.of());
            if (members.size() != 2) {
                continue; // already a trio, or (shouldn't happen) empty
            }
            boolean anyHardExclusion = members.stream()
                    .anyMatch(member -> pairingConstraintService.hasHardExclusion(
                            eventId, participantId, member.getParticipant().getId()));
            if (!anyHardExclusion) {
                compatibleTeams.add(TeamResponse.from(team, members));
            }
        }

        List<LatecomerCandidateResponse> compatibleLatecomers = findUnassignedLatecomers(eventId).stream()
                .filter(other -> !other.getId().equals(participantId))
                .filter(other -> !pairingConstraintService.hasHardExclusion(eventId, participantId, other.getId()))
                .map(LatecomerCandidateResponse::from)
                .toList();

        return new LatecomerOptionsResponse(compatibleTeams, compatibleLatecomers);
    }

    @Transactional
    public TeamResponse joinExistingTeam(UUID eventId, UUID participantId, UUID teamId) {
        requireLatecomerNotYetTeamed(eventId, participantId);
        Team team = teamRepository
                .findById(teamId)
                .filter(t -> t.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new NotFoundException("Equipe introuvable pour cet evenement."));

        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        if (members.size() != 2) {
            throw new BusinessRuleViolationException(
                    "LATECOMER_TEAM_NOT_A_BINOME",
                    "Seule une equipe de 2 personnes peut accueillir un retardataire (elle deviendrait un trio).");
        }
        for (TeamMember member : members) {
            if (pairingConstraintService.hasHardExclusion(eventId, participantId, member.getParticipant().getId())) {
                throw new BusinessRuleViolationException(
                        "LATECOMER_HARD_EXCLUSION",
                        "Ce retardataire ne peut pas rejoindre cette equipe: exclusion absolue avec "
                                + member.getParticipant().getDisplayName() + ".");
            }
        }

        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        TeamMember newMember = new TeamMember(team, participant);
        newMember.setCharacter(pickUnusedActiveCharacters(eventId, 1).get(0));
        members = new ArrayList<>(members);
        members.add(teamMemberRepository.save(newMember));

        return TeamResponse.from(team, members);
    }

    @Transactional
    public TeamResponse pairTwoLatecomers(UUID eventId, UUID participantId, UUID otherParticipantId) {
        if (participantId.equals(otherParticipantId)) {
            throw new InvalidRequestException(
                    "LATECOMER_SAME_PARTICIPANT", "Un participant ne peut pas etre associe a lui-meme.");
        }
        requireLatecomerNotYetTeamed(eventId, participantId);
        requireLatecomerNotYetTeamed(eventId, otherParticipantId);
        if (pairingConstraintService.hasHardExclusion(eventId, participantId, otherParticipantId)) {
            throw new BusinessRuleViolationException(
                    "LATECOMER_HARD_EXCLUSION", "Ces deux retardataires ne peuvent pas etre associes: exclusion absolue.");
        }

        WeddingEvent event =
                weddingEventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Evenement introuvable."));
        Participant participant = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        Participant other = participantRepository
                .findById(otherParticipantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));

        List<GameCharacter> characters = pickUnusedActiveCharacters(eventId, 2);
        Team team = teamRepository.save(new Team(event));
        TeamMember firstMember = new TeamMember(team, participant);
        firstMember.setCharacter(characters.get(0));
        TeamMember secondMember = new TeamMember(team, other);
        secondMember.setCharacter(characters.get(1));

        List<TeamMember> members = List.of(
                teamMemberRepository.save(firstMember), teamMemberRepository.save(secondMember));
        return TeamResponse.from(team, members);
    }

    private void requireLatecomerNotYetTeamed(UUID eventId, UUID participantId) {
        Participant participant = participantRepository
                .findById(participantId)
                .filter(p -> p.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new NotFoundException("Participant introuvable pour cet evenement."));
        if (teamMemberRepository.existsByParticipantId(participantId)) {
            throw new BusinessRuleViolationException(
                    "LATECOMER_ALREADY_ON_A_TEAM",
                    participant.getDisplayName() + " appartient deja a une equipe.");
        }
        boolean isLate = lobbyRepository
                .findByEventId(eventId)
                .flatMap(lobby -> lobbyParticipantRepository.findByLobbyIdAndParticipantId(lobby.getId(), participantId))
                .map(entry -> entry.getConnectionStatus() == LobbyConnectionStatus.LATE)
                .orElse(false);
        if (!isLate) {
            throw new InvalidRequestException(
                    "PARTICIPANT_NOT_A_LATECOMER",
                    participant.getDisplayName() + " n'est pas marque comme retardataire dans le salon.");
        }
    }

    private List<Participant> findUnassignedLatecomers(UUID eventId) {
        return lobbyRepository
                .findByEventId(eventId)
                .map(lobby -> lobbyParticipantRepository.findByLobbyIdAndConnectionStatus(
                        lobby.getId(), LobbyConnectionStatus.LATE))
                .orElseGet(List::of)
                .stream()
                .map(entry -> entry.getParticipant())
                .filter(p -> !teamMemberRepository.existsByParticipantId(p.getId()))
                .toList();
    }

    private List<GameCharacter> pickUnusedActiveCharacters(UUID eventId, int count) {
        List<Team> teams = teamRepository.findByEventId(eventId);
        Set<UUID> usedCharacterIds = teamMemberRepository
                .findByTeamIdIn(teams.stream().map(Team::getId).toList())
                .stream()
                .map(TeamMember::getCharacter)
                .filter(c -> c != null)
                .map(GameCharacter::getId)
                .collect(Collectors.toSet());

        List<GameCharacter> available = gameCharacterRepository.findByEventId(eventId).stream()
                .filter(GameCharacter::isActive)
                .filter(c -> !usedCharacterIds.contains(c.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (available.size() < count) {
            throw new InvalidRequestException(
                    "MATCHMAKING_NOT_ENOUGH_CHARACTERS",
                    "Personnages actifs disponibles insuffisants (" + available.size() + ") pour attribuer "
                            + count + " retardataire(s).");
        }
        Collections.shuffle(available);
        return available.subList(0, count);
    }
}
