package com.duocuc.ms_andesstay_mqadmin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Test de contexto: verifica que la aplicación arranca con toda su configuración
 * (seguridad, AMQP, topología declarada, controladores).
 *
 * <p>Se reemplaza el {@link JwtDecoder} por un mock para que el arranque no haga
 * la petición de descubrimiento OIDC al IDaaS: el test no depende de la red ni de
 * un broker.</p>
 */
@SpringBootTest
class MsAndesstayMqadminApplicationTests {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}
