package edu.hcmute.careersim.guidance.domain;

import java.util.List;

/** Bounded resource skill handles; resources are always reloaded from the reviewed catalog. */
public final class SynResourceMemory {
    private SynResourceMemory() {}

    public static List<String> read(String summary) {
        if (summary == null) return List.of();
        var match =
                java.util.regex.Pattern.compile("(?:^|\\|)resources=([a-z_,]{1,180})(?:\\||$)")
                        .matcher(summary);
        return match.find()
                ? java.util.Arrays.stream(match.group(1).split(","))
                        .filter(s -> s.matches("[a-z_]{1,50}"))
                        .limit(3)
                        .toList()
                : List.of();
    }

    public static List<String> resolve(String message, String summary) {
        String text = KnowledgeChunker.normalize(message);
        if (!(text.contains("resource")
                || text.contains("tai nguyen")
                || text.contains("tai lieu")
                || text.contains("nguon")
                || text.contains("khoa hoc"))) return List.of();
        List<String> skills = read(summary);
        if (skills.isEmpty()) return skills;
        if (text.contains("thu hai") || text.contains("second"))
            return skills.size() > 1 ? List.of(skills.get(1)) : List.of();
        if (text.contains("dau tien")
                || text.contains("first")
                || text.contains("nay")
                || text.contains("this")) return List.of(skills.get(0));
        return List.of();
    }

    public static String remember(String summary, List<String> skills) {
        String value =
                String.join(
                        ",",
                        skills.stream()
                                .filter(s -> s.matches("[a-z_]{1,50}"))
                                .distinct()
                                .limit(3)
                                .toList());
        return value.isEmpty()
                ? summary
                : summary.substring(0, Math.min(370, summary.length())) + "|resources=" + value;
    }
}
