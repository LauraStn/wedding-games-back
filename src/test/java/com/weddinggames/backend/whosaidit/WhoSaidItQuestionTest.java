package com.weddinggames.backend.whosaidit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import org.junit.jupiter.api.Test;

/** Pure unit test for the guarded PENDING/ACCEPTED/REJECTED/PLAYED status transitions. */
class WhoSaidItQuestionTest {

    private WhoSaidItQuestion newQuestion() {
        return new WhoSaidItQuestion(mock(WeddingEvent.class), mock(Participant.class), "Qui est le plus retard ?");
    }

    @Test
    void startsAsPending() {
        assertThat(newQuestion().getStatus()).isEqualTo(WhoSaidItQuestionStatus.PENDING);
    }

    @Test
    void acceptsFromPending() {
        WhoSaidItQuestion question = newQuestion();
        question.accept();
        assertThat(question.getStatus()).isEqualTo(WhoSaidItQuestionStatus.ACCEPTED);
    }

    @Test
    void rejectsFromPending() {
        WhoSaidItQuestion question = newQuestion();
        question.reject();
        assertThat(question.getStatus()).isEqualTo(WhoSaidItQuestionStatus.REJECTED);
    }

    @Test
    void aRejectionCanBeReconsideredIntoAnAcceptance() {
        WhoSaidItQuestion question = newQuestion();
        question.reject();
        question.accept();
        assertThat(question.getStatus()).isEqualTo(WhoSaidItQuestionStatus.ACCEPTED);
    }

    @Test
    void anAcceptanceCanBeReconsideredIntoARejection() {
        WhoSaidItQuestion question = newQuestion();
        question.accept();
        question.reject();
        assertThat(question.getStatus()).isEqualTo(WhoSaidItQuestionStatus.REJECTED);
    }

    @Test
    void onlyAnAcceptedQuestionCanBeMarkedPlayed() {
        WhoSaidItQuestion pending = newQuestion();
        assertThatThrownBy(pending::markPlayed).isInstanceOf(BusinessRuleViolationException.class);

        WhoSaidItQuestion accepted = newQuestion();
        accepted.accept();
        accepted.markPlayed();
        assertThat(accepted.getStatus()).isEqualTo(WhoSaidItQuestionStatus.PLAYED);
    }

    @Test
    void playedIsTerminalAndCannotBeAcceptedOrRejectedAgain() {
        WhoSaidItQuestion question = newQuestion();
        question.accept();
        question.markPlayed();

        assertThatThrownBy(question::accept).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(question::reject).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotCorrectOrReviseAPlayedQuestion() {
        WhoSaidItQuestion question = newQuestion();
        question.accept();
        question.markPlayed();

        assertThatThrownBy(() -> question.correct("Nouveau texte")).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> question.reviseByAuthor("Nouveau texte", false))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void defaultsToNoRevealConsent() {
        assertThat(newQuestion().isRevealAuthorConsent()).isFalse();
    }

    @Test
    void reviseByAuthorCanChangeTheRevealConsent() {
        WhoSaidItQuestion question = newQuestion();
        question.reviseByAuthor("Nouveau texte", true);
        assertThat(question.isRevealAuthorConsent()).isTrue();
    }

    @Test
    void correctingDoesNotChangeTheModerationStatus() {
        WhoSaidItQuestion question = newQuestion();
        question.accept();

        question.correct("Texte corrige");

        assertThat(question.getContent()).isEqualTo("Texte corrige");
        assertThat(question.getStatus()).isEqualTo(WhoSaidItQuestionStatus.ACCEPTED);
    }
}
