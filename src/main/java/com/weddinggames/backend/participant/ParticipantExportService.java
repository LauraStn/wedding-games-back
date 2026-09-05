package com.weddinggames.backend.participant;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Formats the participant list of an event as a downloadable CSV export. */
@Service
public class ParticipantExportService {

    private static final List<String> HEADERS =
            List.of("prenom", "nom", "nom_affiche", "table", "type", "statut", "points", "victoires");

    private final ParticipantRepository participantRepository;

    public ParticipantExportService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(
            UUID eventId, ParticipantStatus status, String tableLabel, ParticipantType participantType, String query) {
        List<Participant> participants = participantRepository.search(eventId, status, tableLabel, participantType, query);

        StringWriter writer = new StringWriter();
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader(HEADERS.toArray(new String[0]))
                .build();
        try (CSVPrinter printer = format.print(writer)) {
            for (Participant participant : participants) {
                printer.printRecord(
                        participant.getFirstName(),
                        participant.getLastName(),
                        participant.getDisplayName(),
                        participant.getTableLabel(),
                        participant.getParticipantType(),
                        participant.getStatus(),
                        participant.getTotalPoints(),
                        participant.getTotalWins());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }
}
