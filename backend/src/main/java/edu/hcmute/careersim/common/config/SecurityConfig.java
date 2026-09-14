package edu.hcmute.careersim.common.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.local-demo-mode:false}") boolean localDemoMode,
            @Value("${app.security.local-demo-email:student@demo.com}") String localDemoEmail)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                                policy ->
                                                        policy.policyDirectives(
                                                                "default-src 'none'; frame-ancestors 'none'"))
                                        .frameOptions(frame -> frame.deny())
                                        .referrerPolicy(
                                                referrer ->
                                                        referrer.policy(
                                                                ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .NO_REFERRER))
                                        .permissionsPolicyHeader(
                                                permissions ->
                                                        permissions.policy(
                                                                "camera=(), microphone=(), geolocation=(), payment=()")))
                .authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers("/health")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/v1/simulations/**")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST, "/v1/simulations/*/attempts")
                                        .permitAll() // TODO(auth): change to .hasRole("STUDENT")
                                        // when identity module is ready
                                        .requestMatchers("/v1/assessments/**")
                                        .permitAll()
                                        .requestMatchers("/v1/guidance/**")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        exceptions ->
                                exceptions.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        if (localDemoMode) {
            http.addFilterBefore(
                    new LocalDemoAuthenticationFilter(localDemoEmail),
                    AnonymousAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "X-Student-Id"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
