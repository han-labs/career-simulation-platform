package edu.hcmute.careersim.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hcmute.careersim.common.response.ErrorResponse;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        var encoders = new HashMap<String, PasswordEncoder>();
        encoders.put(
                "pbkdf2@SpringSecurity_v5_8",
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        return new DelegatingPasswordEncoder("pbkdf2@SpringSecurity_v5_8", encoders);
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper mapper,
            CsrfTokenRepository csrf,
            Environment env,
            edu.hcmute.careersim.identity.service.BrowserAuthentication browserAuth,
            BrowserCredentialCookies cookies,
            edu.hcmute.careersim.identity.access.ActorAccess actors,
            org.springframework.beans.factory.ObjectProvider<ClientRegistrationRepository> clients)
            throws Exception {
        http.csrf(
                        c ->
                                c.csrfTokenRepository(csrf)
                                        .csrfTokenRequestHandler(
                                                new CsrfTokenRequestAttributeHandler()))
                .cors(Customizer.withDefaults())
                .securityContext(
                        c ->
                                c.securityContextRepository(
                                        new RequestAttributeSecurityContextRepository()))
                .requestCache(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(l -> l.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .headers(
                        h ->
                                h.contentSecurityPolicy(
                                                p ->
                                                        p.policyDirectives(
                                                                "default-src 'none'; frame-ancestors 'none'"))
                                        .frameOptions(f -> f.deny()))
                .authorizeHttpRequests(
                        r ->
                                r.requestMatchers(
                                                "/health",
                                                "/v2/auth/csrf",
                                                "/v2/auth/providers",
                                                "/oauth2/authorization/google",
                                                "/login/oauth2/code/google")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/v2/auth/register",
                                                "/v2/auth/login",
                                                "/v2/auth/logout")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/v1/simulations",
                                                "/v1/simulations/*",
                                                "/v1/simulations/*/tasks",
                                                "/v2/scenarios",
                                                "/v2/scenarios/*",
                                                "/v2/assessment-types")
                                        .permitAll()
                                        .requestMatchers("/v1/**", "/v2/**")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, x) -> {
                                                    res.setStatus(401);
                                                    res.setContentType("application/json");
                                                    mapper.writeValue(
                                                            res.getOutputStream(),
                                                            ErrorResponse.of(
                                                                    "UNAUTHORIZED",
                                                                    "Sign in is required.",
                                                                    req.getRequestURI()));
                                                })
                                        .accessDeniedHandler(
                                                (req, res, x) -> {
                                                    res.setStatus(403);
                                                    res.setContentType("application/json");
                                                    mapper.writeValue(
                                                            res.getOutputStream(),
                                                            ErrorResponse.of(
                                                                    "FORBIDDEN",
                                                                    "Access or CSRF validation denied.",
                                                                    req.getRequestURI()));
                                                }));
        http.addFilterBefore(
                new BrowserAuthenticationFilter(browserAuth, cookies, actors, mapper),
                org.springframework.security.web.csrf.CsrfFilter.class);
        if (clients.getIfAvailable() != null) {
            var resolver =
                    new DefaultOAuth2AuthorizationRequestResolver(
                            clients.getObject(), "/oauth2/authorization");
            resolver.setAuthorizationRequestCustomizer(
                    OAuth2AuthorizationRequestCustomizers.withPkce());
            http.oauth2Login(
                    o ->
                            o.authorizationEndpoint(
                                            a ->
                                                    a.authorizationRequestResolver(resolver)
                                                            .authorizationRequestRepository(
                                                                    GoogleOAuthFlow.requests()))
                                    .authorizedClientRepository(GoogleOAuthFlow.transientClients())
                                    .successHandler(
                                            (req, res, auth) ->
                                                    GoogleOAuthFlow.success(
                                                            req,
                                                            res,
                                                            auth,
                                                            browserAuth,
                                                            cookies,
                                                            csrf))
                                    .failureHandler(
                                            (req, res, error) -> {
                                                org.springframework.security.core.context
                                                        .SecurityContextHolder.clearContext();
                                                res.sendRedirect("/account?oauthError=denied");
                                            }));
        }
        if (localAdminEnabled(env))
            http.addFilterBefore(
                    new LocalDemoAuthenticationFilter(actors), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    static boolean localAdminEnabled(Environment env) {
        return env.acceptsProfiles(Profiles.of("local"))
                && !env.acceptsProfiles(Profiles.of("prod", "production"));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String origins) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type", "Accept", "X-CSRF-TOKEN", "If-Match"));
        c.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
