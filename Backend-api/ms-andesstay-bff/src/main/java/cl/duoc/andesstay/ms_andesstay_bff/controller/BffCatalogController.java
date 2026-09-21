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
@RequestMapping({"/api/catalog", "/api/v1/catalog", "/api/units", "/api/v1/units"})
@CrossOrigin(origins = "*")
public class BffCatalogController {

    private final WebClient webClient;

    public BffCatalogController(WebClient.Builder webClientBuilder,
                                @Value("${services.catalog.url:http://localhost:8082}") String catalogUrl) {
        this.webClient = webClientBuilder.baseUrl(catalogUrl).build();
    }

    @GetMapping({"", "/units"})
    public Mono<ResponseEntity<String>> getAllUnits(@AuthenticationPrincipal Jwt jwt) {
        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri("/api/catalog/units")
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }

    @GetMapping({"/units/{id}", "/{id}"})
    public Mono<ResponseEntity<String>> getUnitById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        WebClient.RequestHeadersSpec<?> spec = this.webClient.get()
                .uri("/api/catalog/units/{id}", id)
                .accept(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.retrieve().toEntity(String.class);
    }

    @PostMapping({"", "/units"})
    public Mono<ResponseEntity<String>> createUnit(
            @RequestBody String unitJson,
            @AuthenticationPrincipal Jwt jwt) {

        WebClient.RequestBodySpec spec = this.webClient.post()
                .uri("/api/catalog/units")
                .contentType(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.bodyValue(unitJson).retrieve().toEntity(String.class);
    }

    @PutMapping({"/units/{id}", "/{id}"})
    public Mono<ResponseEntity<String>> updateUnit(
            @PathVariable Long id,
            @RequestBody String unitJson,
            @AuthenticationPrincipal Jwt jwt) {

        WebClient.RequestBodySpec spec = this.webClient.put()
                .uri("/api/catalog/units/{id}", id)
                .contentType(MediaType.APPLICATION_JSON);

        if (jwt != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
        }

        return spec.bodyValue(unitJson).retrieve().toEntity(String.class);
    }
}