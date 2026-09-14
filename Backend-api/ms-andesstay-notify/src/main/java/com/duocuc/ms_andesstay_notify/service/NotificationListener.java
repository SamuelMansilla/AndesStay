package com.duocuc.ms_andesstay_notify.service;

import com.duocuc.ms_andesstay_notify.model.NotificationEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    /**
     * Consume mensajes para envío de Email/Push al huésped[cite: 2].
     */
    @RabbitListener(queues = "q.cmd.email")
    public void processEmailCommand(NotificationEvent event) {
        System.out.println("Procesando envío de EMAIL. TraceID: " + event.getTraceId());
        System.out.println("Payload: " + event.getPayload());
        // Aquí iría la lógica de integración con un SMTP o servicio como SendGrid
    }

    /**
     * Consume mensajes para generar el ticket de preparación de unidad[cite: 2].
     */
    @RabbitListener(queues = "q.cmd.housekeeping")
    public void processHousekeepingCommand(NotificationEvent event) {
        System.out.println("Procesando ticket de HOUSEKEEPING. TraceID: " + event.getTraceId());
        System.out.println("Payload: " + event.getPayload());
        // Aquí iría la lógica para enviar el ticket al sistema del personal de limpieza
    }

    /**
     * Consume mensajes para la generación de PDF (voucher de reserva o boleta)[cite: 2].
     */
    @RabbitListener(queues = "q.cmd.voucher")
    public void processVoucherCommand(NotificationEvent event) {
        System.out.println("Procesando generación de VOUCHER PDF. TraceID: " + event.getTraceId());
        System.out.println("Payload: " + event.getPayload());
        // Aquí iría la lógica de generación del PDF y almacenamiento
    }
}