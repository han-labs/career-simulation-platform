package edu.hcmute.careersim.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class LocalDemoAuthenticationFilterTest {

    private final LocalDemoAuthenticationFilter filter =
            new LocalDemoAuthenticationFilter("student@demo.com");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesSeededStudentForLocalGuidanceRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServletPath("/v1/guidance/dashboard");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("student@demo.com");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void doesNotAuthenticateGuidanceRequestForNonLoopbackHost()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("careersim.example.com");
        request.setServletPath("/v1/guidance/dashboard");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
