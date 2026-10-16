package com.duocuc.ms_andesstay_mqadmin.controller;

import com.duocuc.ms_andesstay_mqadmin.dto.BindingRequestDTO;
import com.duocuc.ms_andesstay_mqadmin.dto.ExchangeRequestDTO;
import com.duocuc.ms_andesstay_mqadmin.dto.QueueRequestDTO;
import com.duocuc.ms_andesstay_mqadmin.dto.ResourceNaming;
import com.duocuc.ms_andesstay_mqadmin.model.BindingInfo;
import com.duocuc.ms_andesstay_mqadmin.model.ExchangeInfo;
import com.duocuc.ms_andesstay_mqadmin.model.QueueInfo;
import com.duocuc.ms_andesstay_mqadmin.service.RabbitAdminService;
import com.duocuc.ms_andesstay_mqadmin.service.dto.BindingRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.ExchangeRequest;
import com.duocuc.ms_andesstay_mqadmin.service.dto.QueueRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST de administración de RabbitMQ (indicador 6 de la pauta).
 *
 * <p>Controlador <b>delgado</b>: solo valida la entrada ({@code @Valid}) y delega
 * en {@link RabbitAdminService}. No conoce ningún detalle de AMQP. Los errores se
 * traducen a HTTP en {@code MqAdminExceptionHandler}.</p>
 *
 * <table>
 *   <tr><th>Método</th><th>Ruta</th><th>Acción</th></tr>
 *   <tr><td>GET</td><td>/api/mqadmin/queues</td><td>Listar colas</td></tr>
 *   <tr><td>POST</td><td>/api/mqadmin/queues</td><td>Crear cola → 201</td></tr>
 *   <tr><td>DELETE</td><td>/api/mqadmin/queues/{name}</td><td>Eliminar cola → 204</td></tr>
 *   <tr><td>POST</td><td>/api/mqadmin/queues/{name}/purge</td><td>Purgar cola</td></tr>
 *   <tr><td>GET</td><td>/api/mqadmin/exchanges</td><td>Listar exchanges</td></tr>
 *   <tr><td>POST</td><td>/api/mqadmin/exchanges</td><td>Crear exchange → 201</td></tr>
 *   <tr><td>DELETE</td><td>/api/mqadmin/exchanges/{name}</td><td>Eliminar exchange → 204</td></tr>
 *   <tr><td>GET</td><td>/api/mqadmin/bindings</td><td>Listar bindings</td></tr>
 *   <tr><td>POST</td><td>/api/mqadmin/bindings</td><td>Crear binding → 201</td></tr>
 *   <tr><td>DELETE</td><td>/api/mqadmin/bindings</td><td>Eliminar binding → 204</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/mqadmin")
@Validated
@PreAuthorize("hasRole('Admin')")
public class MqAdminController {

    private final RabbitAdminService rabbitAdminService;

    public MqAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    // ── Colas ─────────────────────────────────────────────────────────────────

    @GetMapping("/queues")
    public List<QueueInfo> listQueues() {
        return rabbitAdminService.listQueues();
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public QueueInfo createQueue(@Valid @RequestBody QueueRequestDTO request) {
        return rabbitAdminService.createQueue(new QueueRequest(
                request.getName(),
                Boolean.TRUE.equals(request.getDurable()),
                Boolean.TRUE.equals(request.getAutoDelete()),
                request.getTtlMillis(),
                Boolean.TRUE.equals(request.getDeadLetter())));
    }

    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQueue(@PathVariable @Pattern(regexp = ResourceNaming.NAME_PATTERN,
            message = ResourceNaming.NAME_MESSAGE) String name) {
        rabbitAdminService.deleteQueue(name);
    }

    @PostMapping("/queues/{name}/purge")
    public Map<String, Object> purgeQueue(@PathVariable @Pattern(regexp = ResourceNaming.NAME_PATTERN,
            message = ResourceNaming.NAME_MESSAGE) String name) {
        int purged = rabbitAdminService.purgeQueue(name);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("queue", name);
        response.put("purged", purged);
        return response;
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @GetMapping("/exchanges")
    public List<ExchangeInfo> listExchanges() {
        return rabbitAdminService.listExchanges();
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeInfo createExchange(@Valid @RequestBody ExchangeRequestDTO request) {
        return rabbitAdminService.createExchange(new ExchangeRequest(
                request.getName(),
                request.getType(),
                Boolean.TRUE.equals(request.getDurable()),
                Boolean.TRUE.equals(request.getAutoDelete())));
    }

    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExchange(@PathVariable @Pattern(regexp = ResourceNaming.NAME_PATTERN,
            message = ResourceNaming.NAME_MESSAGE) String name) {
        rabbitAdminService.deleteExchange(name);
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @GetMapping("/bindings")
    public List<BindingInfo> listBindings() {
        return rabbitAdminService.listBindings();
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public BindingInfo createBinding(@Valid @RequestBody BindingRequestDTO request) {
        return rabbitAdminService.createBinding(toBindingRequest(request));
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBinding(@Valid @RequestBody BindingRequestDTO request) {
        rabbitAdminService.deleteBinding(toBindingRequest(request));
    }

    private BindingRequest toBindingRequest(BindingRequestDTO request) {
        return new BindingRequest(request.getQueue(), request.getExchange(), request.getRoutingKey());
    }
}
