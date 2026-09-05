package com.weddinggames.backend.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure unit test (no Spring context) for the lobby's guarded status transitions. */
class LobbyTest {

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    private Lobby newLobby() {
        return new Lobby(mock(WeddingEvent.class));
    }

    @Test
    void followsTheFullHappyPathFromClosedToFinished() {
        Lobby lobby = newLobby();

        lobby.open(now);
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.OPEN);

        lobby.lock();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.LOCKED);

        lobby.start();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.ACTIVE);

        lobby.pause();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.PAUSED);

        lobby.resume();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.ACTIVE);

        lobby.finish();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.FINISHED);
    }

    @Test
    void allowsReopeningReclosingAndRelockingAsHarmlessNoOps() {
        Lobby lobby = newLobby();
        lobby.open(now);

        lobby.open(now);
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.OPEN);

        lobby.lock();
        lobby.lock();
        assertThat(lobby.getStatus()).isEqualTo(LobbyStatus.LOCKED);
    }

    @Test
    void cannotStartASessionWithoutLockingFirst() {
        Lobby lobby = newLobby();
        lobby.open(now);

        assertThatThrownBy(lobby::start).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotPauseALobbyThatIsNotActive() {
        Lobby lobby = newLobby();

        assertThatThrownBy(lobby::pause).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotResumeALobbyThatIsNotPaused() {
        Lobby lobby = newLobby();
        lobby.open(now);
        lobby.lock();
        lobby.start();

        assertThatThrownBy(lobby::resume).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void finishedIsATerminalStateWithNoWayOut() {
        Lobby lobby = newLobby();
        lobby.open(now);
        lobby.lock();
        lobby.start();
        lobby.finish();

        assertThatThrownBy(lobby::start).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(lobby::pause).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> lobby.open(now)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotCloseALobbyThatIsAlreadyActive() {
        Lobby lobby = newLobby();
        lobby.open(now);
        lobby.lock();
        lobby.start();

        assertThatThrownBy(() -> lobby.close(now)).isInstanceOf(BusinessRuleViolationException.class);
    }
}
