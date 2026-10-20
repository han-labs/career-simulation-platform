package edu.hcmute.careersim.guidance.agent.infrastructure;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class AgentExecutionWorkers {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService agentExecutionExecutor() {
        var sequence = new AtomicInteger();
        return new ThreadPoolExecutor(
                2,
                2,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(16),
                r -> {
                    var thread = new Thread(r, "careersim-agent-" + sequence.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }
}
