package com.weddinggames.backend.whosaidit;

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
class WhoSaidItSelectionServiceTest {

    private WhoSaidItQuestionRepository questionRepository;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        questionRepository = mock(WhoSaidItQuestionRepository.class);
        eventId = UUID.randomUUID();
    }

    private WhoSaidItQuestion acceptedQuestion(String content) {
        WhoSaidItQuestion question =
                new WhoSaidItQuestion(mock(WeddingEvent.class), mock(Participant.class), content);
        question.accept();
        return question;
    }

    @Test
    void selectsTheOnlyAcceptedQuestionAndMarksItPlayed() {
        WhoSaidItQuestion only = acceptedQuestion("Qui est le plus retard ?");
        when(questionRepository.findByEventIdAndStatus(eventId, WhoSaidItQuestionStatus.ACCEPTED))
                .thenReturn(List.of(only));
        WhoSaidItSelectionService service = new WhoSaidItSelectionService(questionRepository, new Random());

        WhoSaidItQuestion selected = service.selectRandom(eventId);

        assertThat(selected).isSameAs(only);
        assertThat(selected.getStatus()).isEqualTo(WhoSaidItQuestionStatus.PLAYED);
    }

    @Test
    void picksTheIndexReturnedByTheInjectedRandomSource() {
        WhoSaidItQuestion first = acceptedQuestion("Premiere");
        WhoSaidItQuestion second = acceptedQuestion("Deuxieme");
        WhoSaidItQuestion third = acceptedQuestion("Troisieme");
        when(questionRepository.findByEventIdAndStatus(eventId, WhoSaidItQuestionStatus.ACCEPTED))
                .thenReturn(List.of(first, second, third));
        Random fixedRandom = mock(Random.class);
        when(fixedRandom.nextInt(3)).thenReturn(1);
        WhoSaidItSelectionService service = new WhoSaidItSelectionService(questionRepository, fixedRandom);

        WhoSaidItQuestion selected = service.selectRandom(eventId);

        assertThat(selected).isSameAs(second);
        assertThat(first.getStatus()).isEqualTo(WhoSaidItQuestionStatus.ACCEPTED);
        assertThat(third.getStatus()).isEqualTo(WhoSaidItQuestionStatus.ACCEPTED);
    }

    @Test
    void rejectsSelectionWhenNoQuestionIsAccepted() {
        when(questionRepository.findByEventIdAndStatus(eventId, WhoSaidItQuestionStatus.ACCEPTED))
                .thenReturn(List.of());
        WhoSaidItSelectionService service = new WhoSaidItSelectionService(questionRepository, new Random());

        assertThatThrownBy(() -> service.selectRandom(eventId)).isInstanceOf(BusinessRuleViolationException.class);
    }
}
