package com.weddinggames.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weddinggames.backend.character.GameCharacter;
import com.weddinggames.backend.character.GameCharacterRepository;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import com.weddinggames.backend.team.TeamRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Persists one instance of every entity of the "game engine" relational foundation
 * (personnage, équipe, jeu, question, réponse, vote, score) end to end against the real
 * Postgres schema, and checks the two constraints later stories (matchmaking, voting) will
 * rely on: a participant belongs to at most one team, and a participant votes at most once
 * per question.
 */
class GameEngineSchemaIT extends AbstractIntegrationTest {

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private GameCharacterRepository gameCharacterRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    private WeddingEvent seedEvent() {
        return weddingEventRepository.findBySlug("seed-wedding").orElseThrow();
    }

    private Participant seedParticipant(String firstName, String lastName) {
        return participantRepository.findAll().stream()
                .filter(p -> p.getFirstName().equals(firstName) && p.getLastName().equals(lastName))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theFullGameEngineChainCanBePersistedAndReadBack() {
        WeddingEvent event = seedEvent();
        Participant jessika = seedParticipant("Jessika", "Dijoux");

        GameCharacter character = gameCharacterRepository.save(
                new GameCharacter(event, "Detective-" + UUID.randomUUID(), "Mène l'enquête", null));

        Team team = new Team(event);
        team.setCharacter(character);
        team = teamRepository.save(team);
        TeamMember membership = teamMemberRepository.save(new TeamMember(team, jessika));

        Game game = gameRepository.save(new Game(event, GameType.QUIZ, "Quiz du dessert", 0));
        Question question = questionRepository.save(
                new Question(game, "Quel est le dessert préféré des mariés ?", 0, QuestionSource.ADMIN, null));
        Answer answer = answerRepository.save(new Answer(question, team, "La tarte aux pommes", Instant.now()));
        Vote vote = voteRepository.save(new Vote(question, answer, jessika));
        Score score = scoreRepository.save(new Score(event, game, team, 10, "Bonne réponse"));

        assertThat(teamMemberRepository.findByTeamId(team.getId())).containsExactly(membership);
        assertThat(answerRepository.findByQuestionId(question.getId())).containsExactly(answer);
        assertThat(voteRepository.findByQuestionId(question.getId())).containsExactly(vote);
        assertThat(scoreRepository.findByTeamId(team.getId())).containsExactly(score);
        assertThat(scoreRepository.findByTeamId(team.getId()).get(0).getPoints()).isEqualTo(10);
    }

    @Test
    void aParticipantCanNeverBelongToTwoTeamsAtOnce() {
        WeddingEvent event = seedEvent();
        Participant sandrine = seedParticipant("Sandrine", "Santin");

        Team firstTeam = teamRepository.save(new Team(event));
        teamMemberRepository.saveAndFlush(new TeamMember(firstTeam, sandrine));

        Team secondTeam = teamRepository.save(new Team(event));
        assertThatThrownBy(() -> teamMemberRepository.saveAndFlush(new TeamMember(secondTeam, sandrine)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aParticipantCanNeverVoteTwiceOnTheSameQuestion() {
        WeddingEvent event = seedEvent();
        Participant patrick = seedParticipant("Patrick", "Santin");

        Team team = teamRepository.save(new Team(event));
        Game game = gameRepository.save(new Game(event, GameType.QUIZ, "Quiz du dessert", 1));
        Question question = questionRepository.save(new Question(game, "Question ?", 0, QuestionSource.ADMIN, null));
        Answer firstAnswer = answerRepository.save(new Answer(question, team, "Réponse A", Instant.now()));

        voteRepository.saveAndFlush(new Vote(question, firstAnswer, patrick));

        Team otherTeam = teamRepository.save(new Team(event));
        Answer secondAnswer = answerRepository.save(new Answer(question, otherTeam, "Réponse B", Instant.now()));

        assertThatThrownBy(() -> voteRepository.saveAndFlush(new Vote(question, secondAnswer, patrick)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
