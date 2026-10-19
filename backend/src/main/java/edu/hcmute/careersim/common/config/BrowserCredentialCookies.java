package edu.hcmute.careersim.common.config;

import edu.hcmute.careersim.identity.service.BrowserAuthentication;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class BrowserCredentialCookies {
    public static final String NAME = "CAREERSIM_AUTH";
    private final boolean secure;

    public BrowserCredentialCookies(
            @Value("${server.servlet.session.cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void write(
            HttpServletRequest req,
            HttpServletResponse res,
            BrowserAuthentication.Credential credential) {
        res.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(NAME, credential == null ? "" : credential.value())
                        .path(req.getContextPath().isEmpty() ? "/" : req.getContextPath())
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Lax")
                        .maxAge(
                                credential == null
                                        ? Duration.ZERO
                                        : Duration.between(Instant.now(), credential.expiresAt()))
                        .build()
                        .toString());
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
