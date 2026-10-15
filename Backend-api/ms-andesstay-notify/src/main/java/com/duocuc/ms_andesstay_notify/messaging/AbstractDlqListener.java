package com.duocuc.ms_andesstay_notify.messaging;

import com.duocuc.ms_andesstay_notify.messaging.exception.NonRecoverableException;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;

/**
 * Contrato único de <b>confirmación explícita (ACK manual)</b> de los consumidores.
 *
 * <p>El contenedor está en {@code AcknowledgeMode.MANUAL}, por lo que el mensaje
 * sólo queda confirmado si este código lo dice explícitamente. Las tres únicas
 * salidas posibles son:</p>
 *
 * <table>
 *   <caption>Contrato de confirmación</caption>
 *   <tr><th>Situación</th><th>Acción AMQP</th><th>Destino del mensaje</th></tr>
 *   <tr><td>{@code process(...)} termina bien</td><td>{@code basicAck}</td><td>confirmado y eliminado de la cola</td></tr>
 *   <tr><td>{@link com.duocuc.ms_andesstay_notify.messaging.exception.RecoverableException}</td>
 *       <td>reintentos acotados y luego {@code basicNack(requeue=false)}</td><td>{@code q.andesstay.dlq}</td></tr>
 *   <tr><td>{@link NonRecoverableException} u otro error inesperado</td>
 *       <td>{@code basicNack(requeue=false)}</td><td>{@code q.andesstay.dlq}</td></tr>
 * </table>
 *
 * <p><b>Decisiones de diseño (indicador 5):</b></p>
 * <ol>
 *   <li>Los reintentos se ejecutan <i>aquí dentro</i> con {@link RetryTemplate}
 *       (3 intentos, 2 s entre intentos) y <b>sólo</b> para errores recuperables.
 *       No se usa el advice chain del contenedor
 *       ({@code RetryInterceptor} + {@code RepublishMessageRecoverer}) porque en
 *       modo MANUAL el {@code recoverer} republicaría el mensaje mientras el ACK
 *       sigue pendiente de este listener: el mensaje original quedaría sin
 *       confirmar y se reentregaría al cerrarse el canal (duplicados).</li>
 *   <li>El descarte usa {@code basicNack(requeue=false)}: la propia cola aplica su
 *       dead-lettering ({@code x-dead-letter-exchange}, tarea 03), de modo que el
 *       mensaje llega a la DLQ conservando su cuerpo y con los headers
 *       {@code x-death} que indican cola y motivo del rechazo.</li>
 *   <li>El mensaje nunca se reencola ({@code requeue=false} siempre), así que no
 *       existe el bucle de reentrega infinito clásico de {@code requeue=true}.</li>
 * </ol>
 *
 * @param <T> tipo del evento que viaja por la cola
 */
public abstract class AbstractDlqListener<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final RetryTemplate retryTemplate;

    protected AbstractDlqListener(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    /**
     * Lógica de negocio del dominio. Puede lanzar {@code RecoverableException} o
     * {@code NonRecoverableException} para indicar al contrato cómo proceder.
     *
     * <p>Debe ser idempotente: si el consumidor cae después de procesar y antes
     * del ACK, el mensaje se reentrega.</p>
     *
     * @param channelName canal lógico del dominio que atiende el mensaje (EMAIL, SMS, PUSH...)
     * @param event       evento deserializado
     */
    protected abstract void process(String channelName, T event);

    /**
     * Aplica el contrato de confirmación sobre el mensaje recibido.
     *
     * @param channelName canal lógico del dominio
     * @param event       evento deserializado
     * @param channel     canal AMQP de la entrega (para confirmar/rechazar)
     * @param deliveryTag identificador de la entrega dentro del canal
     * @param redelivered indica si el broker ya había entregado antes este mensaje
     */
    protected void handle(String channelName, T event, Channel channel,
                          long deliveryTag, boolean redelivered) throws IOException {
        try {
            retryTemplate.execute(context -> {
                if (context.getRetryCount() > 0) {
                    log.warn("[REINTENTO] canal={} deliveryTag={} intento={} evento={}",
                            channelName, deliveryTag, context.getRetryCount() + 1, event);
                }
                process(channelName, event);
                return null;
            });

            // ACK explícito: el evento quedó procesado (comportamiento normal).
            channel.basicAck(deliveryTag, false);
            log.info("[ACK] canal={} deliveryTag={} reentregado={} evento={}",
                    channelName, deliveryTag, redelivered, event);

        } catch (NonRecoverableException e) {
            discard(channelName, event, channel, deliveryTag, e, "error no recuperable (no se reintenta)");
        } catch (Exception e) {
            discard(channelName, event, channel, deliveryTag, e, "reintentos agotados");
        }
    }

    /**
     * Descarte definitivo: registra en {@code ERROR} el motivo con contexto
     * (cola, deliveryTag, excepción y payload) y rechaza el mensaje sin
     * reencolarlo para que la cola lo derive a la DLQ.
     */
    private void discard(String channelName, T event, Channel channel, long deliveryTag,
                         Exception cause, String motivo) throws IOException {
        log.error("[DESCARTE->DLQ] canal={} deliveryTag={} motivo={} excepcion={} mensaje={} evento={}",
                channelName, deliveryTag, motivo, cause.getClass().getName(), cause.getMessage(), event, cause);
        channel.basicNack(deliveryTag, false, false);
    }
}
