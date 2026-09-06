package com.weddinggames.backend.whosaidit;

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

/** Pure unit test (Mockito, no Spring context) for staff moderation of Who Said It questions. */
class WhoSaidItModerationServiceTest {

    private WhoSaidItQuestionRepository questionRepository;
    private WhoSaidItModerationService service;
    private UUID questionId;
    private WhoSaidItQuestion question;

    @BeforeEach
    void setUp() {
        questionRepository = mock(WhoSaidItQuestionRepository.class);
        service = new WhoSaidItModerationService(questionRepository);
        questionId = UUID.randomUUID();
        question = new WhoSaidItQuestion(mock(WeddingEvent.class), mock(Participant.class), "Qui est le plus radin ?");
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
    }

    @Test
    void acceptsAPendingQuestion() {
        WhoSaidItQuestion accepted = service.accept(questionId);
        assertThat(accepted.getStatus()).isEqualTo(WhoSaidItQuestionStatus.ACCEPTED);
    }

    @Test
    void rejectsAPendingQuestion() {
        WhoSaidItQuestion rejected = service.reject(questionId);
        assertThat(rejected.getStatus()).isEqualTo(WhoSaidItQuestionStatus.REJECTED);
    }

    @Test
    void correctsTheContentOfAQuestion() {
        WhoSaidItQuestion corrected = service.correct(questionId, "Texte corrige");
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
