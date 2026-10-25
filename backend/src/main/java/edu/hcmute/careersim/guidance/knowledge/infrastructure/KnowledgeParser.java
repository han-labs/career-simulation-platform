package edu.hcmute.careersim.guidance.knowledge.infrastructure;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.domain.KnowledgeChunker;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

/** Bounded, non-executing extraction. Locators name real source units, never guessed offsets. */
@Component
public class KnowledgeParser {
    public static final String PROFILE = "careersim-source-v1:pdfbox3.0.8:poi5.5.1:units2400";
    public static final int MAX_BYTES = 8 * 1024 * 1024, MAX_TEXT = 100000;

    public record Chunk(
            String section, String text, com.fasterxml.jackson.databind.JsonNode locator) {}

    public record Block(
            String section, String text, com.fasterxml.jackson.databind.JsonNode locator) {}

    public record Parsed(String profile, List<Chunk> chunks, List<Block> blocks) {
        public com.fasterxml.jackson.databind.JsonNode snapshot(String inputHash) {
            return CoreValues.tree(
                    Map.of(
                            "representationVersion",
                            1,
                            "inputHash",
                            inputHash,
                            "parserProfile",
                            profile,
                            "textUnit",
                            "UTF-16",
                            "blocks",
                            blocks,
                            "warnings",
                            List.of()));
        }
    }

    public static final class ParseFailure extends RuntimeException {
        public ParseFailure(String code) {
            super(code);
        }
    }

    public String validateType(String name, String mime, byte[] bytes) {
        if (name == null
                || name.contains("/")
                || name.contains("\\")
                || name.chars().anyMatch(Character::isISOControl)
                || name.length() > 255
                || bytes == null
                || bytes.length == 0
                || bytes.length > MAX_BYTES) throw new ParseFailure("INVALID_SOURCE_FILE");
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String expected =
                switch (extension) {
                    case "txt" -> "text/plain";
                    case "md" -> "text/markdown";
                    case "pdf" -> "application/pdf";
                    case "docx" ->
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    case "xlsx" ->
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    default -> "";
                };
        if (expected.isEmpty() || !expected.equals(mime))
            throw new ParseFailure("UNSUPPORTED_OR_MISMATCHED_MIME");
        if (extension.equals("pdf")
                && (bytes.length < 5
                        || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")))
            throw new ParseFailure("INVALID_PDF_SIGNATURE");
        if (Set.of("docx", "xlsx").contains(extension)
                && (bytes.length < 4
                        || bytes[0] != 'P'
                        || bytes[1] != 'K'
                        || bytes[2] != 3
                        || bytes[3] != 4)) throw new ParseFailure("ENCRYPTED_OR_INVALID_OFFICE");
        if (Set.of("txt", "md").contains(extension)) text(bytes);
        return extension;
    }

    public Parsed parse(String name, String mime, byte[] bytes, String title) {
        String ext = validateType(name, mime, bytes);
        var out = new ArrayList<Chunk>();
        var blocks = new ArrayList<Block>();
        try {
            switch (ext) {
                case "txt", "md" -> {
                    var lines =
                            text(bytes).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
                    var block = new StringBuilder();
                    int start = 1;
                    boolean fenced = false;
                    for (int i = 0; i < lines.length; i++) {
                        if (lines[i].stripLeading().startsWith("```")) fenced = !fenced;
                        if (lines[i].isBlank() && !fenced) {
                            add(
                                    out,
                                    blocks,
                                    title,
                                    block.toString(),
                                    Map.of("format", ext, "lineStart", start, "lineEnd", i));
                            block.setLength(0);
                            start = i + 2;
                        } else block.append(lines[i]).append('\n');
                    }
                    if (fenced) throw new ParseFailure("UNCLOSED_CODE_FENCE");
                    add(
                            out,
                            blocks,
                            title,
                            block.toString(),
                            Map.of("format", ext, "lineStart", start, "lineEnd", lines.length));
                }
                case "pdf" -> {
                    try (var doc = Loader.loadPDF(bytes)) {
                        if (doc.isEncrypted()) throw new ParseFailure("ENCRYPTED_PDF");
                        if (doc.getNumberOfPages() > 100) throw new ParseFailure("PDF_PAGE_LIMIT");
                        var stripper = new PDFTextStripper();
                        for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                            stripper.setStartPage(page);
                            stripper.setEndPage(page);
                            add(
                                    out,
                                    blocks,
                                    "Page " + page,
                                    boundedPage(stripper, doc),
                                    Map.of("format", "pdf", "page", page));
                        }
                    }
                }
                case "docx" -> {
                    zipCheck(bytes, "word/document.xml");
                    try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                        int paragraph = 0, table = 0;
                        for (var element : doc.getBodyElements()) {
                            if (element instanceof XWPFParagraph p)
                                add(
                                        out,
                                        blocks,
                                        title,
                                        p.getText(),
                                        Map.of("format", "docx", "paragraph", ++paragraph));
                            else if (element instanceof XWPFTable t) {
                                table++;
                                int row = 0;
                                for (var r : t.getRows())
                                    add(
                                            out,
                                            blocks,
                                            title,
                                            r.getTableCells().stream()
                                                    .map(XWPFTableCell::getText)
                                                    .reduce((a, b) -> a + " | " + b)
                                                    .orElse(""),
                                            Map.of("format", "docx", "table", table, "row", ++row));
                            }
                        }
                    }
                }
                case "xlsx" -> {
                    zipCheck(bytes, "xl/workbook.xml");
                    try (var book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                        if (book.getNumberOfSheets() > 30)
                            throw new ParseFailure("SPREADSHEET_SHEET_LIMIT");
                        int cells = 0;
                        for (var sheet : book) {
                            if (sheet.getLastRowNum() > 10000)
                                throw new ParseFailure("SPREADSHEET_ROW_LIMIT");
                            for (var row : sheet) {
                                var values = new ArrayList<String>();
                                for (var cell : row) {
                                    if (++cells > 20000)
                                        throw new ParseFailure("SPREADSHEET_CELL_LIMIT");
                                    if (cell.getCellType() == CellType.FORMULA)
                                        throw new ParseFailure("SPREADSHEET_FORMULA_NOT_ALLOWED");
                                    String value =
                                            switch (cell.getCellType()) {
                                                case STRING -> cell.getStringCellValue();
                                                case NUMERIC ->
                                                        Double.toString(cell.getNumericCellValue());
                                                case BOOLEAN ->
                                                        Boolean.toString(
                                                                cell.getBooleanCellValue());
                                                case BLANK -> "";
                                                default ->
                                                        throw new ParseFailure(
                                                                "UNSUPPORTED_SPREADSHEET_CELL");
                                            };
                                    values.add(cell.getAddress().formatAsString() + ": " + value);
                                }
                                add(
                                        out,
                                        blocks,
                                        sheet.getSheetName(),
                                        String.join(" | ", values),
                                        Map.of(
                                                "format",
                                                "xlsx",
                                                "sheet",
                                                sheet.getSheetName(),
                                                "row",
                                                row.getRowNum() + 1));
                            }
                        }
                    }
                }
                default -> throw new ParseFailure("UNSUPPORTED_SOURCE");
            }
        } catch (ParseFailure failure) {
            throw failure;
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException encrypted) {
            throw new ParseFailure("ENCRYPTED_PDF");
        } catch (Exception invalid) {
            throw new ParseFailure("SOURCE_PARSE_FAILED");
        }
        if (out.isEmpty()) throw new ParseFailure("NO_EXTRACTABLE_TEXT");
        return new Parsed(PROFILE, List.copyOf(out), List.copyOf(blocks));
    }

    private void add(
            List<Chunk> out,
            List<Block> blocks,
            String title,
            String content,
            Map<String, Object> locator) {
        if (content.isBlank()) return;
        if (content.indexOf('\0') >= 0 || content.length() > MAX_TEXT)
            throw new ParseFailure("TEXT_LIMIT_OR_INVALID_TEXT");
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (blocks.stream().mapToInt(b -> b.text().length()).sum() + normalized.length() > MAX_TEXT)
            throw new ParseFailure("SOURCE_TEXT_OR_CHUNK_LIMIT");
        blocks.add(new Block(title, normalized, CoreValues.tree(locator)));
        try {
            int part = 0;
            // A PDF page or spreadsheet row is a locator unit, not an editorial paragraph.
            // Preserve its exact text in deterministic bounded parts without cutting surrogate
            // pairs.
            List<KnowledgeChunker.Chunk> chunks;
            if (content.length() <= 2400) chunks = KnowledgeChunker.split(title, content);
            else {
                chunks = new ArrayList<>();
                for (int start = 0; start < content.length(); ) {
                    int end = Math.min(start + 2400, content.length());
                    if (end < content.length()
                            && Character.isHighSurrogate(content.charAt(end - 1))) end--;
                    String piece = content.substring(start, end).strip();
                    if (!piece.isBlank()) chunks.add(new KnowledgeChunker.Chunk(title, piece));
                    start = end;
                }
            }
            for (var c : chunks) {
                var l = new LinkedHashMap<>(locator);
                l.put("part", ++part);
                out.add(new Chunk(c.section(), c.text(), CoreValues.tree(l)));
            }
        } catch (IllegalArgumentException bad) {
            throw new ParseFailure("SOURCE_BLOCK_LIMIT");
        }
        if (out.size() > 80 || out.stream().mapToInt(c -> c.text().length()).sum() > MAX_TEXT)
            throw new ParseFailure("SOURCE_TEXT_OR_CHUNK_LIMIT");
    }

    private String text(byte[] bytes) {
        try {
            String t =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes))
                            .toString();
            if (t.indexOf('\0') >= 0) throw new ParseFailure("INVALID_UTF8_TEXT");
            return t.startsWith("\uFEFF") ? t.substring(1) : t;
        } catch (CharacterCodingException invalid) {
            throw new ParseFailure("INVALID_UTF8_TEXT");
        }
    }

    private String boundedPage(PDFTextStripper stripper, org.apache.pdfbox.pdmodel.PDDocument doc)
            throws IOException {
        var text = new StringWriter();
        stripper.writeText(
                doc,
                new Writer() {
                    @Override
                    public void write(char[] chars, int offset, int length) {
                        if (text.getBuffer().length() + length > MAX_TEXT)
                            throw new ParseFailure("TEXT_LIMIT_OR_INVALID_TEXT");
                        text.write(chars, offset, length);
                    }

                    @Override
                    public void flush() {}

                    @Override
                    public void close() {}
                });
        return text.toString();
    }

    private void zipCheck(byte[] bytes, String required) throws IOException {
        long total = 0;
        int entries = 0;
        boolean found = false;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (++entries > 5000
                        || name.startsWith("/")
                        || name.contains("..")
                        || name.contains("\\")
                        || lower.contains("vbaproject")
                        || lower.contains("embeddings/")
                        || lower.contains("externallinks/")
                        || lower.endsWith(".bin")) throw new ParseFailure("UNSAFE_OFFICE_ARCHIVE");
                if (name.equals(required)) found = true;
                int n;
                long expanded = 0;
                ByteArrayOutputStream rel =
                        lower.endsWith(".rels") || lower.endsWith(".xml")
                                ? new ByteArrayOutputStream()
                                : null;
                while ((n = zip.read(buffer)) != -1) {
                    total += n;
                    expanded += n;
                    if (total > 16L * 1024 * 1024
                            || expanded > 8L * 1024 * 1024
                            || total > Math.max(1024L * 1024, bytes.length * 100L))
                        throw new ParseFailure("ARCHIVE_EXPANSION_LIMIT");
                    if (rel != null) rel.write(buffer, 0, n);
                }
                if (rel != null) {
                    try {
                        var factory = javax.xml.parsers.SAXParserFactory.newInstance();
                        factory.setNamespaceAware(true);
                        factory.setFeature(
                                "http://apache.org/xml/features/disallow-doctype-decl", true);
                        factory.setFeature(
                                "http://xml.org/sax/features/external-general-entities", false);
                        factory.setFeature(
                                "http://xml.org/sax/features/external-parameter-entities", false);
                        factory.newSAXParser()
                                .parse(
                                        new ByteArrayInputStream(rel.toByteArray()),
                                        new org.xml.sax.helpers.DefaultHandler() {
                                            @Override
                                            public void startElement(
                                                    String uri,
                                                    String local,
                                                    String qualified,
                                                    org.xml.sax.Attributes attributes) {
                                                for (int i = 0; i < attributes.getLength(); i++)
                                                    if ("TargetMode"
                                                                    .equals(
                                                                            attributes.getLocalName(
                                                                                    i))
                                                            && "External"
                                                                    .equalsIgnoreCase(
                                                                            attributes.getValue(i)))
                                                        throw new ParseFailure(
                                                                "EXTERNAL_OFFICE_RELATION_NOT_ALLOWED");
                                            }
                                        });
                    } catch (ParseFailure failure) {
                        throw failure;
                    } catch (Exception invalid) {
                        throw new ParseFailure("UNSAFE_OR_INVALID_OFFICE_XML");
                    }
                }
            }
        }
        if (!found) throw new ParseFailure("OFFICE_FORMAT_MISMATCH");
    }
}
