package com.weddinggames.backend.jury;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.Question;
import org.junit.jupiter.api.Test;

/** Pure unit test for the PENDING/CHOSEN/CONFIRMED lifecycle and the reveal gate. */
class JuryDecisionTest {

    private JuryDecision newDecision() {
        return new JuryDecision(mock(Question.class));
    }

    @Test
    void startsAsPendingWithNoChoice() {
        JuryDecision decision = newDecision();
        assertThat(decision.getStatus()).isEqualTo(JuryDecisionStatus.PENDING);
        assertThat(decision.getChosenAnswer()).isNull();
    }

    @Test
    void choosingSetsTheAnswerAndMovesToChosen() {
        JuryDecision decision = newDecision();
        Answer answer = mock(Answer.class);

        decision.choose(answer);

        assertThat(decision.getChosenAnswer()).isEqualTo(answer);
        assertThat(decision.getStatus()).isEqualTo(JuryDecisionStatus.CHOSEN);
    }

    @Test
    void canReconsiderTheChoiceBeforeConfirming() {
        JuryDecision decision = newDecision();
        Answer first = mock(Answer.class);
        Answer second = mock(Answer.class);
        decision.choose(first);

        decision.choose(second);

        assertThat(decision.getChosenAnswer()).isEqualTo(second);
    }

    @Test
    void cannotConfirmWithoutHavingChosen() {
        assertThatThrownBy(newDecision()::confirm).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void confirmsAChosenDecision() {
        JuryDecision decision = newDecision();
        decision.choose(mock(Answer.class));

        decision.confirm();

        assertThat(decision.getStatus()).isEqualTo(JuryDecisionStatus.CONFIRMED);
    }

    @Test
    void cannotChangeTheChoiceOnceConfirmed() {
        JuryDecision decision = newDecision();
        decision.choose(mock(Answer.class));
        decision.confirm();

        assertThatThrownBy(() -> decision.choose(mock(Answer.class)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotRevealBeforeConfirming() {
        JuryDecision decision = newDecision();
        decision.choose(mock(Answer.class));

        assertThatThrownBy(decision::reveal).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void revealsAConfirmedDecision() {
        JuryDecision decision = newDecision();
        decision.choose(mock(Answer.class));
        decision.confirm();

        decision.reveal();

        assertThat(decision.isRevealed()).isTrue();
    }
}
