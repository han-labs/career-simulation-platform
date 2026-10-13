package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dao.SynTurnDao;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableScheduling
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.syn.cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class SynRetentionCleanup {
    private final SynTurnDao turns;

    @Scheduled(initialDelay = 60000, fixedDelay = 3600000)
    @Transactional
    public void cleanup() {
        turns.cleanup();
    }
}
