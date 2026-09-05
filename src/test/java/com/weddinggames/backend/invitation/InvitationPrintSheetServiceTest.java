package com.weddinggames.backend.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

/** Pure unit test (no Spring context) for the printable QR sheet: it must produce a valid, loadable PDF. */
class InvitationPrintSheetServiceTest {

    private final InvitationPrintSheetService service = new InvitationPrintSheetService();

    @Test
    void producesAOnePageValidPdfForUpToTwelveCards() throws Exception {
        List<InvitationPrintCard> cards = List.of(
                new InvitationPrintCard("Alice Wonderland", "Table 5", "https://example.test/invite/token-a"),
                new InvitationPrintCard("Bob Builder", null, "https://example.test/invite/token-b"));

        byte[] pdf = service.buildPrintSheet(cards);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void spreadsMoreThanTwelveCardsAcrossSeveralPages() throws Exception {
        List<InvitationPrintCard> cards = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            cards.add(new InvitationPrintCard("Guest " + i, "Table " + i, "https://example.test/invite/token-" + i));
        }

        byte[] pdf = service.buildPrintSheet(cards);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    void producesAnEmptyDocumentWhenThereAreNoCards() throws Exception {
        byte[] pdf = service.buildPrintSheet(List.of());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isZero();
        }
    }
}
