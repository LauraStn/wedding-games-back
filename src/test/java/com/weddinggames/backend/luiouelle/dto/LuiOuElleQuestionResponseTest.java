package com.weddinggames.backend.luiouelle.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.luiouelle.LuiOuElleQuestion;
import com.weddinggames.backend.participant.Participant;
import org.junit.jupiter.api.Test;

/** Pure unit test for the author-name reveal gating on the "public" vs staff response factories. */
class LuiOuElleQuestionResponseTest {

    private LuiOuElleQuestion questionWithConsent(boolean revealAuthorConsent) {
        Participant author = mock(Participant.class);
        when(author.getDisplayName()).thenReturn("Alice");
        return new LuiOuElleQuestion(mock(WeddingEvent.class), author, "Qui est le plus radin ?", revealAuthorConsent);
    }

    @Test
    void hidesTheAuthorNameByDefaultWhenConsentWasNotGiven() {
        LuiOuElleQuestionResponse response = LuiOuElleQuestionResponse.from(questionWithConsent(false));
        assertThat(response.authorDisplayName()).isNull();
    }

    @Test
    void revealsTheAuthorNameByDefaultWhenConsentWasGiven() {
        LuiOuElleQuestionResponse response = LuiOuElleQuestionResponse.from(questionWithConsent(true));
        assertThat(response.authorDisplayName()).isEqualTo("Alice");
    }

    @Test
    void staffAlwaysSeesTheAuthorNameRegardlessOfConsent() {
        LuiOuElleQuestionResponse response = LuiOuElleQuestionResponse.forStaff(questionWithConsent(false));
        assertThat(response.authorDisplayName()).isEqualTo("Alice");
    }

    @Test
    void theConsentFlagItselfIsAlwaysExposedEvenWhenTheNameIsHidden() {
        LuiOuElleQuestionResponse response = LuiOuElleQuestionResponse.from(questionWithConsent(false));
        assertThat(response.revealAuthorConsent()).isFalse();
    }
}
