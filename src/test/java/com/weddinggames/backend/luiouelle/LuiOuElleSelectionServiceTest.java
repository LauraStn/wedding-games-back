package com.weddinggames.backend.luiouelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the random-draw-and-mark-played selection. */
class LuiOuElleSelectionServiceTest {

    private LuiOuElleQuestionRepository questionRepository;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        questionRepository = mock(LuiOuElleQuestionRepository.class);
        eventId = UUID.randomUUID();
    }

    private LuiOuElleQuestion acceptedQuestion(String content) {
        LuiOuElleQuestion question =
                new LuiOuElleQuestion(mock(WeddingEvent.class), mock(Participant.class), content);
        question.accept();
        return question;
    }

    @Test
    void selectsTheOnlyAcceptedQuestionAndMarksItPlayed() {
        LuiOuElleQuestion only = acceptedQuestion("Qui est le plus retard ?");
        when(questionRepository.findByEventIdAndStatus(eventId, LuiOuElleQuestionStatus.ACCEPTED))
                .thenReturn(List.of(only));
        LuiOuElleSelectionService service = new LuiOuElleSelectionService(questionRepository, new Random());

        LuiOuElleQuestion selected = service.selectRandom(eventId);

        assertThat(selected).isSameAs(only);
        assertThat(selected.getStatus()).isEqualTo(LuiOuElleQuestionStatus.PLAYED);
    }

    @Test
    void picksTheIndexReturnedByTheInjectedRandomSource() {
        LuiOuElleQuestion first = acceptedQuestion("Premiere");
        LuiOuElleQuestion second = acceptedQuestion("Deuxieme");
        LuiOuElleQuestion third = acceptedQuestion("Troisieme");
        when(questionRepository.findByEventIdAndStatus(eventId, LuiOuElleQuestionStatus.ACCEPTED))
                .thenReturn(List.of(first, second, third));
        Random fixedRandom = mock(Random.class);
        when(fixedRandom.nextInt(3)).thenReturn(1);
        LuiOuElleSelectionService service = new LuiOuElleSelectionService(questionRepository, fixedRandom);

        LuiOuElleQuestion selected = service.selectRandom(eventId);

        assertThat(selected).isSameAs(second);
        assertThat(first.getStatus()).isEqualTo(LuiOuElleQuestionStatus.ACCEPTED);
        assertThat(third.getStatus()).isEqualTo(LuiOuElleQuestionStatus.ACCEPTED);
    }

    @Test
    void rejectsSelectionWhenNoQuestionIsAccepted() {
        when(questionRepository.findByEventIdAndStatus(eventId, LuiOuElleQuestionStatus.ACCEPTED))
                .thenReturn(List.of());
        LuiOuElleSelectionService service = new LuiOuElleSelectionService(questionRepository, new Random());

        assertThatThrownBy(() -> service.selectRandom(eventId)).isInstanceOf(BusinessRuleViolationException.class);
    }
}
