package com.weddinggames.backend.whosaidit;

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
 * A "Who Said It" question proposed by a guest about the couple, while the lobby is still open.
 *
 * <p>Lifecycle: PENDING -&gt; ACCEPTED/REJECTED (either can be reconsidered into the other) -&gt;
 * PLAYED once selected and used during play. PLAYED is terminal: a played question is never
 * un-played, so its content/status can no longer change.
 */
@Entity
@Table(name = "who_said_it_question")
public class WhoSaidItQuestion extends BaseEntity {

    private static final Map<WhoSaidItQuestionStatus, Set<WhoSaidItQuestionStatus>> ALLOWED_PREDECESSORS = Map.of(
            WhoSaidItQuestionStatus.ACCEPTED,
                    Set.of(
                            WhoSaidItQuestionStatus.PENDING,
                            WhoSaidItQuestionStatus.REJECTED,
                            WhoSaidItQuestionStatus.ACCEPTED),
            WhoSaidItQuestionStatus.REJECTED,
                    Set.of(
                            WhoSaidItQuestionStatus.PENDING,
                            WhoSaidItQuestionStatus.ACCEPTED,
                            WhoSaidItQuestionStatus.REJECTED),
            WhoSaidItQuestionStatus.PLAYED, Set.of(WhoSaidItQuestionStatus.ACCEPTED));

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
    private WhoSaidItQuestionStatus status = WhoSaidItQuestionStatus.PENDING;

    @Column(name = "reveal_author_consent", nullable = false)
    private boolean revealAuthorConsent;

    protected WhoSaidItQuestion() {}

    public WhoSaidItQuestion(WeddingEvent event, Participant author, String content) {
        this(event, author, content, false);
    }

    public WhoSaidItQuestion(WeddingEvent event, Participant author, String content, boolean revealAuthorConsent) {
        this.event = event;
        this.author = author;
        this.content = content;
        this.revealAuthorConsent = revealAuthorConsent;
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

    public WhoSaidItQuestionStatus getStatus() {
        return status;
    }

    /** Whether the author consented to their first name being revealed alongside this question. */
    public boolean isRevealAuthorConsent() {
        return revealAuthorConsent;
    }

    /** The guest author edits their own proposal: blocked once played, and re-queued for review. */
    public void reviseByAuthor(String newContent, boolean revealAuthorConsent) {
        requireNotPlayed();
        this.content = newContent;
        this.revealAuthorConsent = revealAuthorConsent;
        if (status != WhoSaidItQuestionStatus.PENDING) {
            status = WhoSaidItQuestionStatus.PENDING;
        }
    }

    /** Staff fixing a typo/garbled text: blocked once played, but does not reset the moderation status. */
    public void correct(String newContent) {
        requireNotPlayed();
        this.content = newContent;
    }

    public void accept() {
        transitionTo(WhoSaidItQuestionStatus.ACCEPTED);
    }

    public void reject() {
        transitionTo(WhoSaidItQuestionStatus.REJECTED);
    }

    public void markPlayed() {
        transitionTo(WhoSaidItQuestionStatus.PLAYED);
    }

    private void requireNotPlayed() {
        if (status == WhoSaidItQuestionStatus.PLAYED) {
            throw new BusinessRuleViolationException(
                    "WHO_SAID_IT_QUESTION_ALREADY_PLAYED", "Cette question a deja ete jouee: elle n'est plus modifiable.");
        }
    }

    private void transitionTo(WhoSaidItQuestionStatus target) {
        Set<WhoSaidItQuestionStatus> allowedFrom = ALLOWED_PREDECESSORS.get(target);
        if (allowedFrom == null || !allowedFrom.contains(status)) {
            throw new BusinessRuleViolationException(
                    "INVALID_WHO_SAID_IT_QUESTION_TRANSITION",
                    "Impossible de passer de " + status + " a " + target + ".");
        }
        status = target;
    }
}
