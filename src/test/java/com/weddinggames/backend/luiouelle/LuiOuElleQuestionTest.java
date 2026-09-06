package com.weddinggames.backend.luiouelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.participant.Participant;
import org.junit.jupiter.api.Test;

/** Pure unit test for the guarded PENDING/ACCEPTED/REJECTED/PLAYED status transitions. */
class LuiOuElleQuestionTest {

    private LuiOuElleQuestion newQuestion() {
        return new LuiOuElleQuestion(mock(WeddingEvent.class), mock(Participant.class), "Qui est le plus retard ?");
    }

    @Test
    void startsAsPending() {
        assertThat(newQuestion().getStatus()).isEqualTo(LuiOuElleQuestionStatus.PENDING);
    }

    @Test
    void acceptsFromPending() {
        LuiOuElleQuestion question = newQuestion();
        question.accept();
        assertThat(question.getStatus()).isEqualTo(LuiOuElleQuestionStatus.ACCEPTED);
    }

    @Test
    void rejectsFromPending() {
        LuiOuElleQuestion question = newQuestion();
        question.reject();
        assertThat(question.getStatus()).isEqualTo(LuiOuElleQuestionStatus.REJECTED);
    }

    @Test
    void aRejectionCanBeReconsideredIntoAnAcceptance() {
        LuiOuElleQuestion question = newQuestion();
        question.reject();
        question.accept();
        assertThat(question.getStatus()).isEqualTo(LuiOuElleQuestionStatus.ACCEPTED);
    }

    @Test
    void anAcceptanceCanBeReconsideredIntoARejection() {
        LuiOuElleQuestion question = newQuestion();
        question.accept();
        question.reject();
        assertThat(question.getStatus()).isEqualTo(LuiOuElleQuestionStatus.REJECTED);
    }

    @Test
    void onlyAnAcceptedQuestionCanBeMarkedPlayed() {
        LuiOuElleQuestion pending = newQuestion();
        assertThatThrownBy(pending::markPlayed).isInstanceOf(BusinessRuleViolationException.class);

        LuiOuElleQuestion accepted = newQuestion();
        accepted.accept();
        accepted.markPlayed();
        assertThat(accepted.getStatus()).isEqualTo(LuiOuElleQuestionStatus.PLAYED);
    }

    @Test
    void playedIsTerminalAndCannotBeAcceptedOrRejectedAgain() {
        LuiOuElleQuestion question = newQuestion();
        question.accept();
        question.markPlayed();

        assertThatThrownBy(question::accept).isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(question::reject).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotCorrectOrReviseAPlayedQuestion() {
        LuiOuElleQuestion question = newQuestion();
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
        LuiOuElleQuestion question = newQuestion();
        question.reviseByAuthor("Nouveau texte", true);
        assertThat(question.isRevealAuthorConsent()).isTrue();
    }

    @Test
    void correctingDoesNotChangeTheModerationStatus() {
        LuiOuElleQuestion question = newQuestion();
        question.accept();

        question.correct("Texte corrige");

        assertThat(question.getContent()).isEqualTo("Texte corrige");
        assertThat(question.getStatus()).isEqualTo(LuiOuElleQuestionStatus.ACCEPTED);
    }
}
