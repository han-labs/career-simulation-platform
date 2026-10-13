package edu.hcmute.careersim.guidance.domain;

import java.util.List;

/** Construct searches from approved topic names, never transmit raw student text to search. */
public final class PublicLearningQuery {
    private PublicLearningQuery() {}

    public static String build(String message, List<String> paths) {
        String normalized = KnowledgeChunker.normalize(message);
        var topics = new java.util.ArrayList<String>();
        for (String word :
                List.of(
                        "spring",
                        "java",
                        "python",
                        "sql",
                        "postgresql",
                        "api",
                        "react",
                        "html",
                        "css",
                        "javascript",
                        "typescript",
                        "pandas",
                        "junit",
                        "playwright",
                        "owasp",
                        "docker",
                        "debugging",
                        "devops",
                        "kubernetes",
                        "terraform",
                        "aws",
                        "azure",
                        "mlops",
                        "pytorch",
                        "tensorflow")) {
            if ((" " + normalized + " ").contains(" " + word + " ")) topics.add(word);
        }
        for (String path : paths)
            if (List.of(
                            "BACKEND_DEVELOPMENT",
                            "FRONTEND_DEVELOPMENT",
                            "DATA_ANALYSIS",
                            "SOFTWARE_TESTING",
                            "CYBERSECURITY",
                            "DEVOPS",
                            "AI_ENGINEERING")
                    .contains(path)) topics.add(path.toLowerCase().replace('_', ' '));
        if (topics.isEmpty()) return "";
        String language =
                normalized.contains("tieng viet")
                        ? " Vietnamese"
                        : normalized.contains("tieng anh") ? " English" : "";
        return String.join(" ", topics.stream().distinct().limit(5).toList())
                + (normalized.contains("so sanh") || normalized.contains("compare")
                        ? " career roles responsibilities skills differences official documentation"
                        : " official beginner learning documentation tutorial")
                + language;
    }
}
