package com.weddinggames.backend.whosaidit;

import com.weddinggames.backend.common.exception.BusinessRuleViolationException;
import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.lobby.Lobby;
import com.weddinggames.backend.lobby.LobbyRepository;
import com.weddinggames.backend.lobby.LobbyStatus;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets a guest propose (and, while the lobby is still open, edit) up to a configurable number of
 * "Who Said It" questions about the couple, along with their consent to have their first name
 * revealed alongside it. Moderation (see {@link WhoSaidItModerationService}) and random selection
 * for play are handled by their own tickets/services downstream of this one.
 */
@Service
public class WhoSaidItQuestionService {

    private final WhoSaidItQuestionRepository questionRepository;
    private final ParticipantRepository participantRepository;
    private final LobbyRepository lobbyRepository;
    private final WhoSaidItProperties properties;

    public WhoSaidItQuestionService(
            WhoSaidItQuestionRepository questionRepository,
            ParticipantRepository participantRepository,
            LobbyRepository lobbyRepository,
            WhoSaidItProperties properties) {
        this.questionRepository = questionRepository;
        this.participantRepository = participantRepository;
        this.lobbyRepository = lobbyRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<WhoSaidItQuestion> listMine(UUID participantId) {
        return questionRepository.findByAuthorId(participantId);
    }

    @Transactional
    public WhoSaidItQuestion propose(UUID participantId, String content, boolean revealAuthorConsent) {
        Participant author = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        requireLobbyOpen(author.getEvent().getId());
        requireValidContent(content);

        long alreadyProposed = questionRepository.countByAuthorId(participantId);
        if (alreadyProposed >= properties.getMaxQuestionsPerParticipant()) {
            throw new BusinessRuleViolationException(
                    "WHO_SAID_IT_QUESTION_LIMIT_REACHED",
                    "Vous avez deja propose le maximum de " + properties.getMaxQuestionsPerParticipant()
                            + " question(s).");
        }

        WhoSaidItQuestion question =
                new WhoSaidItQuestion(author.getEvent(), author, content.trim(), revealAuthorConsent);
        return questionRepository.save(question);
    }

    @Transactional
    public WhoSaidItQuestion update(UUID participantId, UUID questionId, String content, boolean revealAuthorConsent) {
        WhoSaidItQuestion question = questionRepository
                .findByIdAndAuthorId(questionId, participantId)
                .orElseThrow(() -> new NotFoundException("Question introuvable."));
        requireLobbyOpen(question.getEvent().getId());
        requireValidContent(content);

        question.reviseByAuthor(content.trim(), revealAuthorConsent);
        return question;
    }

    private void requireLobbyOpen(UUID eventId) {
        Lobby lobby = lobbyRepository.findByEventId(eventId).orElse(null);
        if (lobby == null || lobby.getStatus() != LobbyStatus.OPEN) {
            throw new BusinessRuleViolationException(
                    "LOBBY_NOT_OPEN",
                    "Le salon doit etre ouvert pour proposer ou modifier une question Who Said It.");
        }
    }

    private void requireValidContent(String content) {
        if (content.trim().length() > properties.getMaxContentLength()) {
            throw new InvalidRequestException(
                    "CONTENT_TOO_LONG",
                    "La question ne doit pas depasser " + properties.getMaxContentLength() + " caracteres.");
        }
    }
}
