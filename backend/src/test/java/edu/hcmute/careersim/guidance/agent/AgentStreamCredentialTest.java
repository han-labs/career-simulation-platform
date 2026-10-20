package edu.hcmute.careersim.guidance.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.guidance.agent.controller.AgentEventStreamController;
import edu.hcmute.careersim.guidance.agent.dto.StudentAgentContracts.Run;
import edu.hcmute.careersim.guidance.agent.service.StudentAgentService;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class AgentStreamCredentialTest {
    @Test
    void expiredOrRevokedCredentialStopsOpenStreamBeforeReplayEvenWhenAccountStillActive() {
        var service = mock(StudentAgentService.class);
        var auth = mock(BrowserAuthentication.class);
        var cookies = mock(BrowserCredentialCookies.class);
        var actors = mock(ActorAccess.class);
        var scheduler = mock(ScheduledExecutorService.class);
        var scheduled = mock(ScheduledFuture.class);
        var tick = new AtomicReference<Runnable>();
        var principal = new AccountPrincipal(7, "synthetic@example.test");
        var request = new MockHttpServletRequest();
        when(cookies.read(request)).thenReturn("synthetic-browser-credential");
        when(auth.verify(anyString()))
                .thenReturn(principal)
                .thenThrow(new ApiException(ErrorCode.UNAUTHORIZED));
        when(actors.student(principal))
                .thenReturn(
                        new ActorAccess.Actor(
                                7, "synthetic@example.test", "Synthetic", "STUDENT", 0, 0));
        when(service.read(anyLong(), any())).thenReturn(mock(Run.class));
        doAnswer(
                        call -> {
                            tick.set(call.getArgument(0));
                            return scheduled;
                        })
                .when(scheduler)
                .scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any());
        var controller = new AgentEventStreamController(service, auth, cookies, actors, scheduler);
        var emitter =
                controller.events(UUID.randomUUID(), 0, request, new MockHttpServletResponse());
        assertThat(emitter).isNotNull();
        tick.get().run();
        verify(scheduled).cancel(false);
        verify(service, never()).events(anyLong(), any(), anyLong());
        verify(service, times(1)).read(anyLong(), any());
    }
}
