package com.duocuc.ms_andesstay_notify.messaging.dlq;

import com.duocuc.ms_andesstay_notify.messaging.AbstractDlqListener;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Consumidor del dominio <b>DLQ</b> — supervisión de los mensajes descartados.
 *
 * <ul>
 *   <li><b>Cola:</b> {@code q.andesstay.dlq} (destino común de todos los dominios).</li>
 *   <li><b>Routing key:</b> {@code dlq.routing.key} (la que usa cada cola al derivar).</li>
 *   <li><b>ACK:</b> manual ({@link AbstractDlqListener}): se registra el descarte en
 *       {@code WARN} y se confirma el mensaje. Nunca se reencola (la DLQ no tiene
 *       DLX propia, reencolar sólo produciría un bucle de reentrega).</li>
 * </ul>
 *
 * <p>Recibe el {@link Message} crudo, y no un evento tipado, porque en esta cola
 * conviven payloads de los tres dominios (notificaciones, housekeeping y
 * documentos). El objetivo es dejar en el log, en un único lugar, la traza
 * completa del descarte: cola de origen, routing key original, motivo y
 * excepción (headers {@code x-death} que agrega el broker al derivar).</p>
 */
@Service
public class DeadLetterListener extends AbstractDlqListener<Message> {

    public DeadLetterListener(RetryTemplate messagingRetryTemplate) {
        super(messagingRetryTemplate);
    }

    @RabbitListener(queues = "${andesstay.messaging.dlq}")
    public void onDeadLetter(Message message, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                             @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        handle("DLQ", message, channel, deliveryTag, redelivered);
    }

    /**
     * Registra el mensaje descartado con todo su contexto de trazabilidad.
     */
    @Override
    protected void process(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Map<String, Object> death = firstDeath(headers);

        log.warn("[DLQ] Mensaje descartado: colaOrigen={} routingKeyOriginal={} motivo={} reentregas={} "
                        + "excepcionOriginal={} colaOriginal={} payload={}",
                text(death, "queue"),
                text(death, "routing-keys"),
                text(death, "reason"),
                text(death, "count"),
                text(headers, "x-exception-message"),
                text(headers, "x-original-queue"),
                new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * En los logs de ACK/reintento basta con un resumen del mensaje: el payload
     * completo y su trazabilidad ya se registran en {@link #process(Message)}.
     */
    @Override
    protected String describe(Message message) {
        return "Message{cola=" + message.getMessageProperties().getConsumerQueue()
                + ", bytes=" + message.getBody().length + "}";
    }

    /** El header {@code x-death} es una lista: el primer elemento describe el último rechazo. */    @SuppressWarnings("unchecked")
    private Map<String, Object> firstDeath(Map<String, Object> headers) {
        Object xDeath = headers.get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            return (Map<String, Object>) first;
        }
        return Map.of();
    }

    /** Valor legible de un header, sin fallar si no está presente. */
    private String text(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return "-";
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("-");
        }
        return String.valueOf(value);
    }
}
