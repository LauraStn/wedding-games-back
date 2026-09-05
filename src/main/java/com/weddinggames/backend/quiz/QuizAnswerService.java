package com.weddinggames.backend.quiz;

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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One answer per team per question, editable live by whichever team member currently holds the
 * pen. "Prise de main" is also how control is transferred: calling it again from a different team
 * member simply hands the pen to them, keeping whatever content was already typed.
 */
@Service
public class QuizAnswerService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final Clock clock;

    public QuizAnswerService(
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            TeamMemberRepository teamMemberRepository,
            Clock clock) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Answer getMyTeamAnswer(UUID questionId, UUID participantId) {
        Team team = requireTeam(participantId);
        return answerRepository
                .findByQuestionIdAndTeamId(questionId, team.getId())
                .orElseThrow(() -> new NotFoundException("Aucune reponse commencee pour cette question."));
    }

    @Transactional
    public Answer takeControl(UUID questionId, UUID participantId) {
        Question question = requireActiveQuestion(questionId);
        TeamMember membership = requireTeamMembership(participantId);
        Team team = membership.getTeam();

        Answer answer = answerRepository
                .findByQuestionIdAndTeamId(questionId, team.getId())
                .orElseGet(() -> new Answer(question, team, "", null));
        answer.setControllingParticipant(membership.getParticipant());
        return answerRepository.save(answer);
    }

    @Transactional
    public Answer updateContent(UUID questionId, UUID participantId, String content) {
        requireActiveQuestion(questionId);
        Team team = requireTeam(participantId);
        Answer answer = answerRepository
                .findByQuestionIdAndTeamId(questionId, team.getId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "ANSWER_NOT_IN_CONTROL", "Personne dans l'equipe n'a encore pris la main pour cette question."));

        Participant controlling = answer.getControllingParticipant();
        if (controlling == null || !controlling.getId().equals(participantId)) {
            throw new BusinessRuleViolationException(
                    "ANSWER_NOT_IN_CONTROL", "Un autre membre de l'equipe a la main actuellement.");
        }

        answer.setContent(content);
        answer.setSubmittedAt(Instant.now(clock));
        return answer;
    }

    private Question requireActiveQuestion(UUID questionId) {
        Question question =
                questionRepository.findById(questionId).orElseThrow(() -> new NotFoundException("Question introuvable."));
        if (question.getStatus() != QuestionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "QUESTION_NOT_ACTIVE", "Cette question n'est pas active: reponse impossible.");
        }
        return question;
    }

    private TeamMember requireTeamMembership(UUID participantId) {
        return teamMemberRepository
                .findByParticipantId(participantId)
                .orElseThrow(() -> new NotFoundException("Ce participant n'appartient a aucune equipe."));
    }

    private Team requireTeam(UUID participantId) {
        return requireTeamMembership(participantId).getTeam();
    }
}
