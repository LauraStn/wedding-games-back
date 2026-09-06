package com.weddinggames.backend.jury;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionRepository;
import com.weddinggames.backend.jury.dto.JuryPointsRequest;
import com.weddinggames.backend.score.ScoreService;
import com.weddinggames.backend.score.dto.ScoreAwardRequest;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.vote.FinalistService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the jury workflow orchestration. */
class JuryServiceTest {

    private JuryDecisionRepository decisionRepository;
    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private FinalistService finalistService;
    private ScoreService scoreService;
    private JuryService service;
    private UUID questionId;
    private Question question;
    private UUID eventId;
    private UUID gameId;

    @BeforeEach
    void setUp() {
        decisionRepository = mock(JuryDecisionRepository.class);
        questionRepository = mock(QuestionRepository.class);
        answerRepository = mock(AnswerRepository.class);
        finalistService = mock(FinalistService.class);
        scoreService = mock(ScoreService.class);
        service = new JuryService(decisionRepository, questionRepository, answerRepository, finalistService, scoreService);

        questionId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(gameId);
        when(game.getEvent()).thenReturn(event);
        question = mock(Question.class);
        when(question.getGame()).thenReturn(game);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Answer answerWithTeam(UUID answerId, UUID teamId) {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(teamId);
        Answer answer = mock(Answer.class);
        when(answer.getId()).thenReturn(answerId);
        when(answer.getTeam()).thenReturn(team);
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        return answer;
    }

    @Test
    void choosesAFinalistAnswer() {
        UUID answerId = UUID.randomUUID();
        Answer answer = answerWithTeam(answerId, UUID.randomUUID());
        JuryDecision decision = new JuryDecision(question);
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));
        when(finalistService.computeFinalists(questionId)).thenReturn(List.of(new FinalistService.Finalist(answer, 3L)));

        JuryDecision result = service.choose(questionId, answerId);

        assertThat(result.getChosenAnswer()).isEqualTo(answer);
        assertThat(result.getStatus()).isEqualTo(JuryDecisionStatus.CHOSEN);
    }

    @Test
    void rejectsChoosingAnAnswerThatIsNotAFinalist() {
        UUID answerId = UUID.randomUUID();
        answerWithTeam(answerId, UUID.randomUUID());
        JuryDecision decision = new JuryDecision(question);
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));
        when(finalistService.computeFinalists(questionId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.choose(questionId, answerId))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsChoosingAnUnknownAnswer() {
        UUID answerId = UUID.randomUUID();
        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(new JuryDecision(question)));

        assertThatThrownBy(() -> service.choose(questionId, answerId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void confirmingAwardsPointsToTheChosenTeam() {
        UUID answerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Answer answer = answerWithTeam(answerId, teamId);
        JuryDecision decision = new JuryDecision(question);
        decision.choose(answer);
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));

        JuryDecision result = service.confirm(questionId, new JuryPointsRequest(10, null));

        assertThat(result.getStatus()).isEqualTo(JuryDecisionStatus.CONFIRMED);
        verify(scoreService).award(eq(eventId), eq(new ScoreAwardRequest(gameId, teamId, 10, "Reponse gagnante")));
    }

    @Test
    void confirmingUsesTheProvidedReasonWhenGiven() {
        UUID answerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Answer answer = answerWithTeam(answerId, teamId);
        JuryDecision decision = new JuryDecision(question);
        decision.choose(answer);
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));

        service.confirm(questionId, new JuryPointsRequest(10, "Reponse la plus dröle"));

        verify(scoreService)
                .award(eq(eventId), eq(new ScoreAwardRequest(gameId, teamId, 10, "Reponse la plus dröle")));
    }

    @Test
    void rejectsConfirmingWithoutHavingChosen() {
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(new JuryDecision(question)));

        assertThatThrownBy(() -> service.confirm(questionId, new JuryPointsRequest(10, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void awardsABonusOnceConfirmed() {
        UUID answerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Answer answer = answerWithTeam(answerId, teamId);
        JuryDecision decision = new JuryDecision(question);
        decision.choose(answer);
        decision.confirm();
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));

        service.bonus(questionId, new JuryPointsRequest(5, null));

        verify(scoreService).award(eq(eventId), eq(new ScoreAwardRequest(gameId, teamId, 5, "Bonus jury")));
    }

    @Test
    void rejectsBonusBeforeConfirming() {
        UUID answerId = UUID.randomUUID();
        Answer answer = answerWithTeam(answerId, UUID.randomUUID());
        JuryDecision decision = new JuryDecision(question);
        decision.choose(answer);
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));

        assertThatThrownBy(() -> service.bonus(questionId, new JuryPointsRequest(5, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void revealsAConfirmedDecision() {
        UUID answerId = UUID.randomUUID();
        Answer answer = answerWithTeam(answerId, UUID.randomUUID());
        JuryDecision decision = new JuryDecision(question);
        decision.choose(answer);
        decision.confirm();
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(decision));

        JuryDecision result = service.reveal(questionId);

        assertThat(result.isRevealed()).isTrue();
    }

    @Test
    void rejectsRevealingBeforeConfirming() {
        when(decisionRepository.findByQuestionId(questionId)).thenReturn(Optional.of(new JuryDecision(question)));

        assertThatThrownBy(() -> service.reveal(questionId)).isInstanceOf(BusinessRuleViolationException.class);
    }
}
