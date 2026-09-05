package com.weddinggames.backend.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.event.WeddingEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit test (Mockito, no Spring context) for the participant CSV export formatting. */
class ParticipantExportServiceTest {

    private ParticipantRepository participantRepository;
    private ParticipantExportService service;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        participantRepository = mock(ParticipantRepository.class);
        service = new ParticipantExportService(participantRepository);
        eventId = UUID.randomUUID();
    }

    @Test
    void exportsAHeaderRowAndOneLinePerParticipant() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant participant = new Participant(event, "Marie", "Curie", "Marie Curie", "Table 2", ParticipantType.GUEST);
        when(participantRepository.search(eventId, null, null, null, null)).thenReturn(List.of(participant));

        byte[] csv = service.exportCsv(eventId, null, null, null, null);
        String content = new String(csv, StandardCharsets.UTF_8);
        List<String> lines = content.lines().toList();

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).isEqualTo("prenom,nom,nom_affiche,table,type,statut,points,victoires");
        assertThat(lines.get(1)).isEqualTo("Marie,Curie,Marie Curie,Table 2,GUEST,INVITED,0,0");
    }

    @Test
    void exportsOnlyTheHeaderRowWhenThereAreNoParticipants() {
        when(participantRepository.search(eventId, null, null, null, null)).thenReturn(List.of());

        byte[] csv = service.exportCsv(eventId, null, null, null, null);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertThat(content.lines().toList()).hasSize(1);
    }
}
