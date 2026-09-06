package com.weddinggames.backend.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.staff.StaffAccount;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure unit test for the claim/release invariants of the control lock. */
class GameControlLockTest {

    private StaffAccount staff(String displayName) {
        StaffAccount account = mock(StaffAccount.class);
        when(account.getId()).thenReturn(UUID.randomUUID());
        when(account.getDisplayName()).thenReturn(displayName);
        return account;
    }

    private GameControlLock newLock() {
        return new GameControlLock(mock(Game.class));
    }

    @Test
    void isUnclaimedInitially() {
        assertThat(newLock().getHolder()).isNull();
    }

    @Test
    void claimsAnUnclaimedLock() {
        GameControlLock lock = newLock();
        StaffAccount alice = staff("Alice");
        Instant now = Instant.now();

        lock.claim(alice, now);

        assertThat(lock.getHolder()).isEqualTo(alice);
        assertThat(lock.getClaimedAt()).isEqualTo(now);
    }

    @Test
    void reclaimingByTheSameHolderIsANoOp() {
        GameControlLock lock = newLock();
        StaffAccount alice = staff("Alice");
        lock.claim(alice, Instant.now());
        Instant later = Instant.now().plusSeconds(60);

        lock.claim(alice, later);

        assertThat(lock.getClaimedAt()).isEqualTo(later);
    }

    @Test
    void rejectsClaimingWhenHeldBySomeoneElse() {
        GameControlLock lock = newLock();
        lock.claim(staff("Alice"), Instant.now());

        assertThatThrownBy(() -> lock.claim(staff("Bob"), Instant.now()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void theHolderCanReleaseTheirOwnLock() {
        GameControlLock lock = newLock();
        StaffAccount alice = staff("Alice");
        lock.claim(alice, Instant.now());

        lock.release(alice, false);

        assertThat(lock.getHolder()).isNull();
        assertThat(lock.getClaimedAt()).isNull();
    }

    @Test
    void releasingAnUnclaimedLockIsANoOp() {
        GameControlLock lock = newLock();

        lock.release(staff("Alice"), false);

        assertThat(lock.getHolder()).isNull();
    }

    @Test
    void rejectsReleasingSomeoneElsesLockWhenNotAdmin() {
        GameControlLock lock = newLock();
        lock.claim(staff("Alice"), Instant.now());

        assertThatThrownBy(() -> lock.release(staff("Bob"), false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void anAdminCanForceReleaseSomeoneElsesLock() {
        GameControlLock lock = newLock();
        lock.claim(staff("Alice"), Instant.now());

        lock.release(staff("Bob"), true);

        assertThat(lock.getHolder()).isNull();
    }
}
