package com.duocuc.ms_andesstay_notify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${andesstay.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${andesstay.rabbitmq.queue.email}")
    private String emailQueueName;

    @Value("${andesstay.rabbitmq.queue.sms}")
    private String smsQueueName;

    @Value("${andesstay.rabbitmq.queue.push}")
    private String pushQueueName;

    @Value("${andesstay.rabbitmq.queue.dlq}")
    private String dlqName;

    @Value("${andesstay.rabbitmq.exchange.dlx}")
    private String dlxName;

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxName, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dlq.routing.key");
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(emailQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", "dlq.routing.key")
                .build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(smsQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", "dlq.routing.key")
                .build();
    }

    @Bean
    public Queue pushQueue() {
        return QueueBuilder.durable(pushQueueName)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", "dlq.routing.key")
                .build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationsExchange).with("notify.email");
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(smsQueue).to(notificationsExchange).with("notify.sms");
    }

    @Bean
    public Binding pushBinding(Queue pushQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(pushQueue).to(notificationsExchange).with("notify.push");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}