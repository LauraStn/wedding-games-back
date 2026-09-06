package com.weddinggames.backend.luiouelle;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.Set;

/**
 * A "Lui ou Elle" question proposed by a guest about the couple, while the lobby is still open.
 *
 * <p>Lifecycle: PENDING -&gt; ACCEPTED/REJECTED (either can be reconsidered into the other) -&gt;
 * PLAYED once selected and used during play. PLAYED is terminal: a played question is never
 * un-played, so its content/status can no longer change.
 */
@Entity
@Table(name = "lui_ou_elle_question")
public class LuiOuElleQuestion extends BaseEntity {

    private static final Map<LuiOuElleQuestionStatus, Set<LuiOuElleQuestionStatus>> ALLOWED_PREDECESSORS = Map.of(
            LuiOuElleQuestionStatus.ACCEPTED,
                    Set.of(
                            LuiOuElleQuestionStatus.PENDING,
                            LuiOuElleQuestionStatus.REJECTED,
                            LuiOuElleQuestionStatus.ACCEPTED),
            LuiOuElleQuestionStatus.REJECTED,
                    Set.of(
                            LuiOuElleQuestionStatus.PENDING,
                            LuiOuElleQuestionStatus.ACCEPTED,
                            LuiOuElleQuestionStatus.REJECTED),
            LuiOuElleQuestionStatus.PLAYED, Set.of(LuiOuElleQuestionStatus.ACCEPTED));

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Participant author;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LuiOuElleQuestionStatus status = LuiOuElleQuestionStatus.PENDING;

    protected LuiOuElleQuestion() {}

    public LuiOuElleQuestion(WeddingEvent event, Participant author, String content) {
        this.event = event;
        this.author = author;
        this.content = content;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public Participant getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public LuiOuElleQuestionStatus getStatus() {
        return status;
    }

    /** The guest author edits their own proposal: blocked once played, and re-queued for review. */
    public void reviseByAuthor(String newContent) {
        requireNotPlayed();
        this.content = newContent;
        if (status != LuiOuElleQuestionStatus.PENDING) {
            status = LuiOuElleQuestionStatus.PENDING;
        }
    }

    /** Staff fixing a typo/garbled text: blocked once played, but does not reset the moderation status. */
    public void correct(String newContent) {
        requireNotPlayed();
        this.content = newContent;
    }

    public void accept() {
        transitionTo(LuiOuElleQuestionStatus.ACCEPTED);
    }

    public void reject() {
        transitionTo(LuiOuElleQuestionStatus.REJECTED);
    }

    public void markPlayed() {
        transitionTo(LuiOuElleQuestionStatus.PLAYED);
    }

    private void requireNotPlayed() {
        if (status == LuiOuElleQuestionStatus.PLAYED) {
            throw new BusinessRuleViolationException(
                    "LUI_OU_ELLE_QUESTION_ALREADY_PLAYED", "Cette question a deja ete jouee: elle n'est plus modifiable.");
        }
    }

    private void transitionTo(LuiOuElleQuestionStatus target) {
        Set<LuiOuElleQuestionStatus> allowedFrom = ALLOWED_PREDECESSORS.get(target);
        if (allowedFrom == null || !allowedFrom.contains(status)) {
            throw new BusinessRuleViolationException(
                    "INVALID_LUI_OU_ELLE_QUESTION_TRANSITION",
                    "Impossible de passer de " + status + " a " + target + ".");
        }
        status = target;
    }
}
