package edu.hcmute.careersim.guidance.domain;

import java.util.Locale;

/** Closed vocabulary for every intent the bounded Syn agent can execute. */
public enum SynIntent {
    GET_STARTED,
    COMPARE_PATHS,
    LEARNING_RESOURCES,
    IDENTIFY_GAPS,
    DRAFT_PLAN,
    EXPLORE_NEXT,
    CAREER_QUESTION,
    EXPLAIN_RIASEC,
    REVIEW_LATEST,
    REVIEW_RESULT,
    CONFIRM_PLAN,
    SAFETY_BOUNDARY,
    GENERAL;

    public static SynIntent fromActionType(String actionType) {
        if (actionType == null || actionType.isBlank() || "FREE_TEXT".equals(actionType)) {
            return GENERAL;
        }
        if ("NEEDS_EVIDENCE".equals(actionType)) {
            return IDENTIFY_GAPS;
        }
        try {
            return valueOf(actionType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return GENERAL;
        }
    }

    public static SynIntent fromStored(String value, SynIntent fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
