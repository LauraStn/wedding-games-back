package com.weddinggames.backend.game;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.participant.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A question asked within a game. May originate from the admin or from a moderated guest proposal. */
@Entity
@Table(name = "question")
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(nullable = false)
    private int sequence = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSource source = QuestionSource.ADMIN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_participant_id")
    private Participant authorParticipant;

    @Column(name = "reveal_author", nullable = false)
    private boolean revealAuthor = false;

    protected Question() {}

    public Question(Game game, String prompt, int sequence, QuestionSource source, Participant authorParticipant) {
        this.game = game;
        this.prompt = prompt;
        this.sequence = sequence;
        this.source = source;
        this.authorParticipant = authorParticipant;
    }

    public Game getGame() {
        return game;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public QuestionSource getSource() {
        return source;
    }

    public Participant getAuthorParticipant() {
        return authorParticipant;
    }

    public boolean isRevealAuthor() {
        return revealAuthor;
    }

    public void setRevealAuthor(boolean revealAuthor) {
        this.revealAuthor = revealAuthor;
    }
}
