package com.duocuc.ms_andesstay_notify.controller;

import com.duocuc.ms_andesstay_notify.config.MessagingProperties;
import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/notify")
@CrossOrigin(origins = "*")
public class NotifyController {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public NotifyController(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationEvent event) {
        event.setNotificationId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());

        MessagingProperties.RoutingKeys routingKeys = messagingProperties.getRoutingKeys();
        String routingKey = switch (event.getChannel() != null ? event.getChannel().toUpperCase() : "EMAIL") {
            case "SMS" -> routingKeys.getNotifySms();
            case "PUSH" -> routingKeys.getNotifyPush();
            default -> routingKeys.getNotifyEmail();
        };

        rabbitTemplate.convertAndSend(messagingProperties.getExchange(), routingKey, event);
        return ResponseEntity.ok("Notificación enviada a RabbitMQ con routingKey: " + routingKey);
    }
}