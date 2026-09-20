package cl.duoc.andesstay.ms_andesstay_bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping({"/api/v1/catalog", "/api/v1/units"})
public class BffCatalogController {

    private final WebClient webClient;

    public BffCatalogController(WebClient.Builder webClientBuilder,
                                @Value("${services.catalog.url:http://catalog-svc:8082}") String catalogUrl) {
        this.webClient = webClientBuilder.baseUrl(catalogUrl).build();
    }

    @GetMapping
    public ResponseEntity<String> getAllUnits() {
        String response = this.webClient.get()
                .uri("/api/v1/units")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getUnitById(@PathVariable Long id) {
        String response = this.webClient.get()
                .uri("/api/v1/units/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return ResponseEntity.ok(response);
    }
}