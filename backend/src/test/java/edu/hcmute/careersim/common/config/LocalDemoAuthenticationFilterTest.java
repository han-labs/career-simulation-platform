package edu.hcmute.careersim.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.exception.ErrorCode;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;

class LocalDemoAuthenticationFilterTest {
    ActorAccess actors = mock(ActorAccess.class);
    LocalDemoAuthenticationFilter filter = new LocalDemoAuthenticationFilter(actors);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    MockHttpServletRequest request(String path) {
        var r = new MockHttpServletRequest();
        r.setServerName("localhost");
        r.setServletPath(path);
        return r;
    }

    @Test
    void usesExistingActiveAdminWithoutIssuingCookiesOrCredentials() throws Exception {
        when(actors.findActiveAdministrator())
                .thenReturn(
                        Optional.of(
                                new ActorAccess.Actor(
                                        5, "uat@example.invalid", "UAT", "ADMIN", 0, 0)));
        var r = request("/v2/admin/knowledge-bases");
        var res = new MockHttpServletResponse();
        filter.doFilter(r, res, new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(res.getCookies()).isEmpty();
        assertThat(
                        new org.springframework.security.web.context
                                        .RequestAttributeSecurityContextRepository()
                                .containsContext(r))
                .isTrue();
        verify(actors).findActiveAdministrator();
    }

    @Test
    void failsClosedForMissingInactiveOrWrongRoleWithoutProvisioning() throws Exception {
        var r = request("/v2/me");
        when(actors.findActiveAdministrator()).thenReturn(Optional.empty());
        filter.doFilter(r, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        when(actors.findActiveAdministrator())
                .thenReturn(
                        Optional.of(
                                new ActorAccess.Actor(
                                        5, "x@example.invalid", "Student", "STUDENT", 0, 0)));
        filter.doFilter(r, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        when(actors.findActiveAdministrator()).thenThrow(new ApiException(ErrorCode.FORBIDDEN));
        filter.doFilter(r, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void limitsThePrincipalToTheThreeAdminProductAreas() throws Exception {
        for (String path :
                List.of(
                        "/v2/admin/accounts",
                        "/v2/admin/assessment-types",
                        "/v2/auth/login",
                        "/v2/agents",
                        "/v1/guidance/dashboard",
                        "/v2/admin/knowledge-bases-other"))
            filter.doFilter(request(path), new MockHttpServletResponse(), new MockFilterChain());
        verifyNoInteractions(actors);
    }

    @Test
    void refusesRemoteHostAndNeverReplacesAnExistingJwtActor() throws Exception {
        var r = request("/v2/admin/agents");
        r.setServerName("careersim.example.com");
        filter.doFilter(r, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        r.setServerName("localhost");
        var existing =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "student",
                        null,
                        List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_STUDENT")));
        SecurityContextHolder.getContext().setAuthentication(existing);
        filter.doFilter(r, new MockHttpServletResponse(), new MockFilterChain());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(actors);
    }

    @Test
    void onlyLocalProfileCanEnableAndProductionAlwaysWins() {
        var env = new org.springframework.mock.env.MockEnvironment();
        assertThat(SecurityConfig.localAdminEnabled(env)).isFalse();
        env.setActiveProfiles("local");
        assertThat(SecurityConfig.localAdminEnabled(env)).isFalse();
        env.setProperty("app.local-demo-mode", "true");
        for (String[] profiles :
                List.of(
                        new String[] {"prod"},
                        new String[] {"production"},
                        new String[] {"local", "prod"},
                        new String[] {"local", "production"},
                        new String[] {"local-demo"},
                        new String[] {"test"})) {
            env.setActiveProfiles(profiles);
            assertThat(SecurityConfig.localAdminEnabled(env)).isFalse();
        }
        env.setActiveProfiles("local");
        assertThat(SecurityConfig.localAdminEnabled(env)).isTrue();
    }
}
