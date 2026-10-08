package cl.duoc.andesstay.ms_andesstay_bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class BffAuditController {

    private final WebClient webClient;

    public BffAuditController(WebClient.Builder webClientBuilder,
                              @Value("${services.audit.url:http://localhost:8085}") String auditUrl) {
        this.webClient = webClientBuilder.baseUrl(auditUrl).build();
    }

    // GET /api/audit — propaga filtros opcionales: actor, eventType, reservationId
    @GetMapping
    public Mono<ResponseEntity<String>> getTimeline(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long reservationId,
            @AuthenticationPrincipal Jwt jwt) {

        String uri = UriComponentsBuilder.fromPath("/api/audit")
                .queryParamIfPresent("actor", java.util.Optional.ofNullable(actor))
                .queryParamIfPresent("eventType", java.util.Optional.ofNullable(eventType))
                .queryParamIfPresent("reservationId", java.util.Optional.ofNullable(reservationId))
                .build().toUriString();

        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }

    // GET /api/audit/actor/{actor}
    @GetMapping("/actor/{actor}")
    public Mono<ResponseEntity<String>> getTimelineByActor(
            @PathVariable String actor,
            @AuthenticationPrincipal Jwt jwt) {

        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri("/api/audit/actor/{actor}", actor)
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }

    // GET /api/audit/reservation/{reservationId}
    @GetMapping("/reservation/{reservationId}")
    public Mono<ResponseEntity<String>> getTimelineByReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Jwt jwt) {

        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri("/api/audit/reservation/{reservationId}", reservationId)
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }
}
