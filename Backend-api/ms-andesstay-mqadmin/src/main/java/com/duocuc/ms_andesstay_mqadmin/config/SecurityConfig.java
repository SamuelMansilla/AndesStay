package com.duocuc.ms_andesstay_mqadmin.config;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del microservicio administrador (tarea 11).
 *
 * <p>Valida el JWT emitido por el IDaaS (Azure Entra ID) que el BFF propaga en el
 * header {@code Authorization}. Es un <b>resource server</b> sin sesión: cada
 * petición debe traer un token válido. Solo el rol {@code Admin} puede administrar
 * RabbitMQ ({@code @PreAuthorize} en {@code MqAdminController}).</p>
 *
 * <p>Reutiliza los validadores del BFF ({@link AudienceValidator},
 * {@link TenantIssuerValidator}, {@link JwtRoleConverter}) para no duplicar la
 * lógica de issuer v1/v2 y audience con/sin {@code api://}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
    private String audience;

    private final JwtRoleConverter jwtRoleConverter;

    public SecurityConfig(JwtRoleConverter jwtRoleConverter) {
        this.jwtRoleConverter = jwtRoleConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El health no debe exigir token (monitorización/arranque).
                        .requestMatchers("/actuator/**").permitAll()
                        // Todo lo demás (la API de administración) exige JWT válido.
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("[MQADMIN AUTH 401] Petición rechazada en {}: {}",
                                    request.getRequestURI(), authException.getMessage());
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                                    Instant.now(), authException.getMessage()));
                        }))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(customJwtAuthenticationConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter customJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // Extraer el tenantId del issuerUri configurado (UUID).
        String tenantId = issuerUri.replaceAll(".*/([a-f0-9\\-]+)(/.*)?$", "$1");

        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> issuerValidator = new TenantIssuerValidator(tenantId);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }
}
