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

import java.util.Optional;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*")
public class BffReportController {

    private final WebClient webClient;

    public BffReportController(WebClient.Builder webClientBuilder,
                               @Value("${services.report.url:http://localhost:8084}") String reportUrl) {
        this.webClient = webClientBuilder.baseUrl(reportUrl).build();
    }

    @GetMapping("/kpis")
    public Mono<ResponseEntity<String>> getKpis(
            @RequestParam(required = false, defaultValue = "last24h") String range,
            @AuthenticationPrincipal Jwt jwt) {

        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/report/kpis")
                        .queryParamIfPresent("range", Optional.ofNullable(range))
                        .build())
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }

    @GetMapping("/top-units")
    public Mono<ResponseEntity<String>> getTopUnits(
            @RequestParam(required = false, defaultValue = "last7d") String range,
            @AuthenticationPrincipal Jwt jwt) {

        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/report/top-units")
                        .queryParamIfPresent("range", Optional.ofNullable(range))
                        .build())
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }
}
