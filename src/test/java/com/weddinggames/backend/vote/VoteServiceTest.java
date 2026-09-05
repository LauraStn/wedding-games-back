package com.weddinggames.backend.vote;

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
import com.weddinggames.backend.game.Vote;
import com.weddinggames.backend.game.VoteRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.team.Team;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the anonymized, anti-self-vote ballot logic. */
class VoteServiceTest {

    private QuestionRepository questionRepository;
    private AnswerRepository answerRepository;
    private VoteRepository voteRepository;
    private TeamMemberRepository teamMemberRepository;
    private VoteService service;

    private UUID questionId;
    private Question question;

    @BeforeEach
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        answerRepository = mock(AnswerRepository.class);
        voteRepository = mock(VoteRepository.class);
        teamMemberRepository = mock(TeamMemberRepository.class);
        service = new VoteService(questionRepository, answerRepository, voteRepository, teamMemberRepository);

        questionId = UUID.randomUUID();
        question = mock(Question.class);
        when(question.getId()).thenReturn(questionId);
        when(question.getStatus()).thenReturn(QuestionStatus.CLOSED);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
    }

    private Team mockTeam() {
        Team team = mock(Team.class);
        when(team.getId()).thenReturn(UUID.randomUUID());
        return team;
    }

    private Answer acceptedAnswerFor(Team team, String content) {
        Answer answer = new Answer(question, team, content, Instant.now());
        answer.accept();
        return answer;
    }

    private void stubTeamMembership(UUID participantId, Team team) {
        Participant participant = mock(Participant.class);
        when(participant.getId()).thenReturn(participantId);
        TeamMember membership = mock(TeamMember.class);
        when(membership.getTeam()).thenReturn(team);
        when(membership.getParticipant()).thenReturn(participant);
        when(teamMemberRepository.findByParticipantId(participantId)).thenReturn(Optional.of(membership));
    }

    @Test
    void ballotExcludesMyOwnTeamsAnswer() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        Team otherTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);

        Answer mine = acceptedAnswerFor(myTeam, "Ma reponse");
        Answer other = acceptedAnswerFor(otherTeam, "Autre reponse");
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(mine, other));

        List<Answer> ballot = service.listBallot(questionId, participantId);

        assertThat(ballot).hasSize(1);
        assertThat(ballot.get(0).getContent()).isEqualTo("Autre reponse");
    }

    @Test
    void ballotExcludesAnswersThatAreNotAccepted() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        Team otherTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);

        Answer pending = new Answer(question, otherTeam, "Pas encore validee", Instant.now());
        Answer hidden = new Answer(question, otherTeam, "Masquee", Instant.now());
        hidden.hide();
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(pending, hidden));

        List<Answer> ballot = service.listBallot(questionId, participantId);

        assertThat(ballot).isEmpty();
    }

    @Test
    void ballotFailsWhenTheQuestionIsNotClosedYet() {
        when(question.getStatus()).thenReturn(QuestionStatus.ACTIVE);

        assertThatThrownBy(() -> service.listBallot(questionId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void castVoteSucceedsForAnotherTeamsAcceptedAnswer() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        Team otherTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);
        Answer answer = acceptedAnswerFor(otherTeam, "Bonne reponse");
        UUID answerId = UUID.randomUUID();
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(voteRepository.existsByQuestionIdAndVoterParticipantId(questionId, participantId)).thenReturn(false);
        when(voteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Vote vote = service.castVote(questionId, participantId, answerId);

        assertThat(vote.getAnswer()).isSameAs(answer);
    }

    @Test
    void castVoteRejectsVotingForMyOwnTeam() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);
        Answer answer = acceptedAnswerFor(myTeam, "Ma reponse");
        UUID answerId = UUID.randomUUID();
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.castVote(questionId, participantId, answerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("propre equipe");
    }

    @Test
    void castVoteRejectsAnAnswerThatWasNotAccepted() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        Team otherTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);
        Answer answer = new Answer(question, otherTeam, "Pas encore validee", Instant.now());
        UUID answerId = UUID.randomUUID();
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.castVote(questionId, participantId, answerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("eligible");
    }

    @Test
    void castVoteRejectsASecondVoteOnTheSameQuestion() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        Team otherTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);
        Answer answer = acceptedAnswerFor(otherTeam, "Bonne reponse");
        UUID answerId = UUID.randomUUID();
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
        when(voteRepository.existsByQuestionIdAndVoterParticipantId(questionId, participantId)).thenReturn(true);

        assertThatThrownBy(() -> service.castVote(questionId, participantId, answerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("deja vote");
    }

    @Test
    void castVoteFailsForAnAnswerBelongingToAnotherQuestion() {
        UUID participantId = UUID.randomUUID();
        Team myTeam = mockTeam();
        stubTeamMembership(participantId, myTeam);
        Question otherQuestion = mock(Question.class);
        when(otherQuestion.getId()).thenReturn(UUID.randomUUID());
        Answer answer = new Answer(otherQuestion, mockTeam(), "D'une autre question", Instant.now());
        UUID answerId = UUID.randomUUID();
        when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> service.castVote(questionId, participantId, answerId))
                .isInstanceOf(NotFoundException.class);
    }
}
