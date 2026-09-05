package com.weddinggames.backend.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionRepository;
import com.weddinggames.backend.game.QuestionStatus;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the take-control/live-edit answer flow. */
class QuizAnswerServiceTest {

    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private TeamMemberRepository teamMemberRepository;
    private QuizAnswerService service;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        answerRepository = mock(AnswerRepository.class);
        teamMemberRepository = mock(TeamMemberRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new QuizAnswerService(questionRepository, answerRepository, teamMemberRepository, clock);
    }

    private Participant mockParticipant(UUID id) {
        Participant participant = mock(Participant.class);
        when(participant.getId()).thenReturn(id);
        return participant;
    }

    @Test
    void takeControlCreatesTheAnswerRowOnFirstUse() {
        UUID questionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        Participant participant = mockParticipant(participantId);
        TeamMember membership = mock(TeamMember.class);
        when(membership.getTeam()).thenReturn(team);
        when(membership.getParticipant()).thenReturn(participant);
        when(teamMemberRepository.findByParticipantId(participantId)).thenReturn(Optional.of(membership));

        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.empty());
        when(answerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Answer result = service.takeControl(questionId, participantId);

        assertThat(result.getControllingParticipant()).isSameAs(participant);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void takeControlTransfersControlWithoutErasingExistingContent() {
        UUID questionId = UUID.randomUUID();
        UUID newWriterId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        Participant newWriter = mockParticipant(newWriterId);
        TeamMember membership = mock(TeamMember.class);
        when(membership.getTeam()).thenReturn(team);
        when(membership.getParticipant()).thenReturn(newWriter);
        when(teamMemberRepository.findByParticipantId(newWriterId)).thenReturn(Optional.of(membership));

        Answer existingAnswer = new Answer(question, team, "Reponse en cours", null);
        existingAnswer.setControllingParticipant(mockParticipant(UUID.randomUUID()));
        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.of(existingAnswer));
        when(answerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Answer result = service.takeControl(questionId, newWriterId);

        assertThat(result.getControllingParticipant()).isSameAs(newWriter);
        assertThat(result.getContent()).isEqualTo("Reponse en cours");
    }

    @Test
    void takeControlFailsWhenTheQuestionIsNotActive() {
        UUID questionId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.PENDING);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.takeControl(questionId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void takeControlFailsWhenTheParticipantIsNotOnATeam() {
        UUID questionId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(teamMemberRepository.findByParticipantId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.takeControl(questionId, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateContentSucceedsForTheParticipantInControl() {
        UUID questionId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        TeamMember membership = mock(TeamMember.class);
        when(membership.getTeam()).thenReturn(team);
        when(teamMemberRepository.findByParticipantId(participantId)).thenReturn(Optional.of(membership));

        Answer answer = new Answer(question, team, "", null);
        answer.setControllingParticipant(mockParticipant(participantId));
        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.of(answer));

        Answer result = service.updateContent(questionId, participantId, "42");

        assertThat(result.getContent()).isEqualTo("42");
        assertThat(result.getSubmittedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void updateContentFailsForAParticipantWhoDoesNotCurrentlyHoldThePen() {
        UUID questionId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        TeamMember membership = mock(TeamMember.class);
        when(membership.getTeam()).thenReturn(team);
        when(teamMemberRepository.findByParticipantId(callerId)).thenReturn(Optional.of(membership));

        Answer answer = new Answer(question, team, "Deja ecrit", null);
        answer.setControllingParticipant(mockParticipant(UUID.randomUUID()));
        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.updateContent(questionId, callerId, "Vole la main"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(answer.getContent()).isEqualTo("Deja ecrit");
    }

    @Test
    void updateContentFailsWhenNoOneHasTakenControlYet() {
        UUID questionId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        Team team = mock(Team.class);
        UUID teamId = UUID.randomUUID();
        when(team.getId()).thenReturn(teamId);
        TeamMember membership = mock(TeamMember.class);
        when(membership.getTeam()).thenReturn(team);
        when(teamMemberRepository.findByParticipantId(callerId)).thenReturn(Optional.of(membership));
        when(answerRepository.findByQuestionIdAndTeamId(questionId, teamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateContent(questionId, callerId, "Reponse"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void updateContentFailsWhenTheQuestionIsNoLongerActive() {
        UUID questionId = UUID.randomUUID();
        Question question = mock(Question.class);
        when(question.getStatus()).thenReturn(QuestionStatus.CLOSED);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.updateContent(questionId, UUID.randomUUID(), "Trop tard"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
