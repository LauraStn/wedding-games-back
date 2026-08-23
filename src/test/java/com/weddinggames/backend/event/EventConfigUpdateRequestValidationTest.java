package com.weddinggames.backend.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddinggames.backend.event.dto.EventConfigUpdateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Pure Bean Validation unit test (no Spring context) for the event configuration DTO. */
class EventConfigUpdateRequestValidationTest {

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
    void rejectsBlankTitle() {
        EventConfigUpdateRequest request =
                new EventConfigUpdateRequest("", "Jessika", "Sandrine", LocalDate.of(2027, 6, 12), "Le Domaine", null, Map.of());

        Set<ConstraintViolation<EventConfigUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    void acceptsAValidRequestWithOptionalFieldsLeftBlank() {
        EventConfigUpdateRequest request = new EventConfigUpdateRequest("Notre mariage", null, null, null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsAFullyPopulatedRequest() {
        EventConfigUpdateRequest request = new EventConfigUpdateRequest(
                "Le mariage de Jessika et Sandrine",
                "Jessika",
                "Sandrine",
                LocalDate.of(2027, 6, 12),
                "Le Domaine des Ecrins",
                "Bienvenue a notre mariage !",
                Map.of("primaryColor", "#2457ff"));

        assertThat(validator.validate(request)).isEmpty();
    }
}
