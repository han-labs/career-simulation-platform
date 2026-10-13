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
        aliases.put("BACKEND_DEVELOPMENT", List.of("backend", "back-end", "back end", "server"));
        aliases.put(
                "FRONTEND_DEVELOPMENT",
                List.of("frontend", "front-end", "front end", "ui", "web interface", "giao dien"));
        aliases.put("DATA_ANALYSIS", List.of("data", "analyst", "analytics", "du lieu"));
        aliases.put("SOFTWARE_TESTING", List.of("testing", "tester", "qa", "kiem thu"));
        aliases.put("CYBERSECURITY", List.of("security", "cybersecurity", "cyber", "bao mat"));
        aliases.put("DEVOPS", List.of("devops", "site reliability", "sre"));
        aliases.put(
                "AI_ENGINEERING",
                List.of(
                        "artificial intelligence",
                        "machine learning",
                        "tri tue nhan tao",
                        "ky su ai"));
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
        SynIntent intent =
                unsafe ? SynIntent.SAFETY_BOUNDARY : resolveIntent(requestedAction, normalized);
        List<String> paths = extractPaths(normalized);
        if (paths.isEmpty() && containsAny(normalized, "sql", "api", "spring", "java"))
            paths = List.of("BACKEND_DEVELOPMENT");
        if (paths.isEmpty()
                && (refersToPriorContext(normalized) || continuesCurrentExploration(intent))) {
            paths = rememberedPaths == null ? List.of() : rememberedPaths;
        }
        if (extractPaths(normalized).isEmpty() && paths.size() > 1) {
            if (containsAny(
                    normalized, "huong dau", "huong thu nhat", "first path", "first option"))
                paths = List.of(paths.get(0));
            else if (containsAny(normalized, "huong thu hai", "second path", "second option"))
                paths = List.of(paths.get(1));
        }
        String language = resolveLanguage(requestedLanguage, message, rememberedLanguage);
        String depth = responseDepth(intent, normalized);

        List<SynToolIdentifier> tools =
                switch (intent) {
                    case GET_STARTED ->
                            List.of(
                                    SynToolIdentifier.COMPARE_PATHS,
                                    SynToolIdentifier.SUGGEST_SIMULATIONS);
                    case COMPARE_PATHS ->
                            List.of(
                                    SynToolIdentifier.READ_EVIDENCE,
                                    SynToolIdentifier.COMPARE_PATHS,
                                    SynToolIdentifier.SUGGEST_SIMULATIONS);
                    case LEARNING_RESOURCES ->
                            List.of(
                                    SynToolIdentifier.READ_EVIDENCE,
                                    SynToolIdentifier.FIND_RESOURCES,
                                    SynToolIdentifier.SUGGEST_SIMULATIONS);
                    case IDENTIFY_GAPS ->
                            List.of(
                                    SynToolIdentifier.READ_EVIDENCE,
                                    SynToolIdentifier.FIND_SKILL_GAPS,
                                    SynToolIdentifier.SUGGEST_SIMULATIONS);
                    case DRAFT_PLAN ->
                            List.of(
                                    SynToolIdentifier.READ_EVIDENCE,
                                    SynToolIdentifier.FIND_SKILL_GAPS,
                                    SynToolIdentifier.SUGGEST_SIMULATIONS,
                                    SynToolIdentifier.DRAFT_PLAN);
                    case EXPLORE_NEXT ->
                            List.of(
                                    SynToolIdentifier.READ_EVIDENCE,
                                    SynToolIdentifier.SUGGEST_SIMULATIONS);
                    case CAREER_QUESTION ->
                            paths.size() >= 2
                                    ? List.of(
                                            SynToolIdentifier.READ_EVIDENCE,
                                            SynToolIdentifier.COMPARE_PATHS)
                                    : List.of(
                                            SynToolIdentifier.READ_EVIDENCE,
                                            SynToolIdentifier.SUGGEST_SIMULATIONS);
                    case EXPLAIN_RIASEC, REVIEW_LATEST, REVIEW_RESULT ->
                            List.of(SynToolIdentifier.READ_EVIDENCE);
                    default -> List.of();
                };
        if (intent != SynIntent.SAFETY_BOUNDARY
                && intent != SynIntent.CONFIRM_PLAN
                && "FREE_TEXT".equals(requestedAction)
                && containsAny(
                        normalized,
                        "what is",
                        "what are",
                        "how does",
                        "explain",
                        "la gi",
                        "giai thich",
                        "tai sao",
                        "huong dan")
                && !containsAny(
                        normalized,
                        "my score",
                        "my result",
                        "my riasec",
                        "my interests",
                        "diem cua",
                        "ket qua cua",
                        "so thich cua")) {
            List<SynToolIdentifier> withKnowledge = new ArrayList<>();
            withKnowledge.add(SynToolIdentifier.SEARCH_KNOWLEDGE);
            withKnowledge.addAll(tools.stream().limit(3).toList());
            tools = List.copyOf(withKnowledge);
        }
        // Goals compose independently: an early primary-intent match must not erase later requests.
        if (intent != SynIntent.SAFETY_BOUNDARY
                && intent != SynIntent.CONFIRM_PLAN
                && "FREE_TEXT".equals(requestedAction)) {
            LinkedHashSet<SynToolIdentifier> capabilities = new LinkedHashSet<>(tools);
            if (wantsResources(normalized)) {
                capabilities.add(SynToolIdentifier.READ_EVIDENCE);
                capabilities.add(SynToolIdentifier.FIND_RESOURCES);
            }
            if (containsAny(
                    normalized, "compare", "difference", "versus", " vs ", "so sanh", "khac gi"))
                capabilities.add(SynToolIdentifier.COMPARE_PATHS);
            if (containsAny(normalized, "simulation", "mo phong", "trai nghiem"))
                capabilities.add(SynToolIdentifier.SUGGEST_SIMULATIONS);
            if (containsAny(normalized, "ke hoach", "lo trinh", "draft", "plan", "roadmap")) {
                capabilities.add(SynToolIdentifier.FIND_SKILL_GAPS);
                capabilities.add(SynToolIdentifier.DRAFT_PLAN);
                capabilities.add(SynToolIdentifier.FIND_RESOURCES);
                capabilities.add(SynToolIdentifier.SEARCH_KNOWLEDGE);
                depth = "DETAILED";
            }
            if (wantsResources(normalized)
                    && containsAny(
                            normalized,
                            "web",
                            "internet",
                            "ben ngoai",
                            "moi nhat",
                            "latest",
                            "external",
                            "nguon khac",
                            "more sources")) {
                capabilities.add(SynToolIdentifier.SEARCH_WEB);
            }
            tools = List.copyOf(capabilities);
        }
        if ("FREE_TEXT".equals(requestedAction)
                && intent != SynIntent.SAFETY_BOUNDARY
                && intent != SynIntent.CONFIRM_PLAN) {
            var relevant = new LinkedHashSet<>(tools);
            boolean generalExplanation =
                    containsAny(
                                    normalized,
                                    "what is",
                                    "what are",
                                    "how does",
                                    "explain",
                                    "la gi",
                                    "giai thich",
                                    "tai sao")
                            && !containsAny(
                                    normalized,
                                    "ket qua",
                                    "bai lam",
                                    "diem cua",
                                    "so thich cua",
                                    "my result",
                                    "my score",
                                    "my riasec",
                                    "my interest",
                                    "my task")
                            && !containsAny(
                                    normalized,
                                    "goi y",
                                    "de xuat",
                                    "recommend",
                                    "suggest",
                                    "ke hoach",
                                    "plan",
                                    "roadmap",
                                    "trai nghiem",
                                    "practice");
            if (generalExplanation) {
                relevant.remove(SynToolIdentifier.READ_EVIDENCE);
                relevant.remove(SynToolIdentifier.SUGGEST_SIMULATIONS);
                relevant.remove(SynToolIdentifier.FIND_RESOURCES);
                relevant.add(SynToolIdentifier.SEARCH_KNOWLEDGE);
            }
            if (intent == SynIntent.COMPARE_PATHS
                    && !containsAny(
                            normalized,
                            "ket qua",
                            "bai lam",
                            "diem",
                            "bang chung",
                            "so thich",
                            "my result",
                            "my evidence",
                            "my score",
                            "my interest",
                            "my skill")) relevant.remove(SynToolIdentifier.READ_EVIDENCE);
            if (intent == SynIntent.COMPARE_PATHS
                    && !containsAny(normalized, "simulation", "mo phong", "trai nghiem", "nen thu"))
                relevant.remove(SynToolIdentifier.SUGGEST_SIMULATIONS);
            if (java.util.regex.Pattern.compile(
                            "(?:dung|khong|don't|do not|no|without)\\s+(?:(?:de xuat|goi y|recommend|suggest|show|any)\\s+)*(?:simulations?|mo phong)")
                    .matcher(normalized)
                    .find()) relevant.remove(SynToolIdentifier.SUGGEST_SIMULATIONS);
            if (paths.stream().anyMatch(p -> p.equals("DEVOPS") || p.equals("AI_ENGINEERING"))) {
                relevant.add(SynToolIdentifier.SEARCH_KNOWLEDGE);
                relevant.add(SynToolIdentifier.SEARCH_WEB);
            }
            tools = List.copyOf(relevant);
        }
        return new Decision(intent, tools, paths, language, depth);
    }

    private SynIntent resolveIntent(String requestedAction, String normalized) {
        if (!"FREE_TEXT".equals(requestedAction)) {
            return SynIntent.fromActionType(requestedAction);
        }
        if (containsAny(
                normalized,
                "save it",
                "save this",
                "confirm save",
                "luu lai",
                "luu ke hoach",
                "xac nhan luu")) {
            return SynIntent.CONFIRM_PLAN;
        }
        if (wantsResources(normalized)) return SynIntent.LEARNING_RESOURCES;
        if (containsAny(normalized, "ke hoach", "lo trinh", "draft", "roadmap"))
            return SynIntent.DRAFT_PLAN;
        if (containsAny(
                normalized,
                "which simulation",
                "what simulation",
                "simulation should",
                "simulation nao phu hop",
                "simulation phu hop",
                "mo phong nao",
                "nen thu mo phong")) {
            return SynIntent.EXPLORE_NEXT;
        }
        if (containsAny(
                normalized, "compare", "difference", "versus", " vs ", "so sanh", "khac gi")) {
            return SynIntent.COMPARE_PATHS;
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
            return SynIntent.LEARNING_RESOURCES;
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
            return SynIntent.IDENTIFY_GAPS;
        }
        if (containsAny(
                normalized,
                "plan",
                "next step",
                "what should i do next",
                "ke hoach",
                "buoc tiep",
                "lam gi tiep")) {
            return SynIntent.DRAFT_PLAN;
        }
        if (containsAny(
                normalized,
                "my result",
                "latest result",
                "result score",
                "my score",
                "ket qua cua",
                "ket qua gan nhat",
                "diem so")) {
            return SynIntent.REVIEW_LATEST;
        }
        if (containsAny(normalized, "riasec", "interest", "so thich", "hung thu")) {
            return SynIntent.EXPLAIN_RIASEC;
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
            return SynIntent.GET_STARTED;
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
            return SynIntent.CAREER_QUESTION;
        }
        return SynIntent.CAREER_QUESTION;
    }

    private boolean wantsResources(String message) {
        return containsAny(
                message,
                "resource",
                "course",
                "tutorial",
                "learn",
                "study",
                "source",
                "hoc gi",
                "tai lieu",
                "khoa hoc",
                "nguon hoc",
                "nguon nay",
                "nguon do",
                "nguon ben ngoai",
                "nguon khac",
                "tai nguyen",
                "hoc them",
                "link hoc");
    }

    private List<String> extractPaths(String normalized) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        if (containsToken(normalized, "be")) matches.add("BACKEND_DEVELOPMENT");
        if (containsToken(normalized, "fe")) matches.add("FRONTEND_DEVELOPMENT");
        if (containsToken(normalized, "ai") || containsToken(normalized, "ml"))
            matches.add("AI_ENGINEERING");
        PATH_ALIASES.forEach(
                (path, aliases) -> {
                    if (aliases.stream().anyMatch(normalized::contains)) {
                        matches.add(path);
                    }
                });
        return matches.stream()
                .sorted(
                        java.util.Comparator.comparingInt(
                                path -> {
                                    int position =
                                            PATH_ALIASES.get(path).stream()
                                                    .mapToInt(normalized::indexOf)
                                                    .filter(i -> i >= 0)
                                                    .min()
                                                    .orElse(
                                                            path.equals("AI_ENGINEERING")
                                                                            && containsToken(
                                                                                    normalized,
                                                                                    "ai")
                                                                    ? normalized.indexOf("ai")
                                                                    : Integer.MAX_VALUE);
                                    String shortName =
                                            path.equals("BACKEND_DEVELOPMENT")
                                                    ? "be"
                                                    : path.equals("FRONTEND_DEVELOPMENT")
                                                            ? "fe"
                                                            : "";
                                    if (!shortName.isEmpty()
                                            && containsToken(normalized, shortName))
                                        position =
                                                Math.min(
                                                        position,
                                                        (" " + normalized + " ")
                                                                .indexOf(" " + shortName + " "));
                                    return position;
                                }))
                .toList();
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

    private boolean continuesCurrentExploration(SynIntent intent) {
        return switch (intent) {
            case IDENTIFY_GAPS, LEARNING_RESOURCES, DRAFT_PLAN, EXPLORE_NEXT -> true;
            default -> false;
        };
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

    private String responseDepth(SynIntent intent, String normalized) {
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
            case GET_STARTED, COMPARE_PATHS, DRAFT_PLAN, CAREER_QUESTION -> "DETAILED";
            case REVIEW_LATEST, REVIEW_RESULT, EXPLAIN_RIASEC, CONFIRM_PLAN -> "SHORT";
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
            SynIntent intent,
            List<SynToolIdentifier> tools,
            List<String> paths,
            String responseLanguage,
            String responseDepth) {

        public Decision withTools(List<SynToolIdentifier> selectedTools) {
            return new Decision(
                    intent, List.copyOf(selectedTools), paths, responseLanguage, responseDepth);
        }
    }
}
