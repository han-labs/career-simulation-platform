package edu.hcmute.careersim.identity.controller;

import edu.hcmute.careersim.common.config.BrowserCredentialCookies;
import edu.hcmute.careersim.common.config.GoogleOAuthFlow;
import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.*;
import edu.hcmute.careersim.identity.dto.IdentityContracts.*;
import edu.hcmute.careersim.identity.dto.OAuthLinkIntent;
import edu.hcmute.careersim.identity.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class IdentityController {
    private final IdentityService identity;
    private final ConsentService consents;
    private final ActorAccess actors;
    private final CsrfTokenRepository csrf;
    private final BrowserAuthentication browser;
    private final BrowserCredentialCookies cookies;

    @Value("${app.auth.google.enabled:false}")
    private boolean googleEnabled;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    @GetMapping("/auth/providers")
    public ApiResponse<?> providers() {
        return ApiResponse.success(
                Map.of(
                        "google",
                        googleEnabled,
                        "localWorkspace",
                        edu.hcmute.careersim.common.config.SecurityConfig.localAdminEnabled(
                                environment)));
    }

    @PostMapping("/auth/google/link")
    public ApiResponse<?> link(Principal p, HttpServletRequest req) {
        if (!googleEnabled)
            throw new edu.hcmute.careersim.common.exception.ApiException(
                    edu.hcmute.careersim.common.exception.ErrorCode.SERVICE_UNAVAILABLE);
        long user = actors.require(p).id();
        req.getSession()
                .setAttribute(
                        OAuthLinkIntent.KEY,
                        new OAuthLinkIntent(
                                user,
                                browser.version(user),
                                GoogleOAuthFlow.hash(cookies.read(req)),
                                java.time.Instant.now().plusSeconds(300)));
        return ApiResponse.success(Map.of("authorizationUrl", "/api/oauth2/authorization/google"));
    }

    @GetMapping("/auth/csrf")
    public ApiResponse<Map<String, String>> csrf(CsrfToken token) {
        return ApiResponse.success(
                Map.of(
                        "token",
                        token.getToken(),
                        "headerName",
                        token.getHeaderName(),
                        "policyVersion",
                        "careersim-purpose-v1"));
    }

    @PostMapping("/auth/register")
    public ApiResponse<Account> register(@Valid @RequestBody Registration r) {
        return ApiResponse.success(identity.register(r));
    }

    @PostMapping("/auth/login")
    public ApiResponse<Account> login(
            @Valid @RequestBody Login r, HttpServletRequest request, HttpServletResponse response) {
        var p = identity.authenticate(r);
        var a = actors.requireId(p.id());
        if (request.getSession(false) != null) request.changeSessionId();
        var auth =
                new UsernamePasswordAuthenticationToken(
                        p, null, List.of(new SimpleGrantedAuthority("ROLE_" + a.role())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        new CsrfAuthenticationStrategy(csrf).onAuthentication(auth, request, response);
        cookies.write(request, response, browser.issue(p.id()));
        return ApiResponse.success(identity.me(p.id()));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest req, HttpServletResponse res) {
        String token = cookies.read(req);
        if (token != null) browser.revoke(token);
        cookies.write(req, res, null);
        var s = req.getSession(false);
        if (s != null) s.invalidate();
        SecurityContextHolder.clearContext();
        csrf.saveToken(null, req, res);
        return ApiResponse.success(true);
    }

    @GetMapping("/me")
    public ApiResponse<Account> me(Principal p) {
        return ApiResponse.success(identity.me(actors.require(p).id()));
    }

    @PatchMapping("/me/profile")
    public ApiResponse<Account> profile(
            Principal p,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @Valid @RequestBody Profile r) {
        return ApiResponse.success(
                identity.profile(actors.student(p).id(), CoreValues.revision(revision), r));
    }

    @GetMapping("/me/consents")
    public ApiResponse<?> consents(Principal p) {
        return ApiResponse.success(consents.list(actors.require(p).id()));
    }

    @PostMapping("/me/consents")
    public ApiResponse<String> grant(Principal p, @Valid @RequestBody Grant r) {
        return ApiResponse.success(consents.grant(actors.require(p).id(), r));
    }

    @PostMapping("/me/consents/{id}/revoke")
    public ApiResponse<Boolean> revoke(Principal p, @PathVariable String id) {
        consents.revoke(actors.require(p).id(), CoreValues.id(id));
        return ApiResponse.success(true);
    }

    @PatchMapping("/admin/accounts/{id}/status")
    public ApiResponse<Boolean> status(
            Principal p,
            @PathVariable String id,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @Valid @RequestBody Status r) {
        identity.status(
                actors.admin(p).id(), CoreValues.id(id), CoreValues.revision(revision), r.status());
        return ApiResponse.success(true);
    }
}
