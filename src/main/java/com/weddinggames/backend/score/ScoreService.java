package com.weddinggames.backend.score;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import com.weddinggames.backend.game.Score;
import com.weddinggames.backend.game.ScoreRepository;
import com.weddinggames.backend.score.dto.PodiumEntryResponse;
import com.weddinggames.backend.score.dto.ScoreAwardRequest;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Awards points to teams as an append-only ledger ({@link Score}) and computes the ranked podium
 * for an event from that ledger. The "barème" (how many points a win is worth) is not hardcoded
 * here: the caller (staff) supplies the point value at award time, so any scale can be configured
 * per game/round without a backend change.
 */
@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final WeddingEventRepository weddingEventRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;

    public ScoreService(
            ScoreRepository scoreRepository,
            WeddingEventRepository weddingEventRepository,
            GameRepository gameRepository,
            TeamRepository teamRepository) {
        this.scoreRepository = scoreRepository;
        this.weddingEventRepository = weddingEventRepository;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public Score award(UUID eventId, ScoreAwardRequest request) {
        WeddingEvent event = weddingEventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evenement introuvable."));
        Team team = teamRepository
                .findById(request.teamId())
                .orElseThrow(() -> new NotFoundException("Equipe introuvable."));
        Game game = request.gameId() == null
                ? null
                : gameRepository
                        .findById(request.gameId())
                        .orElseThrow(() -> new NotFoundException("Partie introuvable."));
        Score score = new Score(event, game, team, request.points(), request.reason());
        return scoreRepository.save(score);
    }

    @Transactional(readOnly = true)
    public List<Score> listByEvent(UUID eventId) {
        return scoreRepository.findByEventId(eventId);
    }

    /**
     * Ranks every team of the event by total points, descending. Ties share the same rank and the
     * next distinct total skips ranks by the number of tied teams above it (standard competition
     * ranking, e.g. 1-2-2-4), so no tie is ever broken arbitrarily.
     */
    @Transactional(readOnly = true)
    public List<PodiumEntryResponse> podium(UUID eventId) {
        List<Team> teams = teamRepository.findByEventId(eventId);
        Map<UUID, Long> totalByTeamId = new HashMap<>();
        for (Score score : scoreRepository.findByEventId(eventId)) {
            totalByTeamId.merge(score.getTeam().getId(), (long) score.getPoints(), Long::sum);
        }

        List<Team> ranked = new ArrayList<>(teams);
        ranked.sort(Comparator.comparingLong((Team team) -> totalByTeamId.getOrDefault(team.getId(), 0L))
                .reversed());

        List<PodiumEntryResponse> podium = new ArrayList<>();
        long previousTotal = Long.MIN_VALUE;
        int previousRank = 0;
        int position = 0;
        for (Team team : ranked) {
            position++;
            long total = totalByTeamId.getOrDefault(team.getId(), 0L);
            int rank = total == previousTotal ? previousRank : position;
            previousTotal = total;
            previousRank = rank;
            podium.add(new PodiumEntryResponse(team.getId(), team.getLabel(), total, rank));
        }
        return podium;
    }
}
