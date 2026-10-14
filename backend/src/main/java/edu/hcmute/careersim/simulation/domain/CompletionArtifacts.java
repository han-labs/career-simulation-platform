package edu.hcmute.careersim.simulation.domain;

import java.util.List;

public final class CompletionArtifacts {
    private CompletionArtifacts() {}

    public static String snippet(String title, List<String> skills) {
        return "Completed the CareerSim simulation \""
                + title
                + "\" by submitting its required tasks."
                + (skills.isEmpty() ? "" : " Practiced: " + String.join(", ", skills) + ".")
                + " This records simulation participation, not professional competence.";
    }
}
