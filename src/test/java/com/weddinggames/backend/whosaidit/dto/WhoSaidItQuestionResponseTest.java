package com.weddinggames.backend.whosaidit.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.whosaidit.WhoSaidItQuestion;
import com.weddinggames.backend.participant.Participant;
import org.junit.jupiter.api.Test;

/** Pure unit test for the author-name reveal gating on the "public" vs staff response factories. */
class WhoSaidItQuestionResponseTest {

    private WhoSaidItQuestion questionWithConsent(boolean revealAuthorConsent) {
        Participant author = mock(Participant.class);
        when(author.getDisplayName()).thenReturn("Alice");
        return new WhoSaidItQuestion(mock(WeddingEvent.class), author, "Qui est le plus radin ?", revealAuthorConsent);
    }

    @Test
    void hidesTheAuthorNameByDefaultWhenConsentWasNotGiven() {
        WhoSaidItQuestionResponse response = WhoSaidItQuestionResponse.from(questionWithConsent(false));
        assertThat(response.authorDisplayName()).isNull();
    }

    @Test
    void revealsTheAuthorNameByDefaultWhenConsentWasGiven() {
        WhoSaidItQuestionResponse response = WhoSaidItQuestionResponse.from(questionWithConsent(true));
        assertThat(response.authorDisplayName()).isEqualTo("Alice");
    }

    @Test
    void staffAlwaysSeesTheAuthorNameRegardlessOfConsent() {
        WhoSaidItQuestionResponse response = WhoSaidItQuestionResponse.forStaff(questionWithConsent(false));
        assertThat(response.authorDisplayName()).isEqualTo("Alice");
    }

    @Test
    void theConsentFlagItselfIsAlwaysExposedEvenWhenTheNameIsHidden() {
        WhoSaidItQuestionResponse response = WhoSaidItQuestionResponse.from(questionWithConsent(false));
        assertThat(response.revealAuthorConsent()).isFalse();
    }
}
