package edu.hcmute.careersim.guidance.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KnowledgeChunker {
    private KnowledgeChunker() {}

    public record Chunk(String section, String text) {}

    public static List<Chunk> split(String title, String content) {
        if (content == null || content.isBlank() || content.length() > 100000)
            throw new IllegalArgumentException("Document must contain 1–100000 characters.");
        List<Chunk> result = new ArrayList<>();
        String section = title;
        StringBuilder current = new StringBuilder();
        // Preserve fenced code and tables as blocks. Oversized blocks require editorial splitting.
        List<String> blocks = new ArrayList<>();
        StringBuilder block = new StringBuilder();
        boolean fenced = false;
        for (String line : content.replace("\r", "").split("\n", -1)) {
            if (line.stripLeading().startsWith("```")) fenced = !fenced;
            if (line.isBlank() && !fenced) {
                if (!block.isEmpty()) {
                    blocks.add(block.toString().trim());
                    block.setLength(0);
                }
            } else block.append(line).append("\n");
        }
        if (fenced) throw new IllegalArgumentException("Unclosed code fence.");
        if (!block.isEmpty()) blocks.add(block.toString().trim());
        for (String paragraph : blocks) {
            if (paragraph.length() > 2400)
                throw new IllegalArgumentException(
                        "Split oversized paragraphs/code/tables before publishing.");
            if (paragraph.startsWith("#")) {
                if (!current.isEmpty()) {
                    result.add(new Chunk(section, current.toString().trim()));
                    current.setLength(0);
                }
                section = paragraph.lines().findFirst().orElse(title).replaceFirst("^#+\\s*", "");
                if (section.length() > 240)
                    throw new IllegalArgumentException("Section title is too long.");
            }
            if (current.length() + paragraph.length() + 2 > 2400 && !current.isEmpty()) {
                result.add(new Chunk(section, current.toString().trim()));
                current.setLength(0);
            }
            current.append(paragraph).append("\n\n");
        }
        if (!current.isEmpty()) result.add(new Chunk(section, current.toString().trim()));
        if (result.size() > 80) throw new IllegalArgumentException("Too many chunks.");
        return List.copyOf(result);
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9_ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
