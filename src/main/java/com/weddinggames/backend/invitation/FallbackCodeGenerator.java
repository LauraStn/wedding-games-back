package com.weddinggames.backend.invitation;

import java.security.SecureRandom;

/**
 * Generates short, easy-to-type fallback codes for guests who lose or can't scan their QR.
 * Unlike the QR token, this code must remain readable back by the administrator (to give it
 * again over the phone), so it is stored in plain text, not hashed.
 */
public final class FallbackCodeGenerator {

    /** Excludes 0/O, 1/I/L and U/V-confusable characters to minimize transcription errors. */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTWXYZ23456789";
    private static final int LENGTH = 6;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private FallbackCodeGenerator() {}

    public static String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
