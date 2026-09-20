package com.duocuc.ms_andesstay_notify.controller;

import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/notify")
@CrossOrigin(origins = "*")
public class NotifyController {

    private final RabbitTemplate rabbitTemplate;

    @Value("${andesstay.rabbitmq.exchange}")
    private String exchange;

    public NotifyController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationEvent event) {
        event.setNotificationId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());

        String routingKey = switch (event.getChannel() != null ? event.getChannel().toUpperCase() : "EMAIL") {
            case "SMS" -> "notify.sms";
            case "PUSH" -> "notify.push";
            default -> "notify.email";
        };

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        return ResponseEntity.ok("Notificación enviada a RabbitMQ con routingKey: " + routingKey);
    }
}