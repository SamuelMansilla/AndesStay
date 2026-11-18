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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Proxy del BFF hacia el administrador de Kafka ({@code ms-andesstay-kafkaadmin},
 * puerto 8087). El BFF valida el JWT y propaga el Bearer; el admin vuelve a
 * validarlo y exige el rol Admin.
 */
@RestController
@RequestMapping("/api/kafkaadmin")
@CrossOrigin(origins = "*")
public class BffKafkaAdminController {

    private final WebClient webClient;

    public BffKafkaAdminController(
            WebClient.Builder webClientBuilder,
            @Value("${services.kafkaadmin.url}") String kafkaadminServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(kafkaadminServiceUrl).build();
    }

    @GetMapping("/cluster")
    public Mono<ResponseEntity<String>> cluster(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/kafkaadmin/cluster", null, jwt);
    }

    @GetMapping("/topics")
    public Mono<ResponseEntity<String>> listTopics(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/kafkaadmin/topics", null, jwt);
    }

    @GetMapping("/topics/declared")
    public Mono<ResponseEntity<String>> declaredTopics(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/kafkaadmin/topics/declared", null, jwt);
    }

    @GetMapping("/topics/{name}")
    public Mono<ResponseEntity<String>> describeTopic(@PathVariable String name,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/kafkaadmin/topics/{name}", null, jwt, name);
    }

    @PostMapping("/topics")
    public Mono<ResponseEntity<String>> createTopic(@RequestBody String body,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/kafkaadmin/topics", body, jwt);
    }

    @PutMapping("/topics/{name}/configs")
    public Mono<ResponseEntity<String>> alterConfigs(@PathVariable String name,
            @RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.PUT, "/api/kafkaadmin/topics/{name}/configs", body, jwt, name);
    }

    @PostMapping("/topics/{name}/partitions")
    public Mono<ResponseEntity<String>> increasePartitions(@PathVariable String name,
            @RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/kafkaadmin/topics/{name}/partitions", body, jwt, name);
    }

    @DeleteMapping("/topics/{name}")
    public Mono<ResponseEntity<String>> deleteTopic(@PathVariable String name,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.DELETE, "/api/kafkaadmin/topics/{name}", null, jwt, name);
    }

    @PostMapping("/topics/{name}/publish")
    public Mono<ResponseEntity<String>> publish(@PathVariable String name,
            @RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/kafkaadmin/topics/{name}/publish", body, jwt, name);
    }

    @GetMapping("/consumer-groups")
    public Mono<ResponseEntity<String>> listConsumerGroups(@AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/kafkaadmin/consumer-groups", null, jwt);
    }

    @GetMapping("/consumer-groups/{groupId}")
    public Mono<ResponseEntity<String>> describeConsumerGroup(@PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.GET, "/api/kafkaadmin/consumer-groups/{groupId}", null, jwt, groupId);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public Mono<ResponseEntity<String>> deleteConsumerGroup(@PathVariable String groupId,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.DELETE, "/api/kafkaadmin/consumer-groups/{groupId}", null, jwt, groupId);
    }

    @PostMapping("/load-test")
    public Mono<ResponseEntity<String>> loadTest(@RequestBody String body,
            @AuthenticationPrincipal Jwt jwt) {
        return exchange(HttpMethod.POST, "/api/kafkaadmin/load-test", body, jwt);
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
