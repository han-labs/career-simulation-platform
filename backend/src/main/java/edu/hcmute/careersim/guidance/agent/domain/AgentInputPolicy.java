package edu.hcmute.careersim.guidance.agent.domain;

import edu.hcmute.careersim.common.value.CoreValues;

public final class AgentInputPolicy {
    private AgentInputPolicy() {}

    public static String minimize(String value, int limit) {
        return CoreValues.text(value, limit)
                .replaceAll(
                        "(?i)[\\w.+-]+@[\\w.-]+|eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+|(?:bearer\\s+|(?:api[_ -]?key|password|secret)\\s*[:=]\\s*)\\S+",
                        "[redacted]");
    }
}
