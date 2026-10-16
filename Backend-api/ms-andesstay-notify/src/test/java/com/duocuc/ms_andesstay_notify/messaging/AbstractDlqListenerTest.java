package com.duocuc.ms_andesstay_notify.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.duocuc.ms_andesstay_notify.config.RabbitListenerConfig;
import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.duocuc.ms_andesstay_notify.messaging.exception.RecoverableException;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Prueba el contrato de confirmación de {@link AbstractDlqListener} (indicadores 4 y 5):
 * <ul>
 *   <li>éxito → {@code basicAck};</li>
 *   <li>error no recuperable → {@code basicNack(requeue=false)} sin reintentos;</li>
 *   <li>error recuperable → se reintenta hasta {@link RabbitListenerConfig#MAX_ATTEMPTS}
 *       veces y luego {@code basicNack(requeue=false)} hacia la DLQ;</li>
 *   <li>cualquier otra excepción → {@code basicNack(requeue=false)} sin reintentos.</li>
 * </ul>
 *
 * <p>No depende de un broker: el {@link Channel} es un mock de Mockito. El
 * {@link RetryTemplate} usa la misma política que producción pero sin espera, para
 * que los tests no duerman {@code RETRY_INTERVAL_MS} ms por reintento.</p>
 */
class AbstractDlqListenerTest {

    private static final long DELIVERY_TAG = 42L;

    private final Channel channel = mock(Channel.class);

    /** Misma política que {@link RabbitListenerConfig} pero con backoff nulo. */
    private static RetryTemplate fastRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(
                RabbitListenerConfig.MAX_ATTEMPTS,
                Map.of(RecoverableException.class, true),
                false,
                false));
        retryTemplate.setBackOffPolicy(new org.springframework.retry.backoff.NoBackOffPolicy());
        return retryTemplate;
    }

    @Test
    void acksWhenProcessSucceeds() throws Exception {
        TestListener listener = new TestListener(null);

        listener.handleNow(channel);

        assertThat(listener.calls.get()).isEqualTo(1);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(DELIVERY_TAG, false, false);
    }

    @Test
    void discardsWithoutRetryWhenNonRecoverable() throws Exception {
        TestListener listener = new TestListener(new NonRecoverableException("payload inválido"));

        listener.handleNow(channel);

        // No se reintenta un error no recuperable.
        assertThat(listener.calls.get()).isEqualTo(1);
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void retriesThenDiscardsWhenRecoverable() throws Exception {
        TestListener listener = new TestListener(new RecoverableException("fallo transitorio"));

        listener.handleNow(channel);

        // Se agotan los intentos y el mensaje termina rechazado hacia la DLQ.
        assertThat(listener.calls.get()).isEqualTo(RabbitListenerConfig.MAX_ATTEMPTS);
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(DELIVERY_TAG, false);
    }

    @Test
    void discardsWithoutRetryOnUnexpectedError() throws Exception {
        TestListener listener = new TestListener(new IllegalStateException("inesperado"));

        listener.handleNow(channel);

        assertThat(listener.calls.get()).isEqualTo(1);
        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(DELIVERY_TAG, false);
    }

    /** Subclase de prueba que instrumenta {@code process} y expone {@code handle}. */
    private static final class TestListener extends AbstractDlqListener<String> {

        private final AtomicInteger calls = new AtomicInteger();
        private final RuntimeException failure;

        TestListener(RuntimeException failure) {
            super(fastRetryTemplate());
            this.failure = failure;
        }

        @Override
        protected void process(String event) {
            calls.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        protected String describe(String event) {
            return event;
        }

        void handleNow(Channel channel) throws IOException {
            handle("TEST", "evento-de-prueba", channel, DELIVERY_TAG, false);
        }
    }
}
