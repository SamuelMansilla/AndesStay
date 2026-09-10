package cl.duoc.andesstay.ms_andesstay_bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class BffReservationsController {

    private final WebClient webClient;

    public BffReservationsController(
            WebClient.Builder webClientBuilder,
            @Value("${services.reservations.url}") String reservationsServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(reservationsServiceUrl).build();
    }

    @GetMapping
    public Mono<ResponseEntity<String>> getAllReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal Jwt jwt) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/reservations")
                        .queryParamIfPresent("status", java.util.Optional.ofNullable(status))
                        .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                        .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                .retrieve()
                .toEntity(String.class);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<String>> getReservationById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return webClient.get()
                .uri("/api/reservations/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping
    public Mono<ResponseEntity<String>> createReservation(
            @RequestBody String reservationJson,
            @AuthenticationPrincipal Jwt jwt) {

        return webClient.post()
                .uri("/api/reservations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reservationJson)
                .retrieve()
                .toEntity(String.class);
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<String>> updateReservationStatus(
            @PathVariable Long id,
            @RequestBody String statusJson,
            @AuthenticationPrincipal Jwt jwt) {

        return webClient.put()
                .uri("/api/reservations/{id}/status", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(statusJson)
                .retrieve()
                .toEntity(String.class);
    }
}