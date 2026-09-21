package cl.duoc.andesstay.ms_andesstay_bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validador de audiencia flexible para Azure Entra ID (soporta audience con o sin prefijo 'api://')
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger log = LoggerFactory.getLogger(AudienceValidator.class);
    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> tokenAudiences = jwt.getAudience();
        String rawAudience = expectedAudience != null ? expectedAudience.replace("api://", "") : "";

        if (tokenAudiences != null && expectedAudience != null) {
            boolean matches = tokenAudiences.stream().anyMatch(aud ->
                aud.equalsIgnoreCase(expectedAudience) ||
                aud.equalsIgnoreCase(rawAudience) ||
                ("api://" + aud).equalsIgnoreCase(expectedAudience)
            );

            if (matches) {
                return OAuth2TokenValidatorResult.success();
            }
        }

        log.warn("❌ [JWT AUDIENCE MISMATCH] Esperado: '{}' o '{}', Recibido en token: {}",
                expectedAudience, rawAudience, tokenAudiences);

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The required audience is missing or invalid. Expected: " + expectedAudience + ", token had: " + tokenAudiences,
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}