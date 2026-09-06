package com.weddinggames.backend.blindtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.blindtest.dto.TrackCreateRequest;
import com.weddinggames.backend.blindtest.dto.TrackUpdateRequest;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.Game;
import com.weddinggames.backend.game.GameRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the track catalog CRUD. */
class TrackAdminServiceTest {

    private TrackRepository trackRepository;
    private GameRepository gameRepository;
    private TrackAdminService service;
    private UUID gameId;
    private Game game;

    @BeforeEach
    void setUp() {
        trackRepository = mock(TrackRepository.class);
        gameRepository = mock(GameRepository.class);
        service = new TrackAdminService(trackRepository, gameRepository);
        gameId = UUID.randomUUID();
        game = mock(Game.class);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsATrackForAnExistingGame() {
        Track created = service.create(gameId, new TrackCreateRequest("Freed from Desire", "Gala", BlindTestVariant.REVERSED, 0));

        assertThat(created.getTitle()).isEqualTo("Freed from Desire");
        assertThat(created.getArtist()).isEqualTo("Gala");
        assertThat(created.getVariant()).isEqualTo(BlindTestVariant.REVERSED);
    }

    @Test
    void rejectsCreationForAnUnknownGame() {
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        gameId, new TrackCreateRequest("Titre", "Artiste", BlindTestVariant.SLOWED_DOWN, 0)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesAnExistingTrack() {
        UUID trackId = UUID.randomUUID();
        Track existing = new Track(game, "Ancien titre", "Ancien artiste", BlindTestVariant.SLOWED_DOWN, 0);
        when(trackRepository.findById(trackId)).thenReturn(Optional.of(existing));

        Track updated = service.update(
                trackId, new TrackUpdateRequest("Nouveau titre", "Nouvel artiste", BlindTestVariant.LYRICS_CONTINUATION, 1));

        assertThat(updated.getTitle()).isEqualTo("Nouveau titre");
        assertThat(updated.getArtist()).isEqualTo("Nouvel artiste");
        assertThat(updated.getVariant()).isEqualTo(BlindTestVariant.LYRICS_CONTINUATION);
        assertThat(updated.getSequence()).isEqualTo(1);
    }

    @Test
    void rejectsUpdatingAnUnknownTrack() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.findById(trackId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        trackId, new TrackUpdateRequest("Titre", "Artiste", BlindTestVariant.REVERSED, 0)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletesAnExistingTrack() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.existsById(trackId)).thenReturn(true);

        service.delete(trackId);

        org.mockito.Mockito.verify(trackRepository).deleteById(trackId);
    }

    @Test
    void rejectsDeletingAnUnknownTrack() {
        UUID trackId = UUID.randomUUID();
        when(trackRepository.existsById(trackId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(trackId)).isInstanceOf(NotFoundException.class);
    }
}
