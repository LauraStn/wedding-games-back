package com.weddinggames.backend.blindtest;

import com.weddinggames.backend.blindtest.dto.TrackCreateRequest;
import com.weddinggames.backend.blindtest.dto.TrackUpdateRequest;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackAdminService {

    private final TrackRepository trackRepository;
    private final GameRepository gameRepository;

    public TrackAdminService(TrackRepository trackRepository, GameRepository gameRepository) {
        this.trackRepository = trackRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<Track> listByGame(UUID gameId) {
        return trackRepository.findByGameIdOrderBySequence(gameId);
    }

    @Transactional(readOnly = true)
    public Track get(UUID id) {
        return trackRepository.findById(id).orElseThrow(() -> new NotFoundException("Morceau introuvable."));
    }

    @Transactional
    public Track create(UUID gameId, TrackCreateRequest request) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Partie introuvable."));
        Track track = new Track(game, request.title(), request.artist(), request.variant(), request.sequence());
        return trackRepository.save(track);
    }

    @Transactional
    public Track update(UUID id, TrackUpdateRequest request) {
        Track track = get(id);
        track.setTitle(request.title());
        track.setArtist(request.artist());
        track.setVariant(request.variant());
        track.setSequence(request.sequence());
        return track;
    }

    @Transactional
    public void delete(UUID id) {
        if (!trackRepository.existsById(id)) {
            throw new NotFoundException("Morceau introuvable.");
        }
        trackRepository.deleteById(id);
    }
}
