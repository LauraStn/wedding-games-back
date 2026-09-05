package com.weddinggames.backend.invitation;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

/** Assembles a printable A4 sheet of participant QR codes ("planche d'impression") from raw invitation URLs. */
@Service
public class InvitationPrintSheetService {

    private static final int COLUMNS = 3;
    private static final int ROWS = 4;
    private static final int QR_SIZE_PX = 300;
    private static final float PAGE_MARGIN = 24f;
    private static final float QR_DRAW_SIZE = 120f;
    private static final PDFont TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont LABEL_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] buildPrintSheet(List<InvitationPrintCard> cards) {
        try (PDDocument document = new PDDocument()) {
            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float cellWidth = (pageWidth - 2 * PAGE_MARGIN) / COLUMNS;
            float cellHeight = (pageHeight - 2 * PAGE_MARGIN) / ROWS;
            int perPage = COLUMNS * ROWS;

            for (int pageStart = 0; pageStart < cards.size(); pageStart += perPage) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                List<InvitationPrintCard> pageCards =
                        cards.subList(pageStart, Math.min(pageStart + perPage, cards.size()));

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    for (int i = 0; i < pageCards.size(); i++) {
                        InvitationPrintCard card = pageCards.get(i);
                        int column = i % COLUMNS;
                        int row = i / COLUMNS;
                        float cellX = PAGE_MARGIN + column * cellWidth;
                        float cellTopY = pageHeight - PAGE_MARGIN - row * cellHeight;
                        drawCard(document, content, card, cellX, cellTopY, cellWidth, cellHeight);
                    }
                }
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void drawCard(
            PDDocument document,
            PDPageContentStream content,
            InvitationPrintCard card,
            float cellX,
            float cellTopY,
            float cellWidth,
            float cellHeight)
            throws IOException {
        float qrX = cellX + (cellWidth - QR_DRAW_SIZE) / 2;
        float qrY = cellTopY - cellHeight + 40;

        PDImageXObject qrImage = LosslessFactory.createFromImage(document, generateQrImage(card.invitationUrl()));
        content.drawImage(qrImage, qrX, qrY, QR_DRAW_SIZE, QR_DRAW_SIZE);

        String displayName = card.displayName() == null ? "" : card.displayName();
        drawCenteredText(content, displayName, TITLE_FONT, 11, cellX, cellWidth, qrY - 16);

        if (card.tableLabel() != null && !card.tableLabel().isBlank()) {
            drawCenteredText(content, card.tableLabel(), LABEL_FONT, 9, cellX, cellWidth, qrY - 30);
        }
    }

    private void drawCenteredText(
            PDPageContentStream content, String text, PDFont font, float fontSize, float cellX, float cellWidth, float y)
            throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = cellX + (cellWidth - textWidth) / 2;
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private BufferedImage generateQrImage(String content) {
        try {
            BitMatrix matrix =
                    new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException e) {
            throw new IllegalStateException("Impossible de generer le QR code.", e);
        }
    }
}
