package com.weddinggames.backend.game;

/**
 * Shared gameplay cycle reused by every game type (quiz absurde, "Lui ou elle ?", blind test):
 * Salon -> Préparation -> Question -> Réponses fermées -> Vote -> Jury -> Résultat -> (Question
 * again, or the game finishes). Not every game type visits every phase (blind test has no jury,
 * for instance) - each game type's own service only calls the transitions it needs.
 */
public enum GamePhase {
    LOBBY,
    PREPARATION,
    QUESTION,
    ANSWERS_CLOSED,
    VOTE,
    JURY,
    RESULT
}
