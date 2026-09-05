package com.weddinggames.backend.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerModerationStatus;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.team.Team;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for staff moderation of team answers. */
class AnswerModerationServiceTest {

    private AnswerRepository answerRepository;
    private AnswerModerationService service;

    @BeforeEach
    void setUp() {
        answerRepository = mock(AnswerRepository.class);
        service = new AnswerModerationService(answerRepository);
    }

    @Test
    void acceptMarksTheAnswerAsAccepted() {
        UUID id = UUID.randomUUID();
        Answer answer = new Answer(mock(Question.class), mock(Team.class), "42", Instant.now());
        when(answerRepository.findById(id)).thenReturn(Optional.of(answer));

        Answer result = service.accept(id);

        assertThat(result.getModerationStatus()).isEqualTo(AnswerModerationStatus.ACCEPTED);
    }

    @Test
    void hideMarksTheAnswerAsHidden() {
        UUID id = UUID.randomUUID();
        Answer answer = new Answer(mock(Question.class), mock(Team.class), "contenu inapproprie", Instant.now());
        when(answerRepository.findById(id)).thenReturn(Optional.of(answer));

        Answer result = service.hide(id);

        assertThat(result.getModerationStatus()).isEqualTo(AnswerModerationStatus.HIDDEN);
    }

    @Test
    void correctChangesTheContentWithoutTouchingModerationStatus() {
        UUID id = UUID.randomUUID();
        Answer answer = new Answer(mock(Question.class), mock(Team.class), "fote de frape", Instant.now());
        answer.accept();
        when(answerRepository.findById(id)).thenReturn(Optional.of(answer));

        Answer result = service.correct(id, "faute de frappe");

        assertThat(result.getContent()).isEqualTo("faute de frappe");
        assertThat(result.getModerationStatus()).isEqualTo(AnswerModerationStatus.ACCEPTED);
    }

    @Test
    void relaunchTeamClearsContentControlAndModerationStatus() {
        UUID questionId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(teamId);
        Answer answer = new Answer(mock(Question.class), team, "Reponse existante", Instant.now());
        answer.setControllingParticipant(mock(Participant.class));
        answer.hide();
        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.of(answer));

        Answer result = service.relaunchTeam(questionId, teamId);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getControllingParticipant()).isNull();
        assertThat(result.getModerationStatus()).isEqualTo(AnswerModerationStatus.PENDING);
        assertThat(result.getSubmittedAt()).isNull();
    }

    @Test
    void relaunchingATeamWithNoAnswerYetFails() {
        UUID questionId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.relaunchTeam(questionId, teamId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void acceptingAnUnknownAnswerFails() {
        UUID id = UUID.randomUUID();
        when(answerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(id)).isInstanceOf(NotFoundException.class);
    }
}
