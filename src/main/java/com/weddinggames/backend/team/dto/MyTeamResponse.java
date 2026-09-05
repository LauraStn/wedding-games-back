package com.weddinggames.backend.team.dto;

import java.util.List;
import java.util.UUID;

/** "Tu es Sangoku. Ton binôme est Sailor Moon." — my own character plus my partner(s)'. */
public record MyTeamResponse(
        UUID teamId,
        UUID myCharacterId,
        String myCharacterName,
        String myCharacterAvatarUrl,
        String myCharacterDescription,
        List<TeammateResponse> partners) {}
