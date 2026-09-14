package edu.hcmute.careersim.common.config;

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
import org.springframework.web.filter.OncePerRequestFilter;

/** Supplies the seeded student identity only for explicitly enabled localhost demo requests. */
final class LocalDemoAuthenticationFilter extends OncePerRequestFilter {

    private static final String GUIDANCE_PATH = "/v1/guidance";
    private final String demoEmail;

    LocalDemoAuthenticationFilter(String demoEmail) {
        this.demoEmail = demoEmail;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(GUIDANCE_PATH)
                || !isLoopbackHost(request.getServerName());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            demoEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoopbackHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host);
    }
}
