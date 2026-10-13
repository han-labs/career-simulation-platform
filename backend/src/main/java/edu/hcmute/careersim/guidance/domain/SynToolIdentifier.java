package edu.hcmute.careersim.guidance.domain;

/** Fixed server-owned tool allowlist for the bounded Syn agent. */
public enum SynToolIdentifier {
    SEARCH_WEB(
            "Find public learning sources when reviewed resources are insufficient; no personal data or arbitrary URL fetch."),
    SEARCH_KNOWLEDGE(
            "Retrieve reviewed knowledge passages with immutable citation IDs; never fetch arbitrary URLs."),
    READ_EVIDENCE(
            "Read the authenticated student's selected assessment and completed simulation evidence."),
    COMPARE_PATHS(
            "Compare reviewed career-path profiles against the student's available interest and task evidence."),
    FIND_RESOURCES(
            "Find reviewed learning resources for the selected career paths or evidenced skill needs."),
    SUGGEST_SIMULATIONS(
            "Find published simulations that can provide another bounded career exploration experience."),
    FIND_SKILL_GAPS(
            "Identify skills that need more evidence using reviewed simulation-task skill mappings."),
    DRAFT_PLAN(
            "Create a server-owned, unsaved exploration plan draft from the available evidence.");

    private final String description;

    SynToolIdentifier(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
