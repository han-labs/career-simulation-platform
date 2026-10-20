package edu.hcmute.careersim.guidance.knowledge;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.guidance.knowledge.infrastructure.KnowledgeParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class KnowledgeParserContractTest {
    static final String FACT =
            "Synthetic backend practice includes API triage and careful validation.";
    static final String
            DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    final KnowledgeParser parser = new KnowledgeParser();

    public static byte[] fixture(String format) throws Exception {
        var bytes = new ByteArrayOutputStream();
        switch (format) {
            case "txt", "md" -> bytes.write(FACT.getBytes(StandardCharsets.UTF_8));
            case "docx" -> {
                try (var doc = new XWPFDocument()) {
                    doc.createParagraph().createRun().setText(FACT);
                    doc.write(bytes);
                }
            }
            case "xlsx" -> {
                try (var book = new XSSFWorkbook()) {
                    book.createSheet("Synthetic").createRow(0).createCell(0).setCellValue(FACT);
                    book.write(bytes);
                }
            }
            case "pdf" -> {
                try (var doc = new PDDocument()) {
                    var page = new PDPage();
                    doc.addPage(page);
                    try (var stream = new PDPageContentStream(doc, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(20, 700);
                        stream.showText(FACT);
                        stream.endText();
                    }
                    doc.save(bytes);
                }
            }
        }
        return bytes.toByteArray();
    }

    public static String mime(String format) {
        return switch (format) {
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "pdf" -> "application/pdf";
            case "docx" -> DOCX;
            default -> XLSX;
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "md", "pdf", "docx", "xlsx"})
    void extractsFiveFormatsWithDeterministicRealLocators(String format) throws Exception {
        var bytes = fixture(format);
        var parsed = parser.parse("fixture." + format, mime(format), bytes, "Synthetic");
        assertThat(parsed)
                .isEqualTo(parser.parse("fixture." + format, mime(format), bytes, "Synthetic"));
        assertThat(parsed.chunks()).hasSize(1);
        assertThat(parsed.chunks().get(0).text()).contains(FACT);
        assertThat(parsed.chunks().get(0).locator().path("format").asText()).isEqualTo(format);
        assertThat(parsed.chunks().get(0).locator().toString())
                .containsAnyOf("lineStart", "page", "paragraph", "sheet");
    }

    @Test
    void rejectsInvalidMimeUtf8AndFileSignature() {
        assertThatThrownBy(() -> parser.parse("x.txt", "application/pdf", new byte[] {1, 2}, "x"))
                .isInstanceOf(KnowledgeParser.ParseFailure.class);
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        "x.txt",
                                        "text/plain",
                                        new byte[] {(byte) 0xc3, (byte) 0xff},
                                        "x"))
                .hasMessage("INVALID_UTF8_TEXT");
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        "x.pdf",
                                        "application/pdf",
                                        "not a PDF".getBytes(StandardCharsets.UTF_8),
                                        "x"))
                .hasMessage("INVALID_PDF_SIGNATURE");
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        "x.docx",
                                        DOCX,
                                        new byte[] {(byte) 0xd0, (byte) 0xcf, 17, 0},
                                        "x"))
                .hasMessage("ENCRYPTED_OR_INVALID_OFFICE");
    }

    @Test
    void rejectsFormulaInsteadOfExecutingIt() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var b = new XSSFWorkbook()) {
            b.createSheet("Data").createRow(0).createCell(0).setCellFormula("1+1");
            b.write(bytes);
        }
        assertThatThrownBy(() -> parser.parse("x.xlsx", XLSX, bytes.toByteArray(), "x"))
                .hasMessage("SPREADSHEET_FORMULA_NOT_ALLOWED");
    }

    @Test
    void rejectsEncryptedAndImageOnlyPdf() throws Exception {
        var encrypted = new ByteArrayOutputStream();
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.protect(
                    new StandardProtectionPolicy(
                            "synthetic-owner", "synthetic-password", new AccessPermission()));
            doc.save(encrypted);
        }
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        "locked.pdf",
                                        "application/pdf",
                                        encrypted.toByteArray(),
                                        "x"))
                .hasMessage("ENCRYPTED_PDF");
        var blank = new ByteArrayOutputStream();
        try (var doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(blank);
        }
        assertThatThrownBy(
                        () -> parser.parse("scan.pdf", "application/pdf", blank.toByteArray(), "x"))
                .hasMessage("NO_EXTRACTABLE_TEXT");
    }

    @Test
    void rejectsArchiveExpansionAndMacroPayloads() throws Exception {
        var bomb = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bomb)) {
            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zip.write(new byte[5 * 1024 * 1024]);
            zip.closeEntry();
        }
        assertThatThrownBy(() -> parser.parse("bomb.xlsx", XLSX, bomb.toByteArray(), "x"))
                .hasMessage("ARCHIVE_EXPANSION_LIMIT");
        var macro = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(macro)) {
            zip.putNextEntry(new ZipEntry("word/vbaProject.bin"));
            zip.write(new byte[] {1});
            zip.closeEntry();
        }
        assertThatThrownBy(() -> parser.parse("macro.docx", DOCX, macro.toByteArray(), "x"))
                .hasMessage("UNSAFE_OFFICE_ARCHIVE");
    }

    @Test
    void preservesVietnameseAndRejectsOversizedInput() {
        var bytes = "Khám phá nghề nghiệp và dữ liệu 🔎".getBytes(StandardCharsets.UTF_8);
        assertThat(parser.parse("vi.md", "text/markdown", bytes, "Nguồn").chunks().get(0).text())
                .isEqualTo("Khám phá nghề nghiệp và dữ liệu 🔎");
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        "large.txt",
                                        "text/plain",
                                        new byte[KnowledgeParser.MAX_BYTES + 1],
                                        "x"))
                .hasMessage("INVALID_SOURCE_FILE");
    }

    @Test
    void splitsLongLocatorUnitsWithoutInventingOffsetsOrBreakingUnicode() {
        String text = "a".repeat(2399) + "\ud83d\udd0e" + "b".repeat(3000);
        var parsed =
                parser.parse(
                        "long.txt",
                        "text/plain",
                        text.getBytes(StandardCharsets.UTF_8),
                        "Synthetic");
        assertThat(parsed.chunks()).hasSize(3);
        assertThat(
                        parsed.chunks().stream()
                                .map(KnowledgeParser.Chunk::text)
                                .collect(java.util.stream.Collectors.joining()))
                .isEqualTo(text);
        assertThat(parsed.chunks())
                .allSatisfy(
                        c -> {
                            assertThat(c.text().length()).isLessThanOrEqualTo(2400);
                            assertThat(c.locator().path("lineStart").asInt()).isEqualTo(1);
                            assertThat(c.locator().path("lineEnd").asInt()).isEqualTo(1);
                        });
    }

    @Test
    void rejectsOfficeDtdAndExternalRelationshipsBeforeLibraryExtraction() throws Exception {
        for (String xml :
                new String[] {
                    "<!DOCTYPE x [<!ENTITY a SYSTEM 'file:///synthetic-only'>]><x>&a;</x>",
                    "<Relationships><Relationship TargetMode='External' Target='https://example.test'/></Relationships>"
                }) {
            var bytes = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("word/document.xml"));
                zip.write(xml.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            assertThatThrownBy(
                            () ->
                                    parser.parse(
                                            "unsafe.docx", DOCX, bytes.toByteArray(), "Synthetic"))
                    .hasMessageContaining(
                            xml.startsWith("<!")
                                    ? "UNSAFE_OR_INVALID_OFFICE_XML"
                                    : "EXTERNAL_OFFICE_RELATION_NOT_ALLOWED");
        }
    }

    @Test
    void rejectsDensePdfAtExtractionLimitInsteadOfAccumulatingUnboundedPageText() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var doc = new PDDocument()) {
            var page = new PDPage();
            doc.addPage(page);
            try (var stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 1);
                stream.newLineAtOffset(1, 700);
                for (int i = 0; i < 1100; i++) {
                    stream.showText("a".repeat(100));
                    stream.newLineAtOffset(0, -0.2f);
                }
                stream.endText();
            }
            doc.save(bytes);
        }
        assertThatThrownBy(
                        () ->
                                parser.parse(
                                        "dense.pdf",
                                        "application/pdf",
                                        bytes.toByteArray(),
                                        "Synthetic dense fixture"))
                .isInstanceOf(KnowledgeParser.ParseFailure.class)
                .hasMessage("TEXT_LIMIT_OR_INVALID_TEXT");
    }
}
