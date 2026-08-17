package com.weddinggames.backend.exclusion;

import static org.assertj.core.api.Assertions.assertThat;

import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The absolute business rule from the wedding organizers: Jessika Dijoux can never be paired
 * with Sandrine Santin nor with Patrick Santin. Verified purely through the UUID-based exclusion
 * records created idempotently by the dev/test seeder - never through a name comparison.
 */
class PairingConstraintServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PairingConstraintService pairingConstraintService;

    @Autowired
    private WeddingEventRepository weddingEventRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private UUID eventId() {
        return weddingEventRepository.findBySlug("seed-wedding").orElseThrow().getId();
    }

    private UUID participantId(String firstName, String lastName) {
        return participantRepository.findAll().stream()
                .filter(p -> p.getFirstName().equals(firstName) && p.getLastName().equals(lastName))
                .findFirst()
                .map(Participant::getId)
                .orElseThrow();
    }

    @Test
    void jessikaCanNeverBePairedWithSandrine() {
        UUID eventId = eventId();
        UUID jessikaId = participantId("Jessika", "Dijoux");
        UUID sandrineId = participantId("Sandrine", "Santin");

        assertThat(pairingConstraintService.canPair(eventId, jessikaId, sandrineId)).isFalse();
        assertThat(pairingConstraintService.canPair(eventId, sandrineId, jessikaId)).isFalse();
        assertThat(pairingConstraintService.hasHardExclusion(eventId, jessikaId, sandrineId)).isTrue();
    }

    @Test
    void jessikaCanNeverBePairedWithPatrick() {
        UUID eventId = eventId();
        UUID jessikaId = participantId("Jessika", "Dijoux");
        UUID patrickId = participantId("Patrick", "Santin");

        assertThat(pairingConstraintService.canPair(eventId, jessikaId, patrickId)).isFalse();
        assertThat(pairingConstraintService.canPair(eventId, patrickId, jessikaId)).isFalse();
        assertThat(pairingConstraintService.hasHardExclusion(eventId, jessikaId, patrickId)).isTrue();
    }

    @Test
    void unrelatedParticipantsCanBePaired() {
        WeddingEvent event = weddingEventRepository.findBySlug("seed-wedding").orElseThrow();
        UUID sandrineId = participantId("Sandrine", "Santin");
        UUID patrickId = participantId("Patrick", "Santin");

        // Sandrine and Patrick have no exclusion recorded between them.
        assertThat(pairingConstraintService.canPair(event.getId(), sandrineId, patrickId)).isTrue();
    }
}
