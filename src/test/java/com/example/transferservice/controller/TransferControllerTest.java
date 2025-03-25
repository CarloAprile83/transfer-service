package com.example.transferservice.controller;

import com.example.transferservice.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
public class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferService transferService;

    @Test
    public void testInitiateTransfer() throws Exception {
        // Given
        String sagaId = "test-saga-id";
        when(transferService.initiateTransfer(eq(1L), eq(2L), eq(3L), any(BigDecimal.class)))
                .thenReturn(sagaId);

        // When & Then
        mockMvc.perform(post("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\": 1, \"fromClubId\": 2, \"toClubId\": 3, \"transferFee\": 1000000}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sagaId").value(sagaId))
                .andExpect(jsonPath("$.message").value("Trasferimento avviato"));
    }
}
