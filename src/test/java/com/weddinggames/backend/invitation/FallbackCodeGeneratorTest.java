package com.weddinggames.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FallbackCodeGeneratorTest {

    private static final String AMBIGUOUS_CHARACTERS = "0O1IL";

    @Test
    void generatesASixCharacterCodeWithNoAmbiguousCharacters() {
        for (int i = 0; i < 200; i++) {
            String code = FallbackCodeGenerator.generate();
            assertThat(code).hasSize(6);
            for (char c : code.toCharArray()) {
                assertThat(AMBIGUOUS_CHARACTERS).doesNotContain(String.valueOf(c));
            }
        }
    }

    @Test
    void generatesDifferentCodesAcrossCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            codes.add(FallbackCodeGenerator.generate());
        }
        assertThat(codes).hasSizeGreaterThan(1);
    }
}
