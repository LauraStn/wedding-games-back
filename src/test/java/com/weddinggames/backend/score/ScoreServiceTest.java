package com.weddinggames.backend.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for point awarding and tie-safe podium ranking. */
class ScoreServiceTest {

    private ScoreRepository scoreRepository;
    private WeddingEventRepository weddingEventRepository;
    private GameRepository gameRepository;
    private TeamRepository teamRepository;
    private ScoreService service;
    private UUID eventId;
    private WeddingEvent event;

    @BeforeEach
    void setUp() {
        scoreRepository = mock(ScoreRepository.class);
        weddingEventRepository = mock(WeddingEventRepository.class);
        gameRepository = mock(GameRepository.class);
        teamRepository = mock(TeamRepository.class);
        service = new ScoreService(scoreRepository, weddingEventRepository, gameRepository, teamRepository);
        eventId = UUID.randomUUID();
        event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    }

    private Team mockTeam(UUID id) {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(id);
        when(teamRepository.findById(id)).thenReturn(Optional.of(team));
        return team;
    }

    @Test
    void awardsPointsToAnExistingTeamWithoutReferencingAGame() {
        UUID teamId = UUID.randomUUID();
        Team team = mockTeam(teamId);
        when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Score score = service.award(eventId, new ScoreAwardRequest(null, teamId, 10, "Manche 1"));

        assertThat(score.getEvent()).isEqualTo(event);
        assertThat(score.getTeam()).isEqualTo(team);
        assertThat(score.getGame()).isNull();
        assertThat(score.getPoints()).isEqualTo(10);
        assertThat(score.getReason()).isEqualTo("Manche 1");
    }

    @Test
    void awardsPointsReferencingASpecificGame() {
        UUID teamId = UUID.randomUUID();
        mockTeam(teamId);
        UUID gameId = UUID.randomUUID();
        Game game = mock(Game.class);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Score score = service.award(eventId, new ScoreAwardRequest(gameId, teamId, 5, "Bonus"));

        assertThat(score.getGame()).isEqualTo(game);
    }

    @Test
    void rejectsAwardingToAnUnknownTeam() {
        UUID teamId = UUID.randomUUID();
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.award(eventId, new ScoreAwardRequest(null, teamId, 10, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsAwardingForAnUnknownEvent() {
        UUID unknownEventId = UUID.randomUUID();
        when(weddingEventRepository.findById(unknownEventId)).thenReturn(Optional.empty());
        UUID teamId = UUID.randomUUID();

        assertThatThrownBy(() -> service.award(unknownEventId, new ScoreAwardRequest(null, teamId, 10, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsAwardingForAnUnknownGame() {
        UUID teamId = UUID.randomUUID();
        mockTeam(teamId);
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.award(eventId, new ScoreAwardRequest(gameId, teamId, 10, null)))
                .isInstanceOf(NotFoundException.class);
    }

    private Score scoreFor(Team team, int points) {
        Score score = mock(Score.class);
        when(score.getTeam()).thenReturn(team);
        when(score.getPoints()).thenReturn(points);
        return score;
    }

    @Test
    void ranksTeamsByTotalPointsDescending() {
        Team first = mockTeam(UUID.randomUUID());
        when(first.getLabel()).thenReturn("Les Champions");
        Team second = mockTeam(UUID.randomUUID());
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(first, second));
        Score firstA = scoreFor(first, 10);
        Score firstB = scoreFor(first, 5);
        Score secondScore = scoreFor(second, 8);
        when(scoreRepository.findByEventId(eventId)).thenReturn(List.of(firstA, firstB, secondScore));

        List<PodiumEntryResponse> podium = service.podium(eventId);

        assertThat(podium).hasSize(2);
        assertThat(podium.get(0).teamId()).isEqualTo(first.getId());
        assertThat(podium.get(0).totalPoints()).isEqualTo(15);
        assertThat(podium.get(0).teamLabel()).isEqualTo("Les Champions");
        assertThat(podium.get(0).rank()).isEqualTo(1);
        assertThat(podium.get(1).teamId()).isEqualTo(second.getId());
        assertThat(podium.get(1).totalPoints()).isEqualTo(8);
        assertThat(podium.get(1).rank()).isEqualTo(2);
    }

    @Test
    void teamsWithNoScoreYetAppearAtZeroPoints() {
        Team scored = mockTeam(UUID.randomUUID());
        Team unscored = mockTeam(UUID.randomUUID());
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(scored, unscored));
        Score scoredEntry = scoreFor(scored, 3);
        when(scoreRepository.findByEventId(eventId)).thenReturn(List.of(scoredEntry));

        List<PodiumEntryResponse> podium = service.podium(eventId);

        assertThat(podium).extracting(PodiumEntryResponse::totalPoints).containsExactly(3L, 0L);
    }

    @Test
    void tiedTeamsShareTheSameRankAndTheNextRankSkipsAccordingly() {
        Team first = mockTeam(UUID.randomUUID());
        Team second = mockTeam(UUID.randomUUID());
        Team third = mockTeam(UUID.randomUUID());
        when(teamRepository.findByEventId(eventId)).thenReturn(List.of(first, second, third));
        Score firstScore = scoreFor(first, 10);
        Score secondScore = scoreFor(second, 10);
        Score thirdScore = scoreFor(third, 5);
        when(scoreRepository.findByEventId(eventId)).thenReturn(List.of(firstScore, secondScore, thirdScore));

        List<PodiumEntryResponse> podium = service.podium(eventId);

        assertThat(podium).extracting(PodiumEntryResponse::rank).containsExactly(1, 1, 3);
    }
}
