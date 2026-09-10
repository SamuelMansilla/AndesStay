package cl.duoc.andesstay.ms_andesstay_bff.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
    private List<String> audiences;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Diagnóstico / Rutas públicas
                .requestMatchers("/actuator/health", "/public/**").permitAll()

                // Reglas según caso AndesStay
                // Catálogo de unidades
                .requestMatchers(HttpMethod.POST, "/api/catalog/**").hasRole("Admin")
                .requestMatchers(HttpMethod.PUT, "/api/catalog/**").hasRole("Admin")
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").hasAnyRole("Admin", "Recepcionista", "Huésped")

                // Reservas
                .requestMatchers(HttpMethod.PUT, "/api/reservations/*/status").hasAnyRole("Admin", "Recepcionista")
                .requestMatchers(HttpMethod.POST, "/api/reservations").hasAnyRole("Admin", "Recepcionista", "Huésped")
                .requestMatchers(HttpMethod.GET, "/api/reservations/**").hasAnyRole("Admin", "Recepcionista", "Huésped")

                // Auditoría y Reportes
                .requestMatchers("/api/report/**").hasRole("Admin")
                .requestMatchers("/api/audit/**").hasAnyRole("Admin", "Auditor")

                .anyRequest().authenticated()
            )
            // Manejador estructurado de respuestas de error (401 y 403) exigido en rúbrica
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(String.format(
                            "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                            Instant.now(), authException.getMessage()
                    ));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(String.format(
                            "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Permisos insuficientes para realizar esta operación.\"}",
                            Instant.now()
                    ));
                })
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    .decoder(jwtDecoder())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // Validador de emisor y vigencia (exp)
        OAuth2TokenValidator<Jwt> defaultWithIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        // Validador de audience
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audiences);

        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(defaultWithIssuer, audienceValidator);
        jwtDecoder.setJwtValidator(combinedValidator);

        return jwtDecoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }
}
