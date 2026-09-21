package cl.duoc.andesstay.ms_andesstay_bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validador de emisor (issuer) compatible con Azure Entra ID (soporta v1 sts.windows.net y v2 login.microsoftonline.com)
 */
public class TenantIssuerValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger log = LoggerFactory.getLogger(TenantIssuerValidator.class);
    private final String tenantId;

    public TenantIssuerValidator(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
        if (issuer.contains(this.tenantId)) {
            return OAuth2TokenValidatorResult.success();
        }

        log.warn("❌ [JWT ISSUER MISMATCH] Tenant esperado: '{}', Issuer recibido en token: '{}'",
                tenantId, issuer);

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "Issuer mismatch. El emisor '" + issuer + "' no pertenece al tenant: " + tenantId,
                null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}
