package com.duocuc.ms_andesstay_kafkaadmin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Verifica los endpoints de lectura del admin contra el clúster local.
 *
 * <p>Requiere Kafka en {@code localhost:9092,9093,9094} y al menos los tópicos
 * del compose. El perfil de seguridad se satisface con {@code @WithMockUser}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class KafkaAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "Admin")
    void listTopicsIncluyeLosTopicosDelCaso() throws Exception {
        mockMvc.perform(get("/api/kafkaadmin/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'reservations.events')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'audit.timeline')]").exists());
    }

    @Test
    @WithMockUser(roles = "Admin")
    void describeConsumerGroupReport() throws Exception {
        mockMvc.perform(get("/api/kafkaadmin/consumer-groups/andesstay-report-group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value("andesstay-report-group"))
                .andExpect(jsonPath("$.partitions").isArray());
    }

    @Test
    @WithMockUser(roles = "Admin")
    void declaredTopicsExponeLaTopologia() throws Exception {
        mockMvc.perform(get("/api/kafkaadmin/topics/declared"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'reservations.events')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'reservations.events.DLT')]").exists());
    }

    @Test
    @WithMockUser(roles = "Admin")
    void creaYeliminaUnTopicoAdHoc() throws Exception {
        String name = "ep5.test." + System.currentTimeMillis();

        mockMvc.perform(post("/api/kafkaadmin/topics")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"%s","partitions":1,"replicationFactor":3,
                                 "configs":{"retention.ms":"3600000"}}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.partitions").value(1));

        mockMvc.perform(delete("/api/kafkaadmin/topics/" + name))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "Admin")
    void loadTestPublicaEventos() throws Exception {
        mockMvc.perform(post("/api/kafkaadmin/load-test")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"count":5,"topic":"reservations.events"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(5))
                .andExpect(jsonPath("$.durationMs").isNumber());
    }
}
