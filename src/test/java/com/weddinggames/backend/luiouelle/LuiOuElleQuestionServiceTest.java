package com.weddinggames.backend.luiouelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for proposing/editing Lui ou Elle questions. */
class LuiOuElleQuestionServiceTest {

    private LuiOuElleQuestionRepository questionRepository;
    private ParticipantRepository participantRepository;
    private LobbyRepository lobbyRepository;
    private LuiOuElleProperties properties;
    private LuiOuElleQuestionService service;
    private UUID eventId;
    private UUID participantId;
    private Participant author;

    @BeforeEach
    void setUp() {
        questionRepository = mock(LuiOuElleQuestionRepository.class);
        participantRepository = mock(ParticipantRepository.class);
        lobbyRepository = mock(LobbyRepository.class);
        properties = new LuiOuElleProperties();
        service = new LuiOuElleQuestionService(questionRepository, participantRepository, lobbyRepository, properties);

        eventId = UUID.randomUUID();
        participantId = UUID.randomUUID();
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        author = mock(Participant.class);
        when(author.getEvent()).thenReturn(event);
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(author));

        Lobby openLobby = new Lobby(event);
        openLobby.open(Instant.now());
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(openLobby));
        when(questionRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void proposesAQuestionWhenTheLobbyIsOpenAndUnderTheLimit() {
        when(questionRepository.countByAuthorId(participantId)).thenReturn(0L);

        LuiOuElleQuestion question = service.propose(participantId, "Qui est le plus bordelique ?", false);

        assertThat(question.getContent()).isEqualTo("Qui est le plus bordelique ?");
        assertThat(question.getAuthor()).isEqualTo(author);
    }

    @Test
    void trimsTheProposedContent() {
        when(questionRepository.countByAuthorId(participantId)).thenReturn(0L);

        LuiOuElleQuestion question = service.propose(participantId, "  Qui cuisine le mieux ?  ", false);

        assertThat(question.getContent()).isEqualTo("Qui cuisine le mieux ?");
    }

    @Test
    void carriesTheAuthorsRevealConsentThroughToThePersistedQuestion() {
        when(questionRepository.countByAuthorId(participantId)).thenReturn(0L);

        LuiOuElleQuestion question = service.propose(participantId, "Qui est le plus radin ?", true);

        assertThat(question.isRevealAuthorConsent()).isTrue();
    }

    @Test
    void updatingCanChangeTheRevealConsent() {
        UUID questionId = UUID.randomUUID();
        WeddingEvent event = author.getEvent();
        LuiOuElleQuestion existing = new LuiOuElleQuestion(event, author, "Ancienne question", false);
        when(questionRepository.findByIdAndAuthorId(questionId, participantId)).thenReturn(Optional.of(existing));

        LuiOuElleQuestion updated = service.update(participantId, questionId, "Nouvelle question", true);

        assertThat(updated.isRevealAuthorConsent()).isTrue();
    }

    @Test
    void rejectsProposingBeyondTheConfiguredLimit() {
        properties.setMaxQuestionsPerParticipant(2);
        when(questionRepository.countByAuthorId(participantId)).thenReturn(2L);

        assertThatThrownBy(() -> service.propose(participantId, "Une troisieme question ?", false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsProposingWhenTheLobbyIsNotOpen() {
        WeddingEvent event = mock(WeddingEvent.class);
        when(event.getId()).thenReturn(eventId);
        Lobby closedLobby = new Lobby(event);
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(closedLobby));

        assertThatThrownBy(() -> service.propose(participantId, "Une question ?", false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsProposingWhenNoLobbyExistsYet() {
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.propose(participantId, "Une question ?", false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void rejectsContentLongerThanTheConfiguredMax() {
        properties.setMaxContentLength(10);
        when(questionRepository.countByAuthorId(participantId)).thenReturn(0L);

        assertThatThrownBy(() -> service.propose(participantId, "Cette question est beaucoup trop longue", false))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsProposingForAnUnknownParticipant() {
        UUID unknownId = UUID.randomUUID();
        when(participantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.propose(unknownId, "Une question ?", false)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesMyOwnQuestionWhileTheLobbyIsOpen() {
        UUID questionId = UUID.randomUUID();
        WeddingEvent event = author.getEvent();
        LuiOuElleQuestion existing = new LuiOuElleQuestion(event, author, "Ancienne question");
        when(questionRepository.findByIdAndAuthorId(questionId, participantId)).thenReturn(Optional.of(existing));

        LuiOuElleQuestion updated = service.update(participantId, questionId, "Nouvelle question", false);

        assertThat(updated.getContent()).isEqualTo("Nouvelle question");
    }

    @Test
    void rejectsUpdatingSomeoneElsesOrAnUnknownQuestion() {
        UUID questionId = UUID.randomUUID();
        when(questionRepository.findByIdAndAuthorId(questionId, participantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(participantId, questionId, "Nouvelle question", false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void editingAnAlreadyModeratedQuestionResetsItToPendingForReReview() {
        UUID questionId = UUID.randomUUID();
        WeddingEvent event = author.getEvent();
        LuiOuElleQuestion existing = new LuiOuElleQuestion(event, author, "Ancienne question");
        existing.accept();
        when(questionRepository.findByIdAndAuthorId(questionId, participantId)).thenReturn(Optional.of(existing));

        LuiOuElleQuestion updated = service.update(participantId, questionId, "Nouvelle question", false);

        assertThat(updated.getStatus()).isEqualTo(LuiOuElleQuestionStatus.PENDING);
    }

    @Test
    void rejectsUpdatingOnceTheLobbyIsNoLongerOpen() {
        UUID questionId = UUID.randomUUID();
        WeddingEvent event = author.getEvent();
        LuiOuElleQuestion existing = new LuiOuElleQuestion(event, author, "Ancienne question");
        when(questionRepository.findByIdAndAuthorId(questionId, participantId)).thenReturn(Optional.of(existing));
        Lobby lockedLobby = new Lobby(event);
        lockedLobby.open(Instant.now());
        lockedLobby.lock();
        when(lobbyRepository.findByEventId(eventId)).thenReturn(Optional.of(lockedLobby));

        assertThatThrownBy(() -> service.update(participantId, questionId, "Nouvelle question", false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
