package com.weddinggames.backend.control;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import com.weddinggames.backend.staff.StaffAccount;
import com.weddinggames.backend.staff.StaffAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameControlLockService {

    private final GameControlLockRepository lockRepository;
    private final GameRepository gameRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final Clock clock;

    public GameControlLockService(
            GameControlLockRepository lockRepository,
            GameRepository gameRepository,
            StaffAccountRepository staffAccountRepository,
            Clock clock) {
        this.lockRepository = lockRepository;
        this.gameRepository = gameRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.clock = clock;
    }

    @Transactional
    public GameControlLock getOrCreate(UUID gameId) {
        return lockRepository.findByGameId(gameId).orElseGet(() -> {
            Game game =
                    gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Partie introuvable."));
            return lockRepository.save(new GameControlLock(game));
        });
    }

    @Transactional
    public GameControlLock claim(UUID gameId, UUID staffAccountId) {
        GameControlLock lock = getOrCreate(gameId);
        StaffAccount claimant = staffAccount(staffAccountId);
        lock.claim(claimant, Instant.now(clock));
        return lock;
    }

    @Transactional
    public GameControlLock release(UUID gameId, UUID staffAccountId, boolean isAdmin) {
        GameControlLock lock = getOrCreate(gameId);
        StaffAccount releaser = staffAccount(staffAccountId);
        lock.release(releaser, isAdmin);
        return lock;
    }

    private StaffAccount staffAccount(UUID staffAccountId) {
        return staffAccountRepository
                .findById(staffAccountId)
                .orElseThrow(() -> new NotFoundException("Compte staff introuvable."));
    }
}
