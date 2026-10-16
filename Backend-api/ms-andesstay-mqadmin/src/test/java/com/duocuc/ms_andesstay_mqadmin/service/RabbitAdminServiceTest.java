package com.duocuc.ms_andesstay_mqadmin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duocuc.ms_andesstay_mqadmin.config.MessagingProperties;
import com.duocuc.ms_andesstay_mqadmin.exception.ResourceNotFoundException;
import com.duocuc.ms_andesstay_mqadmin.model.BindingInfo;
import com.duocuc.ms_andesstay_mqadmin.model.ExchangeInfo;
import com.duocuc.ms_andesstay_mqadmin.model.QueueInfo;
import com.duocuc.ms_andesstay_mqadmin.service.dto.BindingRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.ExchangeRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.QueueRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;

/**
 * Prueba {@link RabbitAdminService} con un {@link AmqpAdmin} de Mockito (indicador 7):
 * verifica que las operaciones se delegan al broker y que el registro interno
 * (sembrado desde la topología declarada + creado en runtime) se mantiene.
 * No requiere broker.
 */
class RabbitAdminServiceTest {

    private AmqpAdmin amqpAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        amqpAdmin = mock(AmqpAdmin.class);
        // Por defecto las colas no existen en el broker => conteos nulos.
        when(amqpAdmin.getQueueInfo(anyString())).thenReturn(null);
        service = new RabbitAdminService(amqpAdmin, messagingProperties());
        service.seedDeclaredTopology();
    }

    @Test
    void seedsDeclaredTopologyWithoutTouchingBroker() {
        assertThat(service.listQueues()).hasSize(6);
        assertThat(service.listExchanges()).extracting(ExchangeInfo::name)
                .containsExactlyInAnyOrder("ex.andesstay.dlx", "ex.andesstay.notifications");
        assertThat(service.listBindings()).hasSize(6);

        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
        verify(amqpAdmin, never()).declareExchange(any());
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void createQueueDeclaresOnBrokerAndRegisters() {
        when(amqpAdmin.declareQueue(any(Queue.class)))
                .thenAnswer(invocation -> ((Queue) invocation.getArgument(0)).getName());

        QueueInfo info = service.createQueue(new QueueRequest("q.nueva.prueba", true, false, 5000, false));

        assertThat(info.name()).isEqualTo("q.nueva.prueba");
        assertThat(info.durable()).isTrue();
        assertThat(info.arguments()).containsEntry("x-message-ttl", 5000);
        verify(amqpAdmin).declareQueue(any(Queue.class));
        assertThat(service.listQueues()).extracting(QueueInfo::name).contains("q.nueva.prueba");
    }

    @Test
    void createQueueAddsDeadLetterArgumentsWhenRequested() {
        when(amqpAdmin.declareQueue(any(Queue.class)))
                .thenAnswer(invocation -> ((Queue) invocation.getArgument(0)).getName());

        QueueInfo info = service.createQueue(new QueueRequest("q.con.dlx", true, false, null, true));

        assertThat(info.arguments())
                .containsEntry("x-dead-letter-exchange", "ex.andesstay.dlx")
                .containsEntry("x-dead-letter-routing-key", "dlq.routing.key");
    }

    @Test
    void createQueueRejectsDuplicate() {
        assertThatThrownBy(() -> service.createQueue(new QueueRequest("q.notify.email", true, false, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está administrada");
        verify(amqpAdmin, never()).declareQueue(any(Queue.class));
    }

    @Test
    void deleteQueueCallsBrokerThenUnregisters() {
        when(amqpAdmin.deleteQueue("q.notify.sms")).thenReturn(true);

        service.deleteQueue("q.notify.sms");

        verify(amqpAdmin).deleteQueue("q.notify.sms");
        assertThat(service.listQueues()).extracting(QueueInfo::name).doesNotContain("q.notify.sms");
    }

    @Test
    void deleteUnknownQueueThrowsNotFound() {
        assertThatThrownBy(() -> service.deleteQueue("q.inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(amqpAdmin, never()).deleteQueue(anyString());
    }

    @Test
    void purgeQueueReturnsPurgedCount() {
        when(amqpAdmin.purgeQueue("q.notify.push")).thenReturn(3);

        assertThat(service.purgeQueue("q.notify.push")).isEqualTo(3);
        verify(amqpAdmin).purgeQueue("q.notify.push");
    }

    @Test
    void listQueuesIncludesBrokerCounts() {
        when(amqpAdmin.getQueueInfo("q.notify.email"))
                .thenReturn(new QueueInformation("q.notify.email", 7, 2));

        QueueInfo email = service.listQueues().stream()
                .filter(queue -> queue.name().equals("q.notify.email"))
                .findFirst()
                .orElseThrow();

        assertThat(email.messageCount()).isEqualTo(7);
        assertThat(email.consumerCount()).isEqualTo(2);
    }

    @Test
    void createExchangeDeclaresOnBroker() {
        ExchangeInfo info = service.createExchange(new ExchangeRequest("ex.nuevo", "topic", true, false));

        assertThat(info.name()).isEqualTo("ex.nuevo");
        assertThat(info.type()).isEqualTo("topic");
        verify(amqpAdmin).declareExchange(any());
    }

    @Test
    void createExchangeRejectsUnknownType() {
        assertThatThrownBy(() -> service.createExchange(new ExchangeRequest("ex.malo", "cuadrado", true, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de exchange no soportado");
        verify(amqpAdmin, never()).declareExchange(any());
    }

    @Test
    void createBindingDeclaresOnBroker() {
        BindingInfo info = service.createBinding(
                new BindingRequest("q.notify.email", "ex.andesstay.dlx", "otra.key"));

        assertThat(info.routingKey()).isEqualTo("otra.key");
        verify(amqpAdmin).declareBinding(any());
    }

    @Test
    void createBindingRejectsUnknownQueue() {
        assertThatThrownBy(() -> service.createBinding(
                new BindingRequest("q.fantasma", "ex.andesstay.notifications", "rk")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void deleteBindingRemovesManagedBinding() {
        service.deleteBinding(new BindingRequest("q.notify.email", "ex.andesstay.notifications", "notify.email"));

        verify(amqpAdmin).removeBinding(any());
        assertThat(service.listBindings()).hasSize(5);
    }

    private static MessagingProperties messagingProperties() {
        MessagingProperties properties = new MessagingProperties();
        properties.setExchange("ex.andesstay.notifications");
        properties.setDlx("ex.andesstay.dlx");
        properties.setDlq("q.andesstay.dlq");
        properties.setDlqRoutingKey("dlq.routing.key");

        MessagingProperties.Queues queues = new MessagingProperties.Queues();
        queues.setNotifyEmail("q.notify.email");
        queues.setNotifySms("q.notify.sms");
        queues.setNotifyPush("q.notify.push");
        queues.setHousekeepingTickets("q.housekeeping.tickets");
        queues.setDocsGenerate("q.docs.generate");
        properties.setQueues(queues);

        MessagingProperties.RoutingKeys routingKeys = new MessagingProperties.RoutingKeys();
        routingKeys.setNotifyEmail("notify.email");
        routingKeys.setNotifySms("notify.sms");
        routingKeys.setNotifyPush("notify.push");
        routingKeys.setHousekeepingTicket("housekeeping.ticket");
        routingKeys.setDocsGenerate("docs.generate");
        properties.setRoutingKeys(routingKeys);
        return properties;
    }
}
