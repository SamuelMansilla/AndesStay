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
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class BffAuditController {

    private final WebClient webClient;

    public BffAuditController(WebClient.Builder webClientBuilder,
                              @Value("${services.audit.url:http://localhost:8085}") String auditUrl) {
        this.webClient = webClientBuilder.baseUrl(auditUrl).build();
    }

    @GetMapping
    public Mono<ResponseEntity<String>> getTimeline(@AuthenticationPrincipal Jwt jwt) {
        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri("/api/audit")
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }

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
}
