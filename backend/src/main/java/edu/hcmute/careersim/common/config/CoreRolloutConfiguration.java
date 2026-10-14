package edu.hcmute.careersim.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreRolloutConfiguration {
    @Value("${app.core-v2-enabled:false}")
    private boolean core;

    @Value("${app.legacy-write-enabled:false}")
    private boolean legacy;

    @PostConstruct
    void validate() {
        if (core && legacy)
            throw new IllegalStateException(
                    "core-v2-enabled and legacy-write-enabled cannot both be true");
    }
}
