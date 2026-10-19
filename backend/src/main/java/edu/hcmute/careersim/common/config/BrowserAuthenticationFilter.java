package edu.hcmute.careersim.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.common.exception.ApiException;
import edu.hcmute.careersim.common.response.ErrorResponse;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class BrowserAuthenticationFilter extends OncePerRequestFilter {
    private final BrowserAuthentication auth;
    private final BrowserCredentialCookies cookies;
    private final ActorAccess actors;
    private final ObjectMapper mapper;

    BrowserAuthenticationFilter(
            BrowserAuthentication auth,
            BrowserCredentialCookies cookies,
            ActorAccess actors,
            ObjectMapper mapper) {
        this.auth = auth;
        this.cookies = cookies;
        this.actors = actors;
        this.mapper = mapper;
    }

    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getServletPath();
        String token = cookies.read(req);
        if (token != null) {
            try {
                var principal = auth.verify(token);
                var actor = actors.requireId(principal.id());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + actor.role()))));
                SecurityContextHolder.setContext(context);
                // Mark this request authenticated without persisting authentication in HttpSession.
                // Otherwise OAuth's session strategy can mistake each JWT request for a new login
                // and rotate the browser's CSRF token on ordinary reads.
                new org.springframework.security.web.context
                                .RequestAttributeSecurityContextRepository()
                        .saveContext(context, req, res);
            } catch (ApiException e) {
                SecurityContextHolder.clearContext();
                // A stale cookie must not prevent recovery, CSRF bootstrap or logout.
                if (!(path.startsWith("/v2/auth/")
                        || path.startsWith("/oauth2/")
                        || path.startsWith("/login/oauth2/"))) {
                    res.setStatus(e.getErrorCode().getStatus().value());
                    res.setContentType("application/json");
                    res.setHeader("Cache-Control", "no-store");
                    mapper.writeValue(
                            res.getOutputStream(),
                            ErrorResponse.of(
                                    e.getErrorCode().getCode(),
                                    e.getMessage(),
                                    req.getRequestURI()));
                    return;
                }
            }
        }
        chain.doFilter(req, res);
    }
}
