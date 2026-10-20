package edu.hcmute.careersim.guidance.knowledge.domain;

import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.Upload;
import java.net.URI;
import java.util.Set;

public final class KnowledgeSourcePolicy {
    private KnowledgeSourcePolicy() {}

    public static void validate(Upload r) {
        if (r == null
                || r.requestId() == null
                || !r.reviewed()
                || r.assetKey() == null
                || !r.assetKey().matches("[a-z0-9][a-z0-9-]{0,99}"))
            CoreValues.invalid(
                    "Reviewed provenance, request ID and a valid asset key are required.");
        CoreValues.text(r.title(), 240);
        CoreValues.text(r.license(), 240);
        CoreValues.text(r.track(), 60);
        if (r.language() == null || !Set.of("EN", "VI").contains(r.language()))
            CoreValues.invalid("Knowledge language must be EN or VI.");
        if (r.sourceUri() != null && !r.sourceUri().isBlank()) {
            try {
                var u = URI.create(r.sourceUri());
                if (r.sourceUri().length() > 600
                        || !"https".equals(u.getScheme())
                        || u.getHost() == null
                        || u.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException bad) {
                CoreValues.invalid(
                        "Source URI must be an HTTPS provenance reference without credentials.");
            }
        }
    }

    public static int topK(Integer k) {
        int value = k == null ? 5 : k;
        if (value < 1 || value > 10) CoreValues.invalid("Top K must be between 1 and 10.");
        return value;
    }

    public static String query(String q) {
        return CoreValues.text(q, 2000).strip();
    }
}
