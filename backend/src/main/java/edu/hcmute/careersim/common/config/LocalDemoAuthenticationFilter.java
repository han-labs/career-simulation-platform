package edu.hcmute.careersim.common.config;

import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.identity.access.AccountPrincipal;
import edu.hcmute.careersim.identity.access.ActorAccess;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Explicit opt-in on local profile only; reuses an ACTIVE Administrator without credential writes.
 */
final class LocalDemoAuthenticationFilter extends OncePerRequestFilter {
    private final ActorAccess actors;

    LocalDemoAuthenticationFilter(ActorAccess actors) {
        this.actors = actors;
    }

    private boolean adminPath(String path) {
        return path.equals("/v2/me")
                || path.equals("/v2/admin/agent-platform/options")
                || path.equals("/v2/admin/agent-usage")
                || path.equals("/v2/admin/agent-role-bindings")
                || List.of(
                                "/v2/admin/knowledge-bases",
                                "/v2/admin/agents",
                                "/v2/admin/agent-executions")
                        .stream()
                        .anyMatch(root -> path.equals(root) || path.startsWith(root + "/"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String host = request.getServerName();
        boolean loopback =
                "localhost".equalsIgnoreCase(host)
                        || "127.0.0.1".equals(host)
                        || "::1".equals(host)
                        || "0:0:0:0:0:0:0:1".equals(host);
        return !loopback || !adminPath(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                var actor = actors.findActiveAdministrator();
                if (actor.isPresent() && "ADMIN".equals(actor.get().role())) {
                    var admin = actor.get();
                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    new AccountPrincipal(admin.id(), admin.email()),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    new RequestAttributeSecurityContextRepository()
                            .saveContext(SecurityContextHolder.getContext(), request, response);
                }
            } catch (ApiException unavailable) {
                // Deleted/inactive/changed-role identities fail closed. No fallback promotion.
            }
        }
        chain.doFilter(request, response);
    }
}
