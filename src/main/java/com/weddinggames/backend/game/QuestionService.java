package com.weddinggames.backend.game;

import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.game.dto.QuestionCreateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final GameRepository gameRepository;

    public QuestionService(QuestionRepository questionRepository, GameRepository gameRepository) {
        this.questionRepository = questionRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional(readOnly = true)
    public List<Question> listByGame(UUID gameId) {
        return questionRepository.findByGameIdOrderBySequence(gameId);
    }

    @Transactional(readOnly = true)
    public Question get(UUID id) {
        return questionRepository.findById(id).orElseThrow(() -> new NotFoundException("Question introuvable."));
    }

    @Transactional
    public Question create(UUID gameId, QuestionCreateRequest request) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new NotFoundException("Partie introuvable."));
        Question question = new Question(game, request.prompt(), request.sequence(), QuestionSource.ADMIN, null);
        return questionRepository.save(question);
    }

    @Transactional
    public Question activate(UUID id) {
        Question question = get(id);
        question.activate();
        return question;
    }

    @Transactional
    public Question close(UUID id) {
        Question question = get(id);
        question.close();
        return question;
    }
}
