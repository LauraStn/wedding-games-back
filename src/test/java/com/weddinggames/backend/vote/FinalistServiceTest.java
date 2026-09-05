package com.weddinggames.backend.vote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.Question;
import com.weddinggames.backend.game.Vote;
import com.weddinggames.backend.game.VoteRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.team.Team;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the tie-safe top-3 finalist computation. */
class FinalistServiceTest {

    private AnswerRepository answerRepository;
    private VoteRepository voteRepository;
    private FinalistService service;
    private UUID questionId;
    private Question question;

    @BeforeEach
    void setUp() {
        answerRepository = mock(AnswerRepository.class);
        voteRepository = mock(VoteRepository.class);
        service = new FinalistService(answerRepository, voteRepository);
        questionId = UUID.randomUUID();
        question = mock(Question.class);
    }

    private Answer acceptedAnswer(String content) {
        Answer answer = new Answer(question, mock(Team.class), content, Instant.now());
        answer.accept();
        return answer;
    }

    private Vote voteFor(Answer answer) {
        return new Vote(question, answer, mock(Participant.class));
    }

    @Test
    void returnsTheTopThreeByVoteCountWhenThereAreNoTies() {
        Answer first = acceptedAnswer("Premiere");
        Answer second = acceptedAnswer("Deuxieme");
        Answer third = acceptedAnswer("Troisieme");
        Answer fourth = acceptedAnswer("Quatrieme");
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(first, second, third, fourth));

        List<Vote> votes = new ArrayList<>();
        votes.addAll(List.of(voteFor(first), voteFor(first), voteFor(first)));
        votes.addAll(List.of(voteFor(second), voteFor(second)));
        votes.add(voteFor(third));
        // fourth gets zero votes
        when(voteRepository.findByQuestionId(questionId)).thenReturn(votes);

        List<FinalistService.Finalist> finalists = service.computeFinalists(questionId);

        assertThat(finalists).hasSize(3);
        assertThat(finalists.stream().map(f -> f.answer().getContent()))
                .containsExactly("Premiere", "Deuxieme", "Troisieme");
        assertThat(finalists.get(0).voteCount()).isEqualTo(3);
    }

    @Test
    void includesEveryAnswerTiedForTheThirdPlaceEvenIfMoreThanFourResult() {
        Answer first = acceptedAnswer("Premiere");
        Answer tiedA = acceptedAnswer("EgaliteA");
        Answer tiedB = acceptedAnswer("EgaliteB");
        Answer tiedC = acceptedAnswer("EgaliteC");
        Answer noVotes = acceptedAnswer("SansVote");
        when(answerRepository.findByQuestionId(questionId))
                .thenReturn(List.of(first, tiedA, tiedB, tiedC, noVotes));

        List<Vote> votes = new ArrayList<>();
        votes.addAll(List.of(voteFor(first), voteFor(first), voteFor(first)));
        votes.add(voteFor(tiedA));
        votes.add(voteFor(tiedB));
        votes.add(voteFor(tiedC));
        when(voteRepository.findByQuestionId(questionId)).thenReturn(votes);

        List<FinalistService.Finalist> finalists = service.computeFinalists(questionId);

        // Two positive-vote tiers: 3 votes (first) and 1 vote (tiedA/B/C). "SansVote" (0 votes)
        // is never a finalist, however few tiers exist above it.
        assertThat(finalists).hasSize(4);
        assertThat(finalists.stream().map(f -> f.answer().getContent()))
                .containsExactlyInAnyOrder("Premiere", "EgaliteA", "EgaliteB", "EgaliteC");
    }

    @Test
    void neverRandomlyDropsATieEvenWhenItPushesWellPastFour() {
        List<Answer> fiveWayTie = new ArrayList<>();
        List<Vote> votes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Answer answer = acceptedAnswer("Reponse" + i);
            fiveWayTie.add(answer);
            votes.add(voteFor(answer));
        }
        when(answerRepository.findByQuestionId(questionId)).thenReturn(fiveWayTie);
        when(voteRepository.findByQuestionId(questionId)).thenReturn(votes);

        List<FinalistService.Finalist> finalists = service.computeFinalists(questionId);

        assertThat(finalists).hasSize(5);
    }

    @Test
    void neverIncludesAnAnswerWithZeroVotesEvenWhenFewerThanThreeTiersExist() {
        Answer voted = acceptedAnswer("Votee");
        Answer neverVoted = acceptedAnswer("JamaisVotee");
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(voted, neverVoted));
        when(voteRepository.findByQuestionId(questionId)).thenReturn(List.of(voteFor(voted)));

        List<FinalistService.Finalist> finalists = service.computeFinalists(questionId);

        assertThat(finalists).hasSize(1);
        assertThat(finalists.get(0).answer().getContent()).isEqualTo("Votee");
    }

    @Test
    void returnsNoFinalistsWhenNobodyHasVotedYet() {
        Answer first = acceptedAnswer("Premiere");
        Answer second = acceptedAnswer("Deuxieme");
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(first, second));
        when(voteRepository.findByQuestionId(questionId)).thenReturn(List.of());

        assertThat(service.computeFinalists(questionId)).isEmpty();
    }

    @Test
    void ignoresAnswersThatAreNotAccepted() {
        Answer accepted = acceptedAnswer("Acceptee");
        Answer pending = new Answer(question, mock(Team.class), "En attente", Instant.now());
        Answer hidden = new Answer(question, mock(Team.class), "Masquee", Instant.now());
        hidden.hide();
        when(answerRepository.findByQuestionId(questionId)).thenReturn(List.of(accepted, pending, hidden));
        when(voteRepository.findByQuestionId(questionId))
                .thenReturn(List.of(voteFor(accepted), voteFor(pending), voteFor(hidden)));

        List<FinalistService.Finalist> finalists = service.computeFinalists(questionId);

        assertThat(finalists).hasSize(1);
        assertThat(finalists.get(0).answer().getContent()).isEqualTo("Acceptee");
    }
}
