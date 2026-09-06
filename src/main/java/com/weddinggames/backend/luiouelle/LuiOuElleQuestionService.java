package com.weddinggames.backend.luiouelle;

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
 * "Lui ou Elle" questions about the couple, along with their consent to have their first name
 * revealed alongside it. Moderation (see {@link LuiOuElleModerationService}) and random selection
 * for play are handled by their own tickets/services downstream of this one.
 */
@Service
public class LuiOuElleQuestionService {

    private final LuiOuElleQuestionRepository questionRepository;
    private final ParticipantRepository participantRepository;
    private final LobbyRepository lobbyRepository;
    private final LuiOuElleProperties properties;

    public LuiOuElleQuestionService(
            LuiOuElleQuestionRepository questionRepository,
            ParticipantRepository participantRepository,
            LobbyRepository lobbyRepository,
            LuiOuElleProperties properties) {
        this.questionRepository = questionRepository;
        this.participantRepository = participantRepository;
        this.lobbyRepository = lobbyRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<LuiOuElleQuestion> listMine(UUID participantId) {
        return questionRepository.findByAuthorId(participantId);
    }

    @Transactional
    public LuiOuElleQuestion propose(UUID participantId, String content, boolean revealAuthorConsent) {
        Participant author = participantRepository
                .findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant introuvable."));
        requireLobbyOpen(author.getEvent().getId());
        requireValidContent(content);

        long alreadyProposed = questionRepository.countByAuthorId(participantId);
        if (alreadyProposed >= properties.getMaxQuestionsPerParticipant()) {
            throw new BusinessRuleViolationException(
                    "LUI_OU_ELLE_QUESTION_LIMIT_REACHED",
                    "Vous avez deja propose le maximum de " + properties.getMaxQuestionsPerParticipant()
                            + " question(s).");
        }

        LuiOuElleQuestion question =
                new LuiOuElleQuestion(author.getEvent(), author, content.trim(), revealAuthorConsent);
        return questionRepository.save(question);
    }

    @Transactional
    public LuiOuElleQuestion update(UUID participantId, UUID questionId, String content, boolean revealAuthorConsent) {
        LuiOuElleQuestion question = questionRepository
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
                    "Le salon doit etre ouvert pour proposer ou modifier une question Lui ou Elle.");
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
