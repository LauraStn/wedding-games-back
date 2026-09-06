package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.blindtest.dto.BlindTestFormatRequest;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Round format configuration (duration, points) for a blind test game: one per game, lazily created. */
@Service
public class BlindTestFormatService {

    private final BlindTestFormatRepository formatRepository;
    private final GameRepository gameRepository;

    public BlindTestFormatService(BlindTestFormatRepository formatRepository, GameRepository gameRepository) {
        this.formatRepository = formatRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional
    public BlindTestFormat getOrCreate(UUID gameId) {
        return formatRepository.findByGameId(gameId).orElseGet(() -> {
            Game game =
                    gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Partie introuvable."));
            return formatRepository.save(new BlindTestFormat(game));
        });
    }

    @Transactional
    public BlindTestFormat update(UUID gameId, BlindTestFormatRequest request) {
        BlindTestFormat format = getOrCreate(gameId);
        format.setRoundDurationSeconds(request.roundDurationSeconds());
        format.setPointsPerCorrectGuess(request.pointsPerCorrectGuess());
        return format;
    }
}
