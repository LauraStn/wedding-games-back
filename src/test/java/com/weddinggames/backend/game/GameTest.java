package com.weddinggames.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.event.WeddingEvent;
import org.junit.jupiter.api.Test;

/** Pure unit test (no Spring context) for the shared game state machine. */
class GameTest {

    private Game newGame() {
        return new Game(mock(WeddingEvent.class), GameType.QUIZ, "Quiz", 0);
    }

    @Test
    void startsAsDraftInTheLobbyPhase() {
        Game game = newGame();

        assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFT);
        assertThat(game.getPhase()).isEqualTo(GamePhase.LOBBY);
    }

    @Test
    void startMovesToActiveAndPreparation() {
        Game game = newGame();

        game.start();

        assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.getPhase()).isEqualTo(GamePhase.PREPARATION);
    }

    @Test
    void nextQuestionMovesFromPreparationToQuestion() {
        Game game = newGame();
        game.start();

        game.nextQuestion();

        assertThat(game.getPhase()).isEqualTo(GamePhase.QUESTION);
    }

    @Test
    void cannotAdvanceToTheNextQuestionBeforeStarting() {
        Game game = newGame();

        assertThatThrownBy(game::nextQuestion).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void pauseAndResumePreserveTheCurrentPhase() {
        Game game = newGame();
        game.start();
        game.nextQuestion();

        game.pause();
        assertThat(game.getStatus()).isEqualTo(GameStatus.PAUSED);
        assertThat(game.getPhase()).isEqualTo(GamePhase.QUESTION);

        game.resume();
        assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(game.getPhase()).isEqualTo(GamePhase.QUESTION);
    }

    @Test
    void cannotAdvanceToTheNextQuestionWhilePaused() {
        Game game = newGame();
        game.start();
        game.pause();

        assertThatThrownBy(game::nextQuestion).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotPauseAGameThatIsNotActive() {
        Game game = newGame();

        assertThatThrownBy(game::pause).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotResumeAGameThatIsNotPaused() {
        Game game = newGame();
        game.start();

        assertThatThrownBy(game::resume).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotStartTwiceInARow() {
        Game game = newGame();
        game.start();

        assertThatThrownBy(game::start).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void cannotSkipStraightFromQuestionToQuestionWithoutGoingThroughTheRestOfTheCycle() {
        Game game = newGame();
        game.start();
        game.nextQuestion(); // -> QUESTION

        // Later tickets (answer moderation, vote, jury, podium) own the QUESTION -> ... -> RESULT
        // leg; calling next-question again before that happens must not silently skip it.
        assertThatThrownBy(game::nextQuestion).isInstanceOf(BusinessRuleViolationException.class);
    }
}
