package com.weddinggames.backend.vote;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerModerationStatus;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.QuestionRepository;
import com.weddinggames.backend.game.QuestionStatus;
import com.weddinggames.backend.game.Vote;
import com.weddinggames.backend.game.VoteRepository;
import com.weddinggames.backend.team.TeamMember;
import com.weddinggames.backend.team.TeamMemberRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public vote on a question's accepted answers: shuffled order, no team/character identity ever
 * exposed (see {@link com.weddinggames.backend.vote.dto.VotingOptionResponse}), and a participant
 * can never see or vote for their own team's answer. Voting opens once the question is CLOSED
 * (answers finalized) - the {@code quiz} package owns closing it.
 */
@Service
public class VoteService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final VoteRepository voteRepository;
    private final TeamMemberRepository teamMemberRepository;

    public VoteService(
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            VoteRepository voteRepository,
            TeamMemberRepository teamMemberRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.voteRepository = voteRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<Answer> listBallot(UUID questionId, UUID participantId) {
        requireClosedQuestion(questionId);
        UUID myTeamId = requireTeamMembership(participantId).getTeam().getId();

        List<Answer> options = answerRepository.findByQuestionId(questionId).stream()
                .filter(answer -> answer.getModerationStatus() == AnswerModerationStatus.ACCEPTED)
                .filter(answer -> !answer.getTeam().getId().equals(myTeamId))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(options);
        return options;
    }

    @Transactional
    public Vote castVote(UUID questionId, UUID participantId, UUID answerId) {
        Question question = requireClosedQuestion(questionId);
        TeamMember membership = requireTeamMembership(participantId);

        Answer answer = answerRepository
                .findById(answerId)
                .filter(a -> a.getQuestion().getId().equals(questionId))
                .orElseThrow(() -> new NotFoundException("Reponse introuvable pour cette question."));
        if (answer.getModerationStatus() != AnswerModerationStatus.ACCEPTED) {
            throw new BusinessRuleViolationException(
                    "ANSWER_NOT_ACCEPTED", "Cette reponse n'est pas eligible au vote.");
        }
        if (answer.getTeam().getId().equals(membership.getTeam().getId())) {
            throw new BusinessRuleViolationException(
                    "VOTE_SELF_TEAM_FORBIDDEN", "Impossible de voter pour la reponse de sa propre equipe.");
        }
        if (voteRepository.existsByQuestionIdAndVoterParticipantId(questionId, participantId)) {
            throw new BusinessRuleViolationException(
                    "VOTE_ALREADY_CAST", "Ce participant a deja vote pour cette question.");
        }

        Vote vote = new Vote(question, answer, membership.getParticipant());
        return voteRepository.save(vote);
    }

    private Question requireClosedQuestion(UUID questionId) {
        Question question =
                questionRepository.findById(questionId).orElseThrow(() -> new NotFoundException("Question introuvable."));
        if (question.getStatus() != QuestionStatus.CLOSED) {
            throw new BusinessRuleViolationException(
                    "QUESTION_NOT_CLOSED", "Le vote n'est ouvert qu'une fois les reponses fermees.");
        }
        return question;
    }

    private TeamMember requireTeamMembership(UUID participantId) {
        return teamMemberRepository
                .findByParticipantId(participantId)
                .orElseThrow(() -> new NotFoundException("Ce participant n'appartient a aucune equipe."));
    }
}
