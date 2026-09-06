package com.weddinggames.backend.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the control lock lifecycle. */
class GameControlLockServiceTest {

    private GameControlLockRepository lockRepository;
    private GameRepository gameRepository;
    private StaffAccountRepository staffAccountRepository;
    private GameControlLockService service;
    private UUID gameId;
    private Game game;

    @BeforeEach
    void setUp() {
        lockRepository = mock(GameControlLockRepository.class);
        gameRepository = mock(GameRepository.class);
        staffAccountRepository = mock(StaffAccountRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T20:00:00Z"), ZoneOffset.UTC);
        service = new GameControlLockService(lockRepository, gameRepository, staffAccountRepository, clock);
        gameId = UUID.randomUUID();
        game = mock(Game.class);
        when(lockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private StaffAccount staff(UUID id) {
        StaffAccount account = mock(StaffAccount.class);
        when(account.getId()).thenReturn(id);
        when(staffAccountRepository.findById(id)).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void createsAnUnclaimedLockOnFirstAccess() {
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.empty());
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        GameControlLock lock = service.getOrCreate(gameId);

        assertThat(lock.getHolder()).isNull();
    }

    @Test
    void rejectsCreatingALockForAnUnknownGame() {
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.empty());
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreate(gameId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void claimsTheLockForAKnownStaffAccount() {
        GameControlLock lock = new GameControlLock(game);
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.of(lock));
        UUID aliceId = UUID.randomUUID();
        staff(aliceId);

        GameControlLock claimed = service.claim(gameId, aliceId);

        assertThat(claimed.getHolder().getId()).isEqualTo(aliceId);
    }

    @Test
    void rejectsClaimingWhenAlreadyHeldBySomeoneElse() {
        GameControlLock lock = new GameControlLock(game);
        UUID aliceId = UUID.randomUUID();
        lock.claim(staff(aliceId), Instant.now());
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.of(lock));
        UUID bobId = UUID.randomUUID();
        staff(bobId);

        assertThatThrownBy(() -> service.claim(gameId, bobId)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void releasesTheLockAsTheHolder() {
        GameControlLock lock = new GameControlLock(game);
        UUID aliceId = UUID.randomUUID();
        lock.claim(staff(aliceId), Instant.now());
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.of(lock));

        GameControlLock released = service.release(gameId, aliceId, false);

        assertThat(released.getHolder()).isNull();
    }

    @Test
    void nonAdminCannotReleaseSomeoneElsesLock() {
        GameControlLock lock = new GameControlLock(game);
        UUID aliceId = UUID.randomUUID();
        lock.claim(staff(aliceId), Instant.now());
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.of(lock));
        UUID bobId = UUID.randomUUID();
        staff(bobId);

        assertThatThrownBy(() -> service.release(gameId, bobId, false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void adminCanForceReleaseSomeoneElsesLock() {
        GameControlLock lock = new GameControlLock(game);
        UUID aliceId = UUID.randomUUID();
        lock.claim(staff(aliceId), Instant.now());
        when(lockRepository.findByGameId(gameId)).thenReturn(Optional.of(lock));
        UUID adminId = UUID.randomUUID();
        staff(adminId);

        GameControlLock released = service.release(gameId, adminId, true);

        assertThat(released.getHolder()).isNull();
    }
}
