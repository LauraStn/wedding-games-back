package com.weddinggames.backend.game;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
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

/** A configured "jeu" (quiz, lui-ou-elle, blind test...) attached to an event. */
@Entity
@Table(name = "game")
public class Game extends BaseEntity {

    private static final Map<GameStatus, Set<GameStatus>> ALLOWED_STATUS_PREDECESSORS = Map.of(
            GameStatus.ACTIVE, Set.of(GameStatus.DRAFT, GameStatus.READY, GameStatus.PAUSED),
            GameStatus.PAUSED, Set.of(GameStatus.ACTIVE));

    /**
     * Only the entry points this ticket's actions actually drive (start, next question) are
     * listed here. Later tickets (answer moderation, vote, jury, podium) add their own predecessor
     * entries for ANSWERS_CLOSED/VOTE/JURY/RESULT when they implement those transitions.
     */
    private static final Map<GamePhase, Set<GamePhase>> ALLOWED_PHASE_PREDECESSORS = Map.of(
            GamePhase.PREPARATION, Set.of(GamePhase.LOBBY),
            GamePhase.QUESTION, Set.of(GamePhase.PREPARATION, GamePhase.RESULT));

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private int sequence = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GamePhase phase = GamePhase.LOBBY;

    protected Game() {}

    public Game(WeddingEvent event, GameType type, String title, int sequence) {
        this.event = event;
        this.type = type;
        this.title = title;
        this.sequence = sequence;
    }

    public void start() {
        transitionStatusTo(GameStatus.ACTIVE);
        transitionPhaseTo(GamePhase.PREPARATION);
    }

    public void pause() {
        transitionStatusTo(GameStatus.PAUSED);
    }

    public void resume() {
        transitionStatusTo(GameStatus.ACTIVE);
    }

    public void nextQuestion() {
        if (status != GameStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "GAME_NOT_ACTIVE", "Impossible de passer a la question suivante: la partie n'est pas active.");
        }
        transitionPhaseTo(GamePhase.QUESTION);
    }

    private void transitionStatusTo(GameStatus target) {
        if (!ALLOWED_STATUS_PREDECESSORS.get(target).contains(this.status)) {
            throw new BusinessRuleViolationException(
                    "INVALID_GAME_STATUS_TRANSITION",
                    "Transition de statut de jeu invalide: " + this.status + " -> " + target + ".");
        }
        this.status = target;
    }

    private void transitionPhaseTo(GamePhase target) {
        if (!ALLOWED_PHASE_PREDECESSORS.get(target).contains(this.phase)) {
            throw new BusinessRuleViolationException(
                    "INVALID_GAME_PHASE_TRANSITION",
                    "Transition de phase de jeu invalide: " + this.phase + " -> " + target + ".");
        }
        this.phase = target;
    }

    public WeddingEvent getEvent() {
        return event;
    }

    public GameType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public GameStatus getStatus() {
        return status;
    }

    public GamePhase getPhase() {
        return phase;
    }
}
