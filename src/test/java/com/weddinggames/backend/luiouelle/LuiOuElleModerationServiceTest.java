package com.weddinggames.backend.luiouelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for staff moderation of Lui ou Elle questions. */
class LuiOuElleModerationServiceTest {

    private LuiOuElleQuestionRepository questionRepository;
    private LuiOuElleModerationService service;
    private UUID questionId;
    private LuiOuElleQuestion question;

    @BeforeEach
    void setUp() {
        questionRepository = mock(LuiOuElleQuestionRepository.class);
        service = new LuiOuElleModerationService(questionRepository);
        questionId = UUID.randomUUID();
        question = new LuiOuElleQuestion(mock(WeddingEvent.class), mock(Participant.class), "Qui est le plus radin ?");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
    }

    @Test
    void acceptsAPendingQuestion() {
        LuiOuElleQuestion accepted = service.accept(questionId);
        assertThat(accepted.getStatus()).isEqualTo(LuiOuElleQuestionStatus.ACCEPTED);
    }

    @Test
    void rejectsAPendingQuestion() {
        LuiOuElleQuestion rejected = service.reject(questionId);
        assertThat(rejected.getStatus()).isEqualTo(LuiOuElleQuestionStatus.REJECTED);
    }

    @Test
    void correctsTheContentOfAQuestion() {
        LuiOuElleQuestion corrected = service.correct(questionId, "Texte corrige");
        assertThat(corrected.getContent()).isEqualTo("Texte corrige");
    }

    @Test
    void rejectsActionsOnAnUnknownQuestion() {
        UUID unknownId = UUID.randomUUID();
        when(questionRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(unknownId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.reject(unknownId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.correct(unknownId, "x")).isInstanceOf(NotFoundException.class);
    }
}
