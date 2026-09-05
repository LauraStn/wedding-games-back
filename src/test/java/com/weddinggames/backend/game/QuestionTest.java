package com.weddinggames.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

/** Pure unit test (no Spring context) for the question's guarded status transitions. */
class QuestionTest {

    private Question newQuestion() {
        return new Question(mock(Game.class), "Quel est le comble ?", 0, QuestionSource.ADMIN, null);
    }

    @Test
    void startsPending() {
        assertThat(newQuestion().getStatus()).isEqualTo(QuestionStatus.PENDING);
    }

    @Test
    void activateMovesFromPendingToActive() {
        Question question = newQuestion();

        question.activate();

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.ACTIVE);
    }

    @Test
    void closeMovesFromActiveToClosed() {
        Question question = newQuestion();
        question.activate();

        question.close();

        assertThat(question.getStatus()).isEqualTo(QuestionStatus.CLOSED);
    }

    @Test
    void cannotActivateTwice() {
        Question question = newQuestion();
        question.activate();

        assertThatThrownBy(question::activate).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotCloseAQuestionThatWasNeverActivated() {
        assertThatThrownBy(newQuestion()::close).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotCloseAnAlreadyClosedQuestion() {
        Question question = newQuestion();
        question.activate();
        question.close();

        assertThatThrownBy(question::close).isInstanceOf(BusinessRuleViolationException.class);
    }
}
