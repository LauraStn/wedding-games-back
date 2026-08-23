package com.weddinggames.backend.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;

/** Visual/textual configuration of an event. All fields but the title are optional: an
 * event can be created and left partially configured until the wedding details are ready. */
public record EventConfigUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 150) String spouseOneName,
        @Size(max = 150) String spouseTwoName,
        LocalDate eventDate,
        @Size(max = 200) String venueName,
        @Size(max = 2000) String welcomeMessage,
        Map<String, Object> visualConfig) {}
