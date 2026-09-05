package com.weddinggames.backend.participant;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddinggames.backend.participant.dto.ParticipantCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pure Bean Validation unit test (no Spring context) for the participant creation DTO. */
class ParticipantDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void rejectsBlankFirstName() {
        ParticipantCreateRequest request =
                new ParticipantCreateRequest("", "Dijoux", "Jessika Dijoux", null, ParticipantType.GUEST, null);

        Set<ConstraintViolation<ParticipantCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
    }

    @Test
    void rejectsMissingParticipantType() {
        ParticipantCreateRequest request =
                new ParticipantCreateRequest("Jessika", "Dijoux", "Jessika Dijoux", null, null, null);

        Set<ConstraintViolation<ParticipantCreateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("participantType"));
    }

    @Test
    void acceptsAValidRequest() {
        ParticipantCreateRequest request = new ParticipantCreateRequest(
                "Jessika", "Dijoux", "Jessika Dijoux", "Table 4", ParticipantType.GUEST, null);

        assertThat(validator.validate(request)).isEmpty();
    }
}
