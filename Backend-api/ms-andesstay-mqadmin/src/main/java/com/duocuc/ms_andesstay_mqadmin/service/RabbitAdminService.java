package com.duocuc.ms_andesstay_mqadmin.service;

import com.duocuc.ms_andesstay_mqadmin.config.MessagingProperties;
import com.duocuc.ms_andesstay_mqadmin.exception.ResourceNotFoundException;
import com.duocuc.ms_andesstay_mqadmin.model.BindingInfo;
import com.duocuc.ms_andesstay_mqadmin.model.ExchangeInfo;
import com.duocuc.ms_andesstay_mqadmin.model.QueueInfo;
import com.duocuc.ms_andesstay_mqadmin.service.dto.BindingRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.ExchangeRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.PublishRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.QueueRequest;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Service;

/**
 * Único punto de acceso a la administración del broker (indicador 7 de la pauta).
 *
 * <p>Encapsula <b>toda</b> la interacción con {@link AmqpAdmin}: creación,
 * eliminación, purga y listado de colas, exchanges y bindings. Los controladores
 * (tarea 10) solo llaman a estos métodos de alto nivel y no conocen RabbitMQ.</p>
 *
 * <h2>Limitación de la librería y estrategia de listado</h2>
 * <p>Spring AMQP no expone un listado nativo completo de recursos del broker
 * (para eso haría falta la Management HTTP API). Por eso este servicio mantiene
 * un <b>registro de recursos administrados</b>:</p>
 * <ul>
 *   <li>al arrancar se <b>siembra</b> con la topología declarada en
 *       {@code andesstay.messaging.*} (indicador 1), sin contactar al broker; y</li>
 *   <li>se actualiza con cada recurso creado/eliminado a través de la API.</li>
 * </ul>
 * <p>El conteo de mensajes/consumidores sí se consulta al broker en el listado
 * ({@link AmqpAdmin#getQueueInfo(String)}); si la cola aún no existe en el broker
 * esos valores son {@code null}.</p>
 */
@Service
public class RabbitAdminService {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitAdminService.class);

    /** Argumentos AMQP estándar de dead-lettering (no son nombres de topología). */
    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    private static final String X_MESSAGE_TTL = "x-message-ttl";

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messaging;

    /** Recursos administrados: declarados en configuración + creados vía la API. */
    private final Map<String, Queue> managedQueues = new ConcurrentHashMap<>();
    private final Map<String, Exchange> managedExchanges = new ConcurrentHashMap<>();
    private final Map<String, Binding> managedBindings = new ConcurrentHashMap<>();

    public RabbitAdminService(AmqpAdmin amqpAdmin, RabbitTemplate rabbitTemplate, MessagingProperties messaging) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.messaging = messaging;
    }

    // ── Siembra de la topología declarada ─────────────────────────────────────

    /**
     * Registra en memoria la topología declarada en {@code andesstay.messaging.*}
     * (exchanges, colas y bindings), <b>sin</b> declararla en el broker: el dueño
     * de esa topología es {@code ms-andesstay-notify}.
     */
    @PostConstruct
    void seedDeclaredTopology() {
        registerExchange(new DirectExchange(messaging.getExchange(), true, false));
        registerExchange(new DirectExchange(messaging.getDlx(), true, false));

        registerQueue(durableWithDeadLetter(messaging.getQueues().getNotifyEmail()));
        registerQueue(durableWithDeadLetter(messaging.getQueues().getNotifySms()));
        registerQueue(durableWithDeadLetter(messaging.getQueues().getNotifyPush()));
        registerQueue(durableWithDeadLetter(messaging.getQueues().getHousekeepingTickets()));
        registerQueue(durableWithDeadLetter(messaging.getQueues().getDocsGenerate()));
        registerQueue(new Queue(messaging.getDlq(), true, false, false, null));

        registerBinding(messaging.getQueues().getNotifyEmail(),
                messaging.getExchange(), messaging.getRoutingKeys().getNotifyEmail());
        registerBinding(messaging.getQueues().getNotifySms(),
                messaging.getExchange(), messaging.getRoutingKeys().getNotifySms());
        registerBinding(messaging.getQueues().getNotifyPush(),
                messaging.getExchange(), messaging.getRoutingKeys().getNotifyPush());
        registerBinding(messaging.getQueues().getHousekeepingTickets(),
                messaging.getExchange(), messaging.getRoutingKeys().getHousekeepingTicket());
        registerBinding(messaging.getQueues().getDocsGenerate(),
                messaging.getExchange(), messaging.getRoutingKeys().getDocsGenerate());
        registerBinding(messaging.getDlq(), messaging.getDlx(), messaging.getDlqRoutingKey());

        LOG.info("[MQADMIN] Topología declarada registrada: {} exchanges, {} colas, {} bindings",
                managedExchanges.size(), managedQueues.size(), managedBindings.size());
    }

    // ── Colas ─────────────────────────────────────────────────────────────────

    /** Crea (declara) una cola en el broker y la añade al registro. */
    public QueueInfo createQueue(QueueRequest request) {
        Queue queue = buildQueue(request);
        if (managedQueues.containsKey(queue.getName())) {
            throw new IllegalArgumentException("La cola ya está administrada: " + queue.getName());
        }
        String declared = amqpAdmin.declareQueue(queue);
        managedQueues.put(queue.getName(), queue);
        LOG.info("[MQADMIN] Cola creada: {} (durable={}, autoDelete={}, ttl={}ms, deadLetter={})",
                declared, queue.isDurable(), queue.isAutoDelete(), request.ttlMillis(), request.deadLetter());
        return toQueueInfo(queue);
    }

    /** Elimina una cola administrada del broker y del registro. */
    public void deleteQueue(String name) {
        if (!managedQueues.containsKey(name)) {
            throw new ResourceNotFoundException("La cola no está administrada: " + name);
        }
        boolean deleted = amqpAdmin.deleteQueue(name);
        managedQueues.remove(name);
        managedBindings.values().removeIf(binding -> binding.getDestination().equals(name));
        if (deleted) {
            LOG.info("[MQADMIN] Cola eliminada: {}", name);
        } else {
            LOG.warn("[MQADMIN] La cola {} no existía en el broker; se retira del registro", name);
        }
    }

    /** Purga todos los mensajes de una cola administrada y devuelve cuántos había. */
    public int purgeQueue(String name) {
        if (!managedQueues.containsKey(name)) {
            throw new ResourceNotFoundException("La cola no está administrada: " + name);
        }
        int purged = amqpAdmin.purgeQueue(name);
        LOG.info("[MQADMIN] Cola purgada: {} ({} mensajes)", name, purged);
        return purged;
    }

    /** Lista las colas administradas, con el conteo actual del broker. */
    public List<QueueInfo> listQueues() {
        List<QueueInfo> queues = managedQueues.values().stream()
                .sorted(Comparator.comparing(Queue::getName))
                .map(this::toQueueInfo)
                .toList();
        LOG.info("[MQADMIN] Listado de colas administradas: {}", queues.size());
        return queues;
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    /** Crea (declara) un exchange en el broker y lo añade al registro. */
    public ExchangeInfo createExchange(ExchangeRequest request) {
        Exchange exchange = buildExchange(request);
        if (managedExchanges.containsKey(exchange.getName())) {
            throw new IllegalArgumentException("El exchange ya está administrado: " + exchange.getName());
        }
        amqpAdmin.declareExchange(exchange);
        managedExchanges.put(exchange.getName(), exchange);
        LOG.info("[MQADMIN] Exchange creado: {} (type={}, durable={}, autoDelete={})",
                exchange.getName(), exchange.getType(), exchange.isDurable(), exchange.isAutoDelete());
        return toExchangeInfo(exchange);
    }

    /** Elimina un exchange administrado del broker y del registro. */
    public void deleteExchange(String name) {
        if (!managedExchanges.containsKey(name)) {
            throw new ResourceNotFoundException("El exchange no está administrado: " + name);
        }
        boolean deleted = amqpAdmin.deleteExchange(name);
        managedExchanges.remove(name);
        managedBindings.values().removeIf(binding -> binding.getExchange().equals(name));
        if (deleted) {
            LOG.info("[MQADMIN] Exchange eliminado: {}", name);
        } else {
            LOG.warn("[MQADMIN] El exchange {} no existía en el broker; se retira del registro", name);
        }
    }

    /** Lista los exchanges administrados. */
    public List<ExchangeInfo> listExchanges() {
        List<ExchangeInfo> exchanges = managedExchanges.values().stream()
                .sorted(Comparator.comparing(Exchange::getName))
                .map(this::toExchangeInfo)
                .toList();
        LOG.info("[MQADMIN] Listado de exchanges administrados: {}", exchanges.size());
        return exchanges;
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    /** Crea un binding entre un exchange y una cola administrados. */
    public BindingInfo createBinding(BindingRequest request) {
        requireManagedQueue(request.queue());
        requireManagedExchange(request.exchange());
        Binding binding = buildBinding(request);
        String key = bindingKey(binding);
        if (managedBindings.containsKey(key)) {
            throw new IllegalArgumentException("El binding ya existe: " + key);
        }
        amqpAdmin.declareBinding(binding);
        managedBindings.put(key, binding);
        LOG.info("[MQADMIN] Binding creado: {} --[{}]--> {}",
                request.exchange(), request.routingKey(), request.queue());
        return new BindingInfo(request.queue(), request.exchange(), request.routingKey());
    }

    /** Elimina un binding administrado. */
    public void deleteBinding(BindingRequest request) {
        Binding binding = buildBinding(request);
        String key = bindingKey(binding);
        if (!managedBindings.containsKey(key)) {
            throw new ResourceNotFoundException("El binding no existe: " + key);
        }
        amqpAdmin.removeBinding(binding);
        managedBindings.remove(key);
        LOG.info("[MQADMIN] Binding eliminado: {} --[{}]--> {}",
                request.exchange(), request.routingKey(), request.queue());
    }

    /** Lista los bindings administrados. */
    public List<BindingInfo> listBindings() {
        List<BindingInfo> bindings = managedBindings.values().stream()
                .map(binding -> new BindingInfo(binding.getDestination(), binding.getExchange(),
                        binding.getRoutingKey()))
                .sorted(Comparator.comparing(BindingInfo::exchange)
                        .thenComparing(BindingInfo::routingKey)
                        .thenComparing(BindingInfo::queue))
                .toList();
        LOG.info("[MQADMIN] Listado de bindings administrados: {}", bindings.size());
        return bindings;
    }

    // ── Publicación de mensajes de prueba ─────────────────────────────────────

    /**
     * Publica un mensaje de prueba hacia el broker (trafico visible en el
     * Management UI).
     *
     * <p>Modos mutuamente excluyentes:</p>
     * <ul>
     *   <li><b>exchange:</b> la salida sale por {@code ex.*} + routing key y se
     *       enruta según los bindings declarados (o se descarta si no hay).</li>
     *   <li><b>queue:</b> entra directo a la cola viajando por el exchange por
     *       defecto ({@code ""}) con {@code routingKey = queue}; así un mensaje
     *       queda visible como "Ready" aunque no existan consumidores.</li>
     * </ul>
     *
     * @throws IllegalArgumentException si el destino es ambiguo o ausente (→ 409)
     * @throws ResourceNotFoundException   si el exchange/cola no está administrado (→ 404)
     */
    public Map<String, Object> publishMessage(PublishRequest request) {
        boolean toExchange = request.exchange() != null && !request.exchange().isBlank();
        boolean toQueue = request.queue() != null && !request.queue().isBlank();

        if (toExchange == toQueue) {
            throw new IllegalArgumentException(
                    "Indica un único destino: 'exchange' (con routingKey) o 'queue', no ambos ni ninguno");
        }

        Message message = MessageBuilder.withBody(request.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();

        String destination;
        String routingKey;
        if (toQueue) {
            requireManagedQueue(request.queue());
            routingKey = request.queue();
            rabbitTemplate.send("", routingKey, message);
            destination = "queue:" + request.queue();
        } else {
            requireManagedExchange(request.exchange());
            routingKey = request.routingKey() == null ? "" : request.routingKey().trim();
            rabbitTemplate.send(request.exchange(), routingKey, message);
            destination = "exchange:" + request.exchange();
        }

        LOG.info("[MQADMIN] Mensaje publicado: {} --[{}]--> ({} bytes)",
                destination, routingKey, message.getBody().length);

        return Map.of(
                "destination", destination,
                "routingKey", routingKey,
                "bytes", message.getBody().length,
                "contentType", MessageProperties.CONTENT_TYPE_JSON);
    }

    // ── Construcción y conversión ─────────────────────────────────────────────

    private Queue buildQueue(QueueRequest request) {
        Map<String, Object> arguments = new HashMap<>();
        if (request.ttlMillis() != null) {
            arguments.put(X_MESSAGE_TTL, request.ttlMillis());
        }
        if (request.deadLetter()) {
            arguments.put(X_DEAD_LETTER_EXCHANGE, messaging.getDlx());
            arguments.put(X_DEAD_LETTER_ROUTING_KEY, messaging.getDlqRoutingKey());
        }
        return new Queue(request.name(), request.durable(), false, request.autoDelete(),
                arguments.isEmpty() ? null : arguments);
    }

    private Exchange buildExchange(ExchangeRequest request) {
        String type = request.type() == null ? "direct" : request.type().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "direct" -> new DirectExchange(request.name(), request.durable(), request.autoDelete());
            case "topic" -> new TopicExchange(request.name(), request.durable(), request.autoDelete());
            case "fanout" -> new FanoutExchange(request.name(), request.durable(), request.autoDelete());
            case "headers" -> new HeadersExchange(request.name(), request.durable(), request.autoDelete());
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + request.type());
        };
    }

    private Binding buildBinding(BindingRequest request) {
        return new Binding(request.queue(), Binding.DestinationType.QUEUE, request.exchange(),
                request.routingKey(), null);
    }

    private Queue durableWithDeadLetter(String name) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(X_DEAD_LETTER_EXCHANGE, messaging.getDlx());
        arguments.put(X_DEAD_LETTER_ROUTING_KEY, messaging.getDlqRoutingKey());
        return new Queue(name, true, false, false, arguments);
    }

    private QueueInfo toQueueInfo(Queue queue) {
        QueueInformation info = amqpAdmin.getQueueInfo(queue.getName());
        Integer messageCount = info != null ? info.getMessageCount() : null;
        Integer consumerCount = info != null ? info.getConsumerCount() : null;
        return new QueueInfo(queue.getName(), queue.isDurable(), queue.isAutoDelete(), queue.isExclusive(),
                messageCount, consumerCount, queue.getArguments());
    }

    private ExchangeInfo toExchangeInfo(Exchange exchange) {
        return new ExchangeInfo(exchange.getName(), exchange.getType(), exchange.isDurable(),
                exchange.isAutoDelete());
    }

    private void requireManagedQueue(String name) {
        if (!managedQueues.containsKey(name)) {
            throw new ResourceNotFoundException("La cola no está administrada: " + name);
        }
    }

    private void requireManagedExchange(String name) {
        if (!managedExchanges.containsKey(name)) {
            throw new ResourceNotFoundException("El exchange no está administrado: " + name);
        }
    }

    private void registerQueue(Queue queue) {
        managedQueues.put(queue.getName(), queue);
    }

    private void registerExchange(Exchange exchange) {
        managedExchanges.put(exchange.getName(), exchange);
    }

    private void registerBinding(String queue, String exchange, String routingKey) {
        Binding binding = new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
        managedBindings.put(bindingKey(binding), binding);
    }

    private String bindingKey(Binding binding) {
        return binding.getExchange() + "|" + binding.getDestination() + "|" + binding.getRoutingKey();
    }
}
