package edu.hcmute.careersim.guidance.domain;

/** Stores only an evidence handle; the use case must reauthorize the record on every read. */
public final class SynMemoryEvidence {
    private SynMemoryEvidence() {}

    public static Long selectedResult(String summary) {
        if (summary == null) return null;
        var match =
                java.util.regex.Pattern.compile("(?:^|\\|)result=(\\d{1,19})(?:\\||$)")
                        .matcher(summary);
        if (!match.find()) return null;
        try {
            long id = Long.parseLong(match.group(1));
            return id > 0 ? id : null;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    public static String remember(String summary, Long result) {
        if (result == null) return summary;
        String bounded = summary.length() > 560 ? summary.substring(0, 560) : summary;
        return bounded + "|result=" + result;
    }

    public static boolean refersToResult(String message) {
        String text = KnowledgeChunker.normalize(message);
        return text.contains("that result")
                || text.contains("this result")
                || text.contains("ket qua do")
                || text.contains("ket qua nay")
                || text.contains("that attempt");
    }
}
