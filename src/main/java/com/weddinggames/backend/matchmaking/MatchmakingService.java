package com.weddinggames.backend.matchmaking;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.character.GameCharacterRepository;
import com.weddinggames.backend.common.Gender;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.exclusion.ExclusionType;
import com.weddinggames.backend.exclusion.PairingExclusion;
import com.weddinggames.backend.exclusion.PairingExclusionRepository;
import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyConnectionStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates a matchmaking launch: gathers the present participants, applies the pure
 * {@link MatchmakingAlgorithm}, replaces any previous teams for the event, and assigns each
 * participant their own character. Can be relaunched at any time; the previous teams are simply
 * cleared and regenerated, respecting the same rules.
 */
@Service
public class MatchmakingService {

    private final WeddingEventRepository weddingEventRepository;
    private final ParticipantRepository participantRepository;
    private final LobbyRepository lobbyRepository;
    private final LobbyParticipantRepository lobbyParticipantRepository;
    private final PairingExclusionRepository pairingExclusionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final GameCharacterRepository gameCharacterRepository;
    private final MatchmakingAlgorithm algorithm;

    public MatchmakingService(
            WeddingEventRepository weddingEventRepository,
            ParticipantRepository participantRepository,
            LobbyRepository lobbyRepository,
            LobbyParticipantRepository lobbyParticipantRepository,
            PairingExclusionRepository pairingExclusionRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            GameCharacterRepository gameCharacterRepository,
            MatchmakingAlgorithm algorithm) {
        this.weddingEventRepository = weddingEventRepository;
        this.participantRepository = participantRepository;
        this.lobbyRepository = lobbyRepository;
        this.lobbyParticipantRepository = lobbyParticipantRepository;
        this.pairingExclusionRepository = pairingExclusionRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.gameCharacterRepository = gameCharacterRepository;
        this.algorithm = algorithm;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeams(UUID eventId) {
        List<Team> teams = teamRepository.findByEventId(eventId);
        List<UUID> teamIds = teams.stream().map(Team::getId).toList();
        Map<UUID, List<TeamMember>> membersByTeamId = teamMemberRepository.findByTeamIdIn(teamIds).stream()
                .collect(Collectors.groupingBy(member -> member.getTeam().getId()));
        return teams.stream()
                .map(team -> TeamResponse.from(team, membersByTeamId.getOrDefault(team.getId(), List.of())))
                .toList();
    }

    @Transactional
    public List<TeamResponse> launch(UUID eventId) {
        WeddingEvent event =
                weddingEventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Evenement introuvable."));
        Lobby lobby = lobbyRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun salon pour cet evenement."));

        List<LobbyParticipant> presentEntries = lobbyParticipantRepository.findByLobbyId(lobby.getId()).stream()
                .filter(entry -> entry.getConnectionStatus() == LobbyConnectionStatus.CONNECTED
                        || entry.getConnectionStatus() == LobbyConnectionStatus.READY)
                .toList();
        List<UUID> presentIds =
                presentEntries.stream().map(entry -> entry.getParticipant().getId()).toList();
        if (presentIds.size() < 2) {
            throw new InvalidRequestException(
                    "MATCHMAKING_NOT_ENOUGH_PARTICIPANTS",
                    "Il faut au moins 2 participants presents dans le salon pour lancer le matchmaking.");
        }

        List<Team> existingTeams = teamRepository.findByEventId(eventId);
        List<UUID> existingTeamIds = existingTeams.stream().map(Team::getId).toList();
        List<TeamMember> existingMembers = teamMemberRepository.findByTeamIdIn(existingTeamIds);
        Set<UnorderedPair> previousPairs = derivePreviousPairs(existingTeams, existingMembers);

        // Flushed immediately: Hibernate would otherwise batch these deletes after the inserts
        // below (it orders the flush queue by action type, not call order), which would trip the
        // "one team per participant" / "one team per character" unique constraints when a
        // relaunch reassigns the same participant or character.
        teamMemberRepository.deleteAll(existingMembers);
        teamMemberRepository.flush();
        teamRepository.deleteAll(existingTeams);
        teamRepository.flush();

        List<PairingExclusion> exclusions = pairingExclusionRepository.findByEventId(eventId);
        Set<UnorderedPair> hardExclusions = toPairs(exclusions, ExclusionType.HARD);
        Set<UnorderedPair> softExclusions = toPairs(exclusions, ExclusionType.PREFERENCE);

        MatchmakingAlgorithm.Input input =
                new MatchmakingAlgorithm.Input(presentIds, hardExclusions, softExclusions, previousPairs);
        List<MatchmakingAlgorithm.Group> groups = algorithm.generate(input).orElseThrow(() -> {
            Map<UUID, Participant> byId = participantRepository.findAllById(presentIds).stream()
                    .collect(Collectors.toMap(Participant::getId, p -> p));
            return new BusinessRuleViolationException(
                    "MATCHMAKING_INFEASIBLE", buildInfeasibilityMessage(algorithm.relevantHardExclusions(input), byId));
        });

        List<GameCharacter> activeCharacters = gameCharacterRepository.findByEventId(eventId).stream()
                .filter(GameCharacter::isActive)
                .collect(Collectors.toCollection(ArrayList::new));
        if (activeCharacters.size() < presentIds.size()) {
            throw new InvalidRequestException(
                    "MATCHMAKING_NOT_ENOUGH_CHARACTERS",
                    "Le catalogue de personnages actifs (" + activeCharacters.size()
                            + ") est insuffisant pour attribuer un personnage a chacun des " + presentIds.size()
                            + " participants presents.");
        }
        Map<UUID, Participant> participantById = participantRepository.findAllById(presentIds).stream()
                .collect(Collectors.toMap(Participant::getId, p -> p));
        Map<UUID, Gender> participantGenders = participantById.values().stream()
                .filter(p -> p.getGender() != null)
                .collect(Collectors.toMap(Participant::getId, Participant::getGender));
        List<UUID> characterIds = activeCharacters.stream().map(GameCharacter::getId).toList();
        Map<UUID, Gender> characterGenders = activeCharacters.stream()
                .filter(c -> c.getGender() != null)
                .collect(Collectors.toMap(GameCharacter::getId, GameCharacter::getGender));
        Map<UUID, GameCharacter> characterById = activeCharacters.stream()
                .collect(Collectors.toMap(GameCharacter::getId, c -> c));
        Map<UUID, UUID> characterAssignment =
                algorithm.assignCharacters(presentIds, participantGenders, characterIds, characterGenders);

        List<TeamResponse> responses = new ArrayList<>();
        for (MatchmakingAlgorithm.Group group : groups) {
            Team team = teamRepository.save(new Team(event));
            List<TeamMember> members = new ArrayList<>();
            for (UUID participantId : group.participantIds()) {
                TeamMember member = new TeamMember(team, participantById.get(participantId));
                member.setCharacter(characterById.get(characterAssignment.get(participantId)));
                members.add(teamMemberRepository.save(member));
            }
            responses.add(TeamResponse.from(team, members));
        }
        return responses;
    }

    private Set<UnorderedPair> toPairs(List<PairingExclusion> exclusions, ExclusionType type) {
        return exclusions.stream()
                .filter(exclusion -> exclusion.getExclusionType() == type)
                .map(exclusion -> new UnorderedPair(
                        exclusion.getParticipantA().getId(), exclusion.getParticipantB().getId()))
                .collect(Collectors.toSet());
    }

    private Set<UnorderedPair> derivePreviousPairs(List<Team> teams, List<TeamMember> members) {
        Map<UUID, List<UUID>> participantIdsByTeamId = members.stream()
                .collect(Collectors.groupingBy(
                        member -> member.getTeam().getId(),
                        Collectors.mapping(member -> member.getParticipant().getId(), Collectors.toList())));
        Set<UnorderedPair> pairs = new java.util.HashSet<>();
        for (Team team : teams) {
            List<UUID> ids = participantIdsByTeamId.getOrDefault(team.getId(), List.of());
            for (int i = 0; i < ids.size(); i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    pairs.add(new UnorderedPair(ids.get(i), ids.get(j)));
                }
            }
        }
        return pairs;
    }

    private String buildInfeasibilityMessage(List<UnorderedPair> conflicts, Map<UUID, Participant> participantById) {
        String details = conflicts.stream()
                .map(pair -> participantById.get(pair.first()).getDisplayName() + " / "
                        + participantById.get(pair.second()).getDisplayName())
                .collect(Collectors.joining(", "));
        return "Aucune repartition ne respecte simultanement les exclusions absolues suivantes parmi les "
                + "participants presents: " + details;
    }
}
