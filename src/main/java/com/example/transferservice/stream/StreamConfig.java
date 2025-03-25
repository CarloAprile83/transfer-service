package com.example.transferservice.stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.transferservice.messages.*;
import com.example.transferservice.service.TransferSagaManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StreamConfig {

    private final TransferSagaManager transferSagaManager;

    // Consumers per le risposte dai servizi
    @Bean
    public Consumer<Message<CheckClubBudgetResponse>> checkClubBudgetResponse() {
        return message -> {
            try {
                log.info("Raw message received: {}", message);
                log.info("Headers: {}", message.getHeaders());
                log.info("Payload: {}", message.getPayload());
                transferSagaManager.handleCheckClubBudgetResponse(message.getPayload());
            } catch (Exception e) {
                log.error("Error processing message: {}", e.getMessage(), e);
            }
        };
    }

    @Bean
    public Consumer<Message<CheckPlayerAvailabilityResponse>> checkPlayerAvailabilityResponse() {
        return message -> {
            log.info("Ricevuta risposta verifica disponibilità giocatore: {}", message.getPayload());
            transferSagaManager.handleCheckPlayerAvailabilityResponse(message.getPayload());
        };
    }

    @Bean
    public Consumer<Message<UpdatePlayerClubResponse>> updatePlayerClubResponse() {
        return message -> {
            log.info("Ricevuta risposta aggiornamento club giocatore: {}", message.getPayload());
            transferSagaManager.handleUpdatePlayerClubResponse(message.getPayload());
        };
    }

    @Bean
    public Consumer<Message<UpdateClubBudgetResponse>> updateClubBudgetResponse() {
        return message -> {
            log.info("Ricevuta risposta aggiornamento budget club: {}", message.getPayload());
            transferSagaManager.handleUpdateClubBudgetResponse(message.getPayload());
        };
    }
}
