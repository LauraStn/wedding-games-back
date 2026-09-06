package com.weddinggames.backend.control;

import com.weddinggames.backend.common.BaseEntity;
import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.staff.StaffAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Claim/release lock so only one intervenant pilots a given game at a time, even when several are
 * connected. One per game, lazily created on first access (unclaimed).
 */
@Entity
@Table(name = "game_control_lock")
public class GameControlLock extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "holder_id")
    private StaffAccount holder;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected GameControlLock() {}

    public GameControlLock(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public StaffAccount getHolder() {
        return holder;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    /** Claims the lock: a no-op if the same staff already holds it, refused if held by another. */
    public void claim(StaffAccount claimant, Instant now) {
        if (holder != null && !holder.getId().equals(claimant.getId())) {
            throw new BusinessRuleViolationException(
                    "GAME_CONTROL_LOCKED",
                    "Cette partie est deja pilotee par " + holder.getDisplayName() + ".");
        }
        holder = claimant;
        claimedAt = now;
    }

    /**
     * Releases the lock: a no-op if already unclaimed. Only the current holder may release it,
     * unless {@code isAdmin} is true (an admin can always free a stuck lock, e.g. after a staff
     * member's session drops without releasing it).
     */
    public void release(StaffAccount releaser, boolean isAdmin) {
        if (holder == null) {
            return;
        }
        if (!holder.getId().equals(releaser.getId()) && !isAdmin) {
            throw new BusinessRuleViolationException(
                    "GAME_CONTROL_NOT_HELD_BY_YOU", "Seul " + holder.getDisplayName() + " peut relacher ce controle.");
        }
        holder = null;
        claimedAt = null;
    }
}
