package com.weddinggames.backend.matchmaking.dto;

import java.util.List;

/** For a given latecomer: which existing binômes they could join (becoming a trio), and which
 * other unassigned latecomers they could instead form a brand new binôme with. Both lists are
 * already filtered for HARD exclusions - anything listed here is safe to pick in the UI. */
public record LatecomerOptionsResponse(
        List<TeamResponse> compatibleTeams, List<LatecomerCandidateResponse> compatibleLatecomers) {}
