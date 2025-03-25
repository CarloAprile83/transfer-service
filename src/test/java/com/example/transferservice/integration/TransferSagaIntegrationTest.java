package com.example.transferservice.integration;

import com.example.transferservice.controller.TransferRequest;
import com.example.transferservice.entity.TransferSaga;
import com.example.transferservice.repository.TransferSagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di integrazione per il flusso completo del Saga.
 * Nota: Questo test richiede che tutti i servizi siano in esecuzione e configurati correttamente.
 * È necessario avere PostgreSQL e Kafka in esecuzione con le configurazioni specificate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TransferSagaIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransferSagaRepository transferSagaRepository;

    /**
     * Test del flusso completo del Saga con successo.
     * Questo test verifica che un trasferimento venga completato con successo quando tutti i passaggi sono validi.
     */
    @Test
    public void testSuccessfulTransferSaga() throws Exception {
        // Dati di test
        Long playerId = 1L;
        Long fromClubId = 2L;
        Long toClubId = 3L;
        BigDecimal transferFee = new BigDecimal("1000000");

        // Crea la richiesta di trasferimento
        TransferRequest request = new TransferRequest(playerId, fromClubId, toClubId, transferFee);
        
        // Configura l'intestazione HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TransferRequest> entity = new HttpEntity<>(request, headers);
        
        // Invia la richiesta POST
        ResponseEntity<String> response = restTemplate.postForEntity("/transfers", entity, String.class);
        
        // Verifica che la risposta sia accettata (202)
        assertEquals(202, response.getStatusCodeValue());
        
        // Estrai l'ID del saga dalla risposta
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        String sagaId = responseBody.split("\"sagaId\":\"")[1].split("\"")[0];
        
        // Attendi il completamento del saga (max 10 secondi)
        boolean sagaCompleted = false;
        for (int i = 0; i < 10; i++) {
            TimeUnit.SECONDS.sleep(1);
            
            Optional<TransferSaga> optionalSaga = transferSagaRepository.findBySagaId(sagaId);
            if (optionalSaga.isPresent()) {
                TransferSaga saga = optionalSaga.get();
                if (saga.getCurrentState() == TransferSaga.TransferSagaState.COMPLETED || 
                    saga.getCurrentState() == TransferSaga.TransferSagaState.FAILED) {
                    sagaCompleted = true;
                    assertEquals(TransferSaga.TransferSagaState.COMPLETED, saga.getCurrentState());
                    break;
                }
            }
        }
        
        assertTrue(sagaCompleted, "Il saga non è stato completato entro il timeout");
    }

    /**
     * Test del flusso del Saga con fallimento e compensazione.
     * Questo test verifica che il saga gestisca correttamente un fallimento e attivi la compensazione.
     */
    @Test
    public void testFailedTransferSagaWithCompensation() throws Exception {
        // Dati di test con un importo di trasferimento troppo alto
        Long playerId = 1L;
        Long fromClubId = 2L;
        Long toClubId = 3L;
        BigDecimal transferFee = new BigDecimal("100000000"); // Importo molto alto che dovrebbe fallire

        // Crea la richiesta di trasferimento
        TransferRequest request = new TransferRequest(playerId, fromClubId, toClubId, transferFee);
        
        // Configura l'intestazione HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TransferRequest> entity = new HttpEntity<>(request, headers);
        
        // Invia la richiesta POST
        ResponseEntity<String> response = restTemplate.postForEntity("/transfers", entity, String.class);
        
        // Verifica che la risposta sia accettata (202)
        assertEquals(202, response.getStatusCodeValue());
        
        // Estrai l'ID del saga dalla risposta
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        String sagaId = responseBody.split("\"sagaId\":\"")[1].split("\"")[0];
        
        // Attendi il completamento del saga (max 10 secondi)
        boolean sagaCompleted = false;
        for (int i = 0; i < 10; i++) {
            TimeUnit.SECONDS.sleep(1);
            
            Optional<TransferSaga> optionalSaga = transferSagaRepository.findBySagaId(sagaId);
            if (optionalSaga.isPresent()) {
                TransferSaga saga = optionalSaga.get();
                if (saga.getCurrentState() == TransferSaga.TransferSagaState.COMPLETED || 
                    saga.getCurrentState() == TransferSaga.TransferSagaState.FAILED) {
                    sagaCompleted = true;
                    assertEquals(TransferSaga.TransferSagaState.FAILED, saga.getCurrentState());
                    assertNotNull(saga.getErrorMessage());
                    break;
                }
            }
        }
        
        assertTrue(sagaCompleted, "Il saga non è stato completato entro il timeout");
    }
}
