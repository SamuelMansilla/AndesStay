package com.duocuc.ms_andesstay_notify.messaging.notifications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.duocuc.ms_andesstay_notify.config.RabbitListenerConfig;
import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.duocuc.ms_andesstay_notify.messaging.exception.RecoverableException;
import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import com.duocuc.ms_andesstay_notify.service.NotificationService;
import com.rabbitmq.client.Channel;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Verifica el comportamiento real de {@link EmailListener}: delega en
 * {@link NotificationService} y confirma/rechaza el mensaje según el resultado
 * (indicadores 4 y 5). No depende de un broker.
 */
class EmailListenerTest {

    private static final long DELIVERY_TAG = 7L;

    private final NotificationService notificationService = mock(NotificationService.class);
    private final Channel channel = mock(Channel.class);
    private final EmailListener listener = new EmailListener(fastRetryTemplate(), notificationService);

    @Test
    void acksWhenNotificationIsSent() throws Exception {
        listener.onEmail(event(), channel, DELIVERY_TAG, false);

        verify(notificationService).sendEmail(any(NotificationEvent.class));
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void discardsToDlqWhenPayloadIsInvalid() throws Exception {
        doThrow(new NonRecoverableException("Payload sin destinatario (recipient) en EMAIL"))
                .when(notificationService).sendEmail(any(NotificationEvent.class));

        listener.onEmail(event(), channel, DELIVERY_TAG, false);

        verify(channel).basicNack(DELIVERY_TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private static NotificationEvent event() {
        return new NotificationEvent("n-1", 1L, "huesped@andesstay.cl", "EMAIL", "Confirmación", "Su reserva fue confirmada");
    }

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
}
