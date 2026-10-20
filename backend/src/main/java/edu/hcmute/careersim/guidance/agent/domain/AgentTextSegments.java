package edu.hcmute.careersim.guidance.agent.domain;

import com.fasterxml.jackson.core.*;
import java.util.function.Consumer;

/**
 * Only semantic root-text segments; native protocol, arguments and private thoughts never escape.
 */
public final class AgentTextSegments {
    private final StringBuilder raw = new StringBuilder();
    private int emitted;
    private final Consumer<String> sink;
    private final java.util.List<Integer> scores;

    public AgentTextSegments(Consumer<String> sink) {
        this(java.util.List.of(), sink);
    }

    public AgentTextSegments(java.util.List<Integer> scores, Consumer<String> sink) {
        this.sink = sink;
        this.scores = java.util.List.copyOf(scores);
    }

    public void accept(String delta) {
        if (delta == null) return;
        raw.append(delta);
        if (raw.length() > 16000) throw new IllegalArgumentException("Output budget exceeded");
        String value = partialText(raw.toString());
        if (value == null || value.length() <= emitted) return;
        int end = value.length();
        while (end > emitted && ".!?\n".indexOf(value.charAt(end - 1)) < 0) end--;
        if (end <= emitted) return;
        edu.hcmute.careersim.guidance.domain.SynReplyValidator.checkEvidence(
                new edu.hcmute.careersim.guidance.service.SynGuidanceProvider.AiReply(
                        value.substring(0, end), java.util.List.of()),
                scores);
        if (end > 4000) throw new IllegalArgumentException("Text too long");
        sink.accept(value.substring(emitted, end));
        emitted = end;
    }

    private String partialText(String input) {
        try (var p = new JsonFactory().createParser(input)) {
            if (p.nextToken() != JsonToken.START_OBJECT) return null;
            while (p.nextToken() == JsonToken.FIELD_NAME) {
                String key = p.currentName();
                if ("text".equals(key)) {
                    int colon = input.indexOf(':', (int) p.currentTokenLocation().getCharOffset());
                    if (colon < 0) return null;
                    int start = colon + 1;
                    while (start < input.length() && Character.isWhitespace(input.charAt(start)))
                        start++;
                    if (start >= input.length() || input.charAt(start) != '"') return null;
                    String suffix = input.substring(start);
                    boolean escape = false;
                    int cut = 1;
                    for (int i = 1; i < suffix.length(); i++) {
                        char c = suffix.charAt(i);
                        if (!escape && c == '"') {
                            cut = i;
                            break;
                        }
                        if (c == '\\' && !escape) {
                            escape = true;
                            continue;
                        }
                        if (escape) {
                            if (c == 'u') {
                                if (i + 4 >= suffix.length()) break;
                                i += 4;
                            }
                            escape = false;
                        }
                        cut = i + 1;
                    }
                    String safe = suffix.substring(0, cut) + "\"";
                    try (var q = new JsonFactory().createParser(safe)) {
                        q.nextToken();
                        return q.getText();
                    }
                }
                p.nextToken();
                p.skipChildren();
            }
        } catch (java.io.IOException invalid) {
            return null;
        }
        return null;
    }
}
