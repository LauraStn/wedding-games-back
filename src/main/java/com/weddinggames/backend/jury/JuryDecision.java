package com.weddinggames.backend.jury;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The jury's winner pick for a question: PENDING -&gt; CHOSEN (re-pickable) -&gt; CONFIRMED
 * (final, points awarded - see {@code ScoreService}, reused rather than duplicated here). Reveal
 * is a separate, later action gated on CONFIRMED: the jury can sit on a confirmed-but-unrevealed
 * decision for dramatic effect before announcing it live.
 */
@Entity
@Table(name = "jury_decision")
public class JuryDecision extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chosen_answer_id")
    private Answer chosenAnswer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JuryDecisionStatus status = JuryDecisionStatus.PENDING;

    @Column(nullable = false)
    private boolean revealed = false;

    protected JuryDecision() {}

    public JuryDecision(Question question) {
        this.question = question;
    }

    public Question getQuestion() {
        return question;
    }

    public Answer getChosenAnswer() {
        return chosenAnswer;
    }

    public JuryDecisionStatus getStatus() {
        return status;
    }

    public boolean isRevealed() {
        return revealed;
    }

    /** Picks (or re-picks, before confirming) the winning answer. */
    public void choose(Answer answer) {
        if (status == JuryDecisionStatus.CONFIRMED) {
            throw new BusinessRuleViolationException(
                    "JURY_DECISION_ALREADY_CONFIRMED", "La decision est deja confirmee, le choix ne peut plus changer.");
        }
        chosenAnswer = answer;
        status = JuryDecisionStatus.CHOSEN;
    }

    /** Confirms the current pick. Final: the choice can no longer change after this. */
    public void confirm() {
        if (status != JuryDecisionStatus.CHOSEN) {
            throw new BusinessRuleViolationException(
                    "JURY_DECISION_NOT_CHOSEN", "Il faut choisir une reponse avant de confirmer.");
        }
        status = JuryDecisionStatus.CONFIRMED;
    }

    /** Reveals the winning team. Only possible once the decision is confirmed. */
    public void reveal() {
        if (status != JuryDecisionStatus.CONFIRMED) {
            throw new BusinessRuleViolationException(
                    "JURY_DECISION_NOT_CONFIRMED", "Il faut confirmer la decision avant de la reveler.");
        }
        revealed = true;
    }
}
