package com.weddinggames.backend.vote;

import com.weddinggames.backend.game.Answer;
import com.weddinggames.backend.game.AnswerModerationStatus;
import com.weddinggames.backend.game.AnswerRepository;
import com.weddinggames.backend.game.VoteRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ranks accepted answers by vote count and keeps the top 3 <em>distinct vote-count tiers</em>
 * (not the top 3 answers): if several answers are tied at whichever tier would otherwise be the
 * cutoff, every tied answer is kept - no random draw ever eliminates a tie, even if that means
 * more than 3 (or the commonly expected 4) answers reach the jury.
 *
 * <p>Tallies by {@link Answer} object identity rather than {@code getId()}: within the single
 * transaction this runs in, Hibernate's session-level identity map guarantees the same DB row
 * is always the same Java instance, so this is safe and sidesteps a brand new, not-yet-persisted
 * {@link Answer} always having a null id.
 */
@Service
public class FinalistService {

    private final AnswerRepository answerRepository;
    private final VoteRepository voteRepository;

    public FinalistService(AnswerRepository answerRepository, VoteRepository voteRepository) {
        this.answerRepository = answerRepository;
        this.voteRepository = voteRepository;
    }

    public record Finalist(Answer answer, long voteCount) {}

    @Transactional(readOnly = true)
    public List<Finalist> computeFinalists(UUID questionId) {
        List<Answer> accepted = answerRepository.findByQuestionId(questionId).stream()
                .filter(answer -> answer.getModerationStatus() == AnswerModerationStatus.ACCEPTED)
                .toList();

        Map<Answer, Long> voteCountByAnswer = new IdentityHashMap<>();
        for (var vote : voteRepository.findByQuestionId(questionId)) {
            voteCountByAnswer.merge(vote.getAnswer(), 1L, Long::sum);
        }

        Map<Answer, Long> countByAnswer = new IdentityHashMap<>();
        for (Answer answer : accepted) {
            countByAnswer.put(answer, voteCountByAnswer.getOrDefault(answer, 0L));
        }

        // An answer with zero votes is never "most voted", however few distinct tiers exist above
        // it - so 0 is deliberately excluded before picking the top 3 tiers, not just naturally
        // squeezed out by them.
        List<Long> distinctPositiveCountsDesc = countByAnswer.values().stream()
                .filter(count -> count > 0)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
        Set<Long> topThreeTiers =
                new HashSet<>(distinctPositiveCountsDesc.subList(0, Math.min(3, distinctPositiveCountsDesc.size())));

        return accepted.stream()
                .filter(answer -> topThreeTiers.contains(countByAnswer.get(answer)))
                .sorted(Comparator.comparingLong((Answer a) -> countByAnswer.get(a)).reversed())
                .map(answer -> new Finalist(answer, countByAnswer.get(answer)))
                .toList();
    }
}
