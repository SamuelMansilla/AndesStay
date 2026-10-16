package com.duocuc.ms_andesstay_mqadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duocuc.ms_andesstay_mqadmin.exception.ResourceNotFoundException;
import com.duocuc.ms_andesstay_mqadmin.model.QueueInfo;
import com.duocuc.ms_andesstay_mqadmin.service.RabbitAdminService;
import com.duocuc.ms_andesstay_mqadmin.service.dto.QueueRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la validación y los códigos HTTP de {@link MqAdminController} (indicadores 6 y 8)
 * con el servicio mockeado y los filtros de seguridad desactivados. No requiere broker.
 */
@WebMvcTest(controllers = MqAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class MqAdminControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdminService rabbitAdminService;

    @Test
    void listQueuesReturns200() throws Exception {
        when(rabbitAdminService.listQueues()).thenReturn(List.of());

        mockMvc.perform(get("/api/mqadmin/queues"))
                .andExpect(status().isOk());
    }

    @Test
    void createQueueWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/mqadmin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void createQueueWithAmqPrefixReturns400() throws Exception {
        mockMvc.perform(post("/api/mqadmin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"amq.demo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void createQueueValidReturns201() throws Exception {
        when(rabbitAdminService.createQueue(any(QueueRequest.class)))
                .thenReturn(new QueueInfo("q.nueva", true, false, false, 0, 0, null));

        mockMvc.perform(post("/api/mqadmin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"q.nueva\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("q.nueva"));
    }

    @Test
    void createExchangeWithInvalidTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/mqadmin/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ex.demo\",\"type\":\"cuadrado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.type").exists());
    }

    @Test
    void deleteUnknownQueueReturns404() throws Exception {
        doThrow(new ResourceNotFoundException("La cola no está administrada: q.inexistente"))
                .when(rabbitAdminService).deleteQueue("q.inexistente");

        mockMvc.perform(delete("/api/mqadmin/queues/q.inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("La cola no está administrada: q.inexistente"));
    }

    @Test
    void deleteQueueWithInvalidNameReturns400() throws Exception {
        mockMvc.perform(delete("/api/mqadmin/queues/ab"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteQueueValidReturns204() throws Exception {
        mockMvc.perform(delete("/api/mqadmin/queues/q.vieja"))
                .andExpect(status().isNoContent());
    }
}
