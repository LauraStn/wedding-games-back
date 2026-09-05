package com.weddinggames.backend.participant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.dto.ParticipantImportRow;
import com.weddinggames.backend.participant.dto.ParticipantImportRowInput;
import com.weddinggames.backend.participant.dto.ParticipantImportRowStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** Pure unit test (Mockito, no Spring context) for CSV/Excel participant import parsing rules. */
class ParticipantImportServiceTest {

    private ParticipantRepository participantRepository;
    private WeddingEventRepository weddingEventRepository;
    private ParticipantImportService service;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        participantRepository = mock(ParticipantRepository.class);
        weddingEventRepository = mock(WeddingEventRepository.class);
        service = new ParticipantImportService(participantRepository, weddingEventRepository);
        eventId = UUID.randomUUID();
        when(weddingEventRepository.existsById(eventId)).thenReturn(true);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of());
    }

    @Test
    void rejectsPreviewWhenEventDoesNotExist() {
        when(weddingEventRepository.existsById(eventId)).thenReturn(false);
        MockMultipartFile file = csvFile("prenom,nom\nMarie,Curie\n");

        assertThatThrownBy(() -> service.preview(eventId, file)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsAnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "participants.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.preview(eventId, file)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void parsesACommaSeparatedCsvWithFrenchHeadersAndDefaultsTheDisplayNameAndType() {
        MockMultipartFile file = csvFile("prenom,nom,table\nMarie,Curie,Table 2\n");

        List<ParticipantImportRow> rows = service.preview(eventId, file);

        assertThat(rows).hasSize(1);
        ParticipantImportRow row = rows.get(0);
        assertThat(row.firstName()).isEqualTo("Marie");
        assertThat(row.lastName()).isEqualTo("Curie");
        assertThat(row.displayName()).isEqualTo("Marie Curie");
        assertThat(row.tableLabel()).isEqualTo("Table 2");
        assertThat(row.participantType()).isEqualTo(ParticipantType.GUEST);
        assertThat(row.status()).isEqualTo(ParticipantImportRowStatus.VALID);
    }

    @Test
    void parsesASemicolonSeparatedCsv() {
        MockMultipartFile file = csvFile("prenom;nom\nMarie;Curie\n");

        List<ParticipantImportRow> rows = service.preview(eventId, file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).status()).isEqualTo(ParticipantImportRowStatus.VALID);
    }

    @Test
    void rejectsARowMissingTheFirstOrLastName() {
        MockMultipartFile file = csvFile("prenom,nom\n,Curie\nMarie,\n");

        List<ParticipantImportRow> rows = service.preview(eventId, file);

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.status()).isEqualTo(ParticipantImportRowStatus.REJECTED);
            assertThat(row.rejectionReason()).isNotBlank();
        });
    }

    @Test
    void rejectsARowWithAnUnknownParticipantType() {
        MockMultipartFile file = csvFile("prenom,nom,type\nMarie,Curie,SCIENTIST\n");

        List<ParticipantImportRow> rows = service.preview(eventId, file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).status()).isEqualTo(ParticipantImportRowStatus.REJECTED);
        assertThat(rows.get(0).rejectionReason()).contains("SCIENTIST");
    }

    @Test
    void flagsTheSecondOccurrenceOfTheSameNameAsADuplicateInFile() {
        MockMultipartFile file = csvFile("prenom,nom\nMarie,Curie\nmarie, curie \n");

        List<ParticipantImportRow> rows = service.preview(eventId, file);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).status()).isEqualTo(ParticipantImportRowStatus.VALID);
        assertThat(rows.get(1).status()).isEqualTo(ParticipantImportRowStatus.DUPLICATE_IN_FILE);
    }

    @Test
    void flagsARowMatchingAnExistingParticipantAsADuplicate() {
        WeddingEvent event = mock(WeddingEvent.class);
        Participant existing = new Participant(event, "Marie", "Curie", "Marie Curie", null, ParticipantType.GUEST);
        when(participantRepository.findByEventId(eventId)).thenReturn(List.of(existing));
        MockMultipartFile file = csvFile("prenom,nom\nMarie,Curie\n");

        List<ParticipantImportRow> rows = service.preview(eventId, file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).status()).isEqualTo(ParticipantImportRowStatus.DUPLICATE_EXISTING);
    }

    @Test
    void confirmPersistsTheGivenRowsForTheEvent() {
        WeddingEvent event = mock(WeddingEvent.class);
        when(weddingEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(participantRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ParticipantImportRowInput> input =
                List.of(new ParticipantImportRowInput("Marie", "Curie", "Marie Curie", "Table 2", ParticipantType.GUEST));

        List<Participant> created = service.confirm(eventId, input);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getFirstName()).isEqualTo("Marie");
        assertThat(created.get(0).getEvent()).isSameAs(event);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file", "participants.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
