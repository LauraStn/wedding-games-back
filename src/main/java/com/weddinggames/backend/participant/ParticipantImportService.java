package com.weddinggames.backend.participant;

import com.weddinggames.backend.common.exception.InvalidRequestException;
import com.weddinggames.backend.common.exception.NotFoundException;
import com.weddinggames.backend.event.WeddingEvent;
import com.weddinggames.backend.event.WeddingEventRepository;
import com.weddinggames.backend.participant.dto.ParticipantImportRow;
import com.weddinggames.backend.participant.dto.ParticipantImportRowInput;
import com.weddinggames.backend.participant.dto.ParticipantImportRowStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Parses an admin-supplied CSV/Excel file of participants into a review-able preview (nothing is
 * persisted at this stage) and, separately, persists the rows the administrator confirmed.
 */
@Service
public class ParticipantImportService {

    private static final Set<String> FIRST_NAME_ALIASES = Set.of("prenom", "firstname");
    private static final Set<String> LAST_NAME_ALIASES = Set.of("nom", "lastname");
    private static final Set<String> DISPLAY_NAME_ALIASES = Set.of("nomaffiche", "surnom", "displayname");
    private static final Set<String> TABLE_LABEL_ALIASES = Set.of("table", "tablelabel");
    private static final Set<String> PARTICIPANT_TYPE_ALIASES = Set.of("type", "participanttype");

    private final ParticipantRepository participantRepository;
    private final WeddingEventRepository weddingEventRepository;

    public ParticipantImportService(
            ParticipantRepository participantRepository, WeddingEventRepository weddingEventRepository) {
        this.participantRepository = participantRepository;
        this.weddingEventRepository = weddingEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ParticipantImportRow> preview(UUID eventId, MultipartFile file) {
        if (!weddingEventRepository.existsById(eventId)) {
            throw new NotFoundException("Evenement introuvable.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("IMPORT_FILE_EMPTY", "Le fichier d'import est vide ou manquant.");
        }

        List<Map<String, String>> rawRows = parseRows(file);

        Set<String> existingKeys = new HashSet<>();
        for (Participant participant : participantRepository.findByEventId(eventId)) {
            existingKeys.add(nameKey(participant.getFirstName(), participant.getLastName()));
        }

        List<ParticipantImportRow> rows = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int rowNumber = 0;
        for (Map<String, String> rawRow : rawRows) {
            rowNumber++;
            rows.add(toImportRow(rowNumber, rawRow, seenInFile, existingKeys));
        }
        return rows;
    }

    @Transactional
    public List<Participant> confirm(UUID eventId, List<ParticipantImportRowInput> rows) {
        WeddingEvent event =
                weddingEventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Evenement introuvable."));

        List<Participant> participants = new ArrayList<>();
        for (ParticipantImportRowInput row : rows) {
            participants.add(new Participant(
                    event, row.firstName(), row.lastName(), row.displayName(), row.tableLabel(), row.participantType()));
        }
        return participantRepository.saveAll(participants);
    }

    private ParticipantImportRow toImportRow(
            int rowNumber, Map<String, String> rawRow, Set<String> seenInFile, Set<String> existingKeys) {
        String firstName = trimToNull(rawRow.get("firstName"));
        String lastName = trimToNull(rawRow.get("lastName"));
        String displayName = trimToNull(rawRow.get("displayName"));
        String tableLabel = trimToNull(rawRow.get("tableLabel"));
        String rawParticipantType = trimToNull(rawRow.get("participantType"));

        if (firstName == null || lastName == null) {
            return new ParticipantImportRow(
                    rowNumber,
                    firstName,
                    lastName,
                    displayName,
                    tableLabel,
                    null,
                    ParticipantImportRowStatus.REJECTED,
                    "Prenom et nom sont requis.");
        }

        ParticipantType participantType = ParticipantType.GUEST;
        if (rawParticipantType != null) {
            try {
                participantType = ParticipantType.valueOf(rawParticipantType.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return new ParticipantImportRow(
                        rowNumber,
                        firstName,
                        lastName,
                        displayName,
                        tableLabel,
                        null,
                        ParticipantImportRowStatus.REJECTED,
                        "Type de participant invalide: " + rawParticipantType);
            }
        }

        if (displayName == null) {
            displayName = firstName + " " + lastName;
        }

        String key = nameKey(firstName, lastName);
        ParticipantImportRowStatus status;
        String rejectionReason = null;
        if (!seenInFile.add(key)) {
            status = ParticipantImportRowStatus.DUPLICATE_IN_FILE;
        } else if (existingKeys.contains(key)) {
            status = ParticipantImportRowStatus.DUPLICATE_EXISTING;
        } else {
            status = ParticipantImportRowStatus.VALID;
        }

        return new ParticipantImportRow(
                rowNumber, firstName, lastName, displayName, tableLabel, participantType, status, rejectionReason);
    }

    private List<Map<String, String>> parseRows(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        try {
            if (filename.endsWith(".xlsx")) {
                return parseXlsx(file);
            }
            if (filename.endsWith(".csv") || filename.isEmpty()) {
                return parseCsv(file);
            }
            throw new InvalidRequestException(
                    "IMPORT_UNSUPPORTED_FORMAT", "Format de fichier non supporte (attendu: .csv ou .xlsx).");
        } catch (IOException | UncheckedIOException e) {
            throw new InvalidRequestException("IMPORT_FILE_UNREADABLE", "Le fichier d'import est illisible ou corrompu.");
        }
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
        byte[] content = file.getBytes();
        char delimiter = detectDelimiter(new String(content, StandardCharsets.UTF_8));
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        List<Map<String, String>> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                CSVParser parser = format.parse(reader)) {
            Map<String, String> headerMapping = mapHeaders(parser.getHeaderNames());
            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                for (Map.Entry<String, String> entry : headerMapping.entrySet()) {
                    row.put(entry.getValue(), record.isSet(entry.getKey()) ? record.get(entry.getKey()) : null);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, String>> parseXlsx(MultipartFile file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();
        try (InputStream inputStream = file.getInputStream();
                XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return rows;
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<Integer, String> columnMapping = new HashMap<>();
            for (Cell cell : headerRow) {
                String canonical = canonicalHeader(dataFormatter.formatCellValue(cell));
                if (canonical != null) {
                    columnMapping.put(cell.getColumnIndex(), canonical);
                }
            }
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null) {
                    continue;
                }
                Map<String, String> row = new HashMap<>();
                boolean allBlank = true;
                for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
                    Cell cell = dataRow.getCell(entry.getKey());
                    String value = cell == null || cell.getCellType() == CellType.BLANK
                            ? null
                            : dataFormatter.formatCellValue(cell);
                    if (value != null && !value.isBlank()) {
                        allBlank = false;
                    }
                    row.put(entry.getValue(), value);
                }
                if (!allBlank) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private Map<String, String> mapHeaders(List<String> headerNames) {
        Map<String, String> mapping = new HashMap<>();
        for (String header : headerNames) {
            String canonical = canonicalHeader(header);
            if (canonical != null) {
                mapping.put(header, canonical);
            }
        }
        return mapping;
    }

    private String canonicalHeader(String rawHeader) {
        String normalized = normalize(rawHeader);
        if (FIRST_NAME_ALIASES.contains(normalized)) {
            return "firstName";
        }
        if (LAST_NAME_ALIASES.contains(normalized)) {
            return "lastName";
        }
        if (DISPLAY_NAME_ALIASES.contains(normalized)) {
            return "displayName";
        }
        if (TABLE_LABEL_ALIASES.contains(normalized)) {
            return "tableLabel";
        }
        if (PARTICIPANT_TYPE_ALIASES.contains(normalized)) {
            return "participantType";
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutDiacritics.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private char detectDelimiter(String content) {
        int newlineIndex = content.indexOf('\n');
        String firstLine = newlineIndex >= 0 ? content.substring(0, newlineIndex) : content;
        long semicolons = firstLine.chars().filter(c -> c == ';').count();
        long commas = firstLine.chars().filter(c -> c == ',').count();
        return semicolons > commas ? ';' : ',';
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nameKey(String firstName, String lastName) {
        return normalize(firstName) + "|" + normalize(lastName);
    }
}
