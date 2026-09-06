package com.weddinggames.backend.configuration;

import com.weddinggames.backend.event.EventService;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.exclusion.ExclusionType;
import com.weddinggames.backend.exclusion.PairingConstraintService;
import com.weddinggames.backend.exclusion.PairingExclusionService;
import com.weddinggames.backend.exclusion.dto.PairingExclusionCreateRequest;
import com.weddinggames.backend.participant.Participant;
import com.weddinggames.backend.participant.ParticipantRepository;
import com.weddinggames.backend.participant.ParticipantType;
import com.weddinggames.backend.participant.dto.ParticipantCreateRequest;
import com.weddinggames.backend.participant.ParticipantService;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Idempotent dev/test fixtures. Guarantees the three named participants and the two absolute
 * (HARD) pairing exclusions exist, expressed strictly as UUID-to-UUID exclusion records - never
 * a name comparison - so the business rule that Jessika Dijoux can never be paired with either
 * Sandrine Santin or Patrick Santin is enforced the same way it would be for any other guest.
 */
@Component
@Profile({"dev", "test"})
public class DevSeedDataRunner implements ApplicationRunner {

    private static final String SEED_EVENT_SLUG = "seed-wedding";

    private final EventService eventService;
    private final ParticipantService participantService;
    private final ParticipantRepository participantRepository;
    private final PairingExclusionService pairingExclusionService;
    private final PairingConstraintService pairingConstraintService;

    public DevSeedDataRunner(
            EventService eventService,
            ParticipantService participantService,
            ParticipantRepository participantRepository,
            PairingExclusionService pairingExclusionService,
            PairingConstraintService pairingConstraintService) {
        this.eventService = eventService;
        this.participantService = participantService;
        this.participantRepository = participantRepository;
        this.pairingExclusionService = pairingExclusionService;
        this.pairingConstraintService = pairingConstraintService;
    }

    @Override
    public void run(ApplicationArguments args) {
        WeddingEvent event = eventService.ensureEventExists(SEED_EVENT_SLUG, "Mariage de demonstration", "fr-FR");

        UUID jessikaId = ensureParticipant(event.getId(), "Jessika", "Dijoux").getId();
        UUID sandrineId = ensureParticipant(event.getId(), "Sandrine", "Santin").getId();
        UUID patrickId = ensureParticipant(event.getId(), "Patrick", "Santin").getId();

        ensureHardExclusion(event.getId(), jessikaId, sandrineId, "Contrainte familiale absolue");
        ensureHardExclusion(event.getId(), jessikaId, patrickId, "Contrainte familiale absolue");
    }

    private Participant ensureParticipant(UUID eventId, String firstName, String lastName) {
        return participantRepository
                .findByEventIdAndFirstNameAndLastName(eventId, firstName, lastName)
                .orElseGet(() -> participantService.create(
                        eventId,
                        new ParticipantCreateRequest(
                                firstName, lastName, firstName + " " + lastName, null, ParticipantType.GUEST, null)));
    }

    private void ensureHardExclusion(UUID eventId, UUID participantAId, UUID participantBId, String reason) {
        if (pairingConstraintService.findExclusion(eventId, participantAId, participantBId).isPresent()) {
            return;
        }
        // null staffAccountId: this is system-seeded fixture data at startup, not a real admin
        // action, so AuditLogService intentionally skips logging it rather than attributing it
        // to nobody.
        pairingExclusionService.create(
                eventId,
                new PairingExclusionCreateRequest(participantAId, participantBId, reason, ExclusionType.HARD),
                null);
    }
}
