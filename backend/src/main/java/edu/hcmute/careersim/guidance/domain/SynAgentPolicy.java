package edu.hcmute.careersim.guidance.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Deterministic router: the model can phrase an answer but cannot select arbitrary tools. */
@Component
public class SynAgentPolicy {

    private static final Map<String, List<String>> PATH_ALIASES = pathAliases();

    private static Map<String, List<String>> pathAliases() {
        Map<String, List<String>> aliases = new java.util.LinkedHashMap<>();
        aliases.put(
                "BACKEND_DEVELOPMENT",
                List.of("backend", "back-end", "back end", "server", "api", "sql"));
        aliases.put(
                "FRONTEND_DEVELOPMENT",
                List.of("frontend", "front-end", "front end", "ui", "web interface", "giao dien"));
        aliases.put("DATA_ANALYSIS", List.of("data", "analyst", "analytics", "du lieu"));
        aliases.put("SOFTWARE_TESTING", List.of("testing", "tester", "qa", "kiem thu"));
        aliases.put("CYBERSECURITY", List.of("security", "cybersecurity", "cyber", "bao mat"));
        return java.util.Collections.unmodifiableMap(aliases);
    }

    public Decision decide(
            String requestedAction, String message, List<String> rememberedPaths, boolean unsafe) {
        return decide(requestedAction, message, rememberedPaths, unsafe, "AUTO", null);
    }

    public Decision decide(
            String requestedAction,
            String message,
            List<String> rememberedPaths,
            boolean unsafe,
            String requestedLanguage,
            String rememberedLanguage) {
        String normalized = normalize(message);
        String intent = unsafe ? "SAFETY_BOUNDARY" : resolveIntent(requestedAction, normalized);
        List<String> paths = extractPaths(normalized);
        if (paths.isEmpty() && refersToPriorContext(normalized)) {
            paths = rememberedPaths == null ? List.of() : rememberedPaths;
        }
        String language = resolveLanguage(requestedLanguage, message, rememberedLanguage);
        String depth = responseDepth(intent, normalized);

        List<String> tools =
                switch (intent) {
                    case "GET_STARTED" -> List.of("COMPARE_PATHS", "SUGGEST_SIMULATIONS");
                    case "COMPARE_PATHS" ->
                            List.of("READ_EVIDENCE", "COMPARE_PATHS", "SUGGEST_SIMULATIONS");
                    case "LEARNING_RESOURCES" ->
                            List.of("READ_EVIDENCE", "FIND_SKILL_GAPS", "FIND_RESOURCES");
                    case "IDENTIFY_GAPS" ->
                            List.of("READ_EVIDENCE", "FIND_SKILL_GAPS", "SUGGEST_SIMULATIONS");
                    case "DRAFT_PLAN", "EXPLORE_NEXT" ->
                            List.of(
                                    "READ_EVIDENCE",
                                    "FIND_SKILL_GAPS",
                                    "SUGGEST_SIMULATIONS",
                                    "DRAFT_PLAN");
                    case "CAREER_QUESTION" -> List.of("READ_EVIDENCE", "COMPARE_PATHS");
                    case "EXPLAIN_RIASEC", "REVIEW_LATEST" -> List.of("READ_EVIDENCE");
                    default -> List.of();
                };
        return new Decision(intent, tools, paths, language, depth);
    }

    private String resolveIntent(String requestedAction, String normalized) {
        if (!"FREE_TEXT".equals(requestedAction)) {
            return switch (requestedAction) {
                case "NEEDS_EVIDENCE" -> "IDENTIFY_GAPS";
                default -> requestedAction;
            };
        }
        if (containsAny(
                normalized, "compare", "difference", "versus", " vs ", "so sanh", "khac gi")) {
            return "COMPARE_PATHS";
        }
        if (containsAny(
                normalized,
                "learn",
                "resource",
                "course",
                "tutorial",
                "study",
                "hoc gi",
                "tai lieu",
                "khoa hoc")) {
            return "LEARNING_RESOURCES";
        }
        if (containsAny(
                normalized,
                "gap",
                "missing evidence",
                "weak",
                "improve",
                "practice",
                "practise",
                "thieu gi",
                "diem yeu",
                "cai thien",
                "luyen tap")) {
            return "IDENTIFY_GAPS";
        }
        if (containsAny(
                normalized,
                "plan",
                "next step",
                "what should i do next",
                "ke hoach",
                "buoc tiep",
                "lam gi tiep")) {
            return "DRAFT_PLAN";
        }
        if (containsAny(
                normalized, "result", "score", "simulation", "ket qua", "diem so", "mo phong")) {
            return "REVIEW_LATEST";
        }
        if (containsAny(normalized, "riasec", "interest", "so thich", "hung thu")) {
            return "EXPLAIN_RIASEC";
        }
        if (containsAny(
                normalized,
                "get started",
                "where to start",
                "i am new",
                "new here",
                "moi bat dau",
                "chua biet bat dau",
                "bat dau ntn")) {
            return "GET_STARTED";
        }
        if (containsAny(
                normalized,
                "career",
                "role",
                "job",
                "work in",
                "suitable",
                "fit",
                "nghe",
                "vai tro",
                "cong viec",
                "phu hop",
                "hop voi")) {
            return "CAREER_QUESTION";
        }
        return "CAREER_QUESTION";
    }

    private List<String> extractPaths(String normalized) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        if (containsToken(normalized, "be")) matches.add("BACKEND_DEVELOPMENT");
        if (containsToken(normalized, "fe")) matches.add("FRONTEND_DEVELOPMENT");
        PATH_ALIASES.forEach(
                (path, aliases) -> {
                    if (aliases.stream().anyMatch(normalized::contains)) {
                        matches.add(path);
                    }
                });
        return new ArrayList<>(matches);
    }

    private boolean containsToken(String value, String token) {
        return (" " + value.replaceAll("[^a-z0-9_-]", " ") + " ").contains(" " + token + " ");
    }

    private boolean refersToPriorContext(String message) {
        return containsAny(
                message,
                "them",
                "those",
                "these",
                "that path",
                "the first",
                "the second",
                "other one",
                "cai do",
                "nhung cai do",
                "huong dau",
                "huong thu hai",
                "cai con lai");
    }

    private String resolveLanguage(
            String requestedLanguage, String originalMessage, String rememberedLanguage) {
        String explicit =
                requestedLanguage == null ? "AUTO" : requestedLanguage.toUpperCase(Locale.ROOT);
        if ("EN".equals(explicit) || "VI".equals(explicit)) return explicit;
        if (looksVietnamese(originalMessage)) return "VI";
        if ("EN".equals(rememberedLanguage) || "VI".equals(rememberedLanguage)) {
            return rememberedLanguage;
        }
        return "EN";
    }

    private boolean looksVietnamese(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (!Normalizer.normalize(lower, Normalizer.Form.NFD).equals(lower)) return true;
        String normalized = " " + normalize(message) + " ";
        if (containsAny(
                normalized,
                " so sanh ",
                " khac gi ",
                " bat dau ",
                " tai lieu ",
                " khoa hoc ",
                " phu hop ")) {
            return true;
        }
        int matches = 0;
        for (String token :
                List.of(
                        " toi ",
                        " minh ",
                        " em ",
                        " ko ",
                        " khong ",
                        " duoc ",
                        " dc ",
                        " ntn ",
                        " nen ",
                        " nghe ",
                        " viec ",
                        " bat dau ",
                        " gi ",
                        " nao ")) {
            if (normalized.contains(token) && ++matches >= 2) return true;
        }
        return false;
    }

    private String responseDepth(String intent, String normalized) {
        if (containsAny(
                normalized,
                "more detail",
                "explain more",
                "detailed",
                "chi tiet",
                "noi ro hon",
                "giai thich ky")) {
            return "DETAILED";
        }
        return switch (intent) {
            case "GET_STARTED", "COMPARE_PATHS", "DRAFT_PLAN", "CAREER_QUESTION" -> "DETAILED";
            case "REVIEW_LATEST", "EXPLAIN_RIASEC" -> "SHORT";
            default -> "STANDARD";
        };
    }

    private String normalize(String message) {
        String decomposed =
                Normalizer.normalize(message.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String message, String... phrases) {
        for (String phrase : phrases) {
            if (message.contains(phrase)) return true;
        }
        return false;
    }

    public record Decision(
            String intent,
            List<String> tools,
            List<String> paths,
            String responseLanguage,
            String responseDepth) {}
}
