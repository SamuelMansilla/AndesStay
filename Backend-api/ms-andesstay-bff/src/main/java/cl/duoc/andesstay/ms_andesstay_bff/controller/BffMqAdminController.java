package cl.duoc.andesstay.ms_andesstay_bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Proxy del BFF hacia el microservicio administrador de RabbitMQ
 * ({@code ms-andesstay-mqadmin}, puerto 8086).
 *
 * <p>El BFF ya valida el JWT; aquí solo propaga el {@code Authorization: Bearer}
 * hacia el microservicio, que es quien vuelve a validarlo y exige el rol Admin.
 * Mantiene las mismas rutas {@code /api/mqadmin/**} que expone el frontend.</p>
 */
@RestController
@RequestMapping("/api/mqadmin")
@CrossOrigin(origins = "*")
public class BffMqAdminController {

    private final WebClient webClient;

    public BffMqAdminController(
            WebClient.Builder webClientBuilder,
            @Value("${services.mqadmin.url}") String mqadminServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(mqadminServiceUrl).build();
    }

    // ── Colas ─────────────────────────────────────────────────────────────────

    @GetMapping("/queues")
    public Mono<ResponseEntity<String>> listQueues(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/mqadmin/queues", null, jwt);
    }

    @PostMapping("/queues")
    public Mono<ResponseEntity<String>> createQueue(@RequestBody String body,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/mqadmin/queues", body, jwt);
    }

    @DeleteMapping("/queues/{name}")
    public Mono<ResponseEntity<String>> deleteQueue(@PathVariable String name,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.DELETE, "/api/mqadmin/queues/{name}", null, jwt, name);
    }

    @PostMapping("/queues/{name}/purge")
    public Mono<ResponseEntity<String>> purgeQueue(@PathVariable String name,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/mqadmin/queues/{name}/purge", null, jwt, name);
    }

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @GetMapping("/exchanges")
    public Mono<ResponseEntity<String>> listExchanges(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/mqadmin/exchanges", null, jwt);
    }

    @PostMapping("/exchanges")
    public Mono<ResponseEntity<String>> createExchange(@RequestBody String body,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/mqadmin/exchanges", body, jwt);
    }

    @DeleteMapping("/exchanges/{name}")
    public Mono<ResponseEntity<String>> deleteExchange(@PathVariable String name,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.DELETE, "/api/mqadmin/exchanges/{name}", null, jwt, name);
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @GetMapping("/bindings")
    public Mono<ResponseEntity<String>> listBindings(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/mqadmin/bindings", null, jwt);
    }

    @PostMapping("/bindings")
    public Mono<ResponseEntity<String>> createBinding(@RequestBody String body,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/mqadmin/bindings", body, jwt);
    }

    @DeleteMapping("/bindings")
    public Mono<ResponseEntity<String>> deleteBinding(@RequestBody String body,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.DELETE, "/api/mqadmin/bindings", body, jwt);
    }

    private Mono<ResponseEntity<String>> exchange(HttpMethod method, String uri, String body,
            Jwt jwt, Object... uriVars) {
        WebClient.RequestBodySpec request = webClient.method(method)
                .uri(uri, uriVars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        if (body == null) {
            return request.retrieve().toEntity(String.class);
        }
        return request.contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toEntity(String.class);
    }
}
