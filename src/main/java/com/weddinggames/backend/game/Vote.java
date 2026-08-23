package com.weddinggames.backend.game;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A participant's public vote for an answer to a question. At most one vote per participant per question. */
@Entity
@Table(name = "vote")
public class Vote extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "voter_participant_id", nullable = false)
    private Participant voterParticipant;

    protected Vote() {}

    public Vote(Question question, Answer answer, Participant voterParticipant) {
        this.question = question;
        this.answer = answer;
        this.voterParticipant = voterParticipant;
    }

    public Question getQuestion() {
        return question;
    }

    public Answer getAnswer() {
        return answer;
    }

    public Participant getVoterParticipant() {
        return voterParticipant;
    }
}
