package edu.hcmute.careersim.assessment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.value.CoreValues;
import java.util.*;

/** Public explanatory copy aligned with assessmentCatalog.js / profileMetadata.js, not scoring. */
public final class AssessmentCatalogInfo {
    private AssessmentCatalogInfo() {}

    public static JsonNode describe(String code, int actualQuestions) {
        String purpose, use, result;
        int referenceCount, min, max;
        switch (code) {
            case "RIASEC" -> {
                purpose =
                        "Explore interests in activities and work environments across Realistic, Investigative, Artistic, Social, Enterprising and Conventional themes.";
                use = "When you want a broad starting point for career exploration.";
                result = "Relative self-reported interest across six RIASEC dimensions.";
                referenceCount = 42;
                min = 5;
                max = 10;
            }
            case "INTERESTS" -> {
                purpose =
                        "Explore enjoyment of analytical, creative, collaborative and organizing work activities.";
                use = "When you want to identify activities you would enjoy trying.";
                result = "Your current interest ratings across four work-activity themes.";
                referenceCount = 12;
                min = 5;
                max = 5;
            }
            case "ABILITIES" -> {
                purpose =
                        "Reflect on confidence in problem solving, communication, organization and technical learning.";
                use = "When you want to reflect on confidence and choose skills to practise.";
                result = "Self-reported confidence, not tested aptitude or certified competence.";
                referenceCount = 12;
                min = 5;
                max = 5;
            }
            case "WORK_ACTIVITIES" -> {
                purpose =
                        "Explore preferences for working with information, people and practical things.";
                use = "When you want to compare the kinds of day-to-day tasks you prefer.";
                result = "Current preferences across Information, People and Things.";
                referenceCount = 9;
                min = 3;
                max = 3;
            }
            default -> {
                return CoreValues.tree(
                        Map.of(
                                "limitation",
                                "No reviewed public explanation or duration estimate is available for this assessment."));
            }
        }
        var fields = new LinkedHashMap<String, Object>();
        fields.put("purpose", purpose);
        fields.put("whenToUse", use);
        fields.put("resultMeaning", result);
        fields.put(
                "limitations",
                "Exploratory self-report; not diagnosis, aptitude testing, a qualification or a definitive career decision.");
        if (actualQuestions == referenceCount)
            fields.put(
                    "durationEstimate",
                    Map.of(
                            "minMinutes",
                            min,
                            "maxMinutes",
                            max,
                            "basis",
                            "EXISTING_UI_ESTIMATE_NOT_MEASURED"));
        // The UI estimate applies to the reviewed inventory only. Counts always come from SQL.
        return CoreValues.tree(fields);
    }
}
