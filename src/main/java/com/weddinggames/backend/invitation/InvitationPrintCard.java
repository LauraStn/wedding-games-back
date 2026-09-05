package com.weddinggames.backend.invitation;

/** One participant's invitation, ready to be laid out on the printable QR sheet. */
public record InvitationPrintCard(String displayName, String tableLabel, String invitationUrl) {}
