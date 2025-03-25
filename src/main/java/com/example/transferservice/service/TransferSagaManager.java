package com.example.transferservice.service;

import com.example.transferservice.entity.TransferSaga;
import com.example.transferservice.entity.TransferSaga.TransferSagaState;
import com.example.transferservice.messages.*;
import com.example.transferservice.repository.TransferSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferSagaManager {

    private final TransferSagaRepository transferSagaRepository;
    private final StreamBridge streamBridge;

    @Transactional
    public void startTransferSaga(TransferSaga transferSaga) {
        log.info("Avvio saga di trasferimento con ID: {}", transferSaga.getSagaId());
        checkClubBudget(transferSaga);
    }

    // Fase 1: Verifica budget del club
    private void checkClubBudget(TransferSaga transferSaga) {
        log.info("Verifica budget del club: {}", transferSaga.getFromClubId());
        
        CheckClubBudgetRequest request = CheckClubBudgetRequest.builder()
                .sagaId(transferSaga.getSagaId())
                .clubId(transferSaga.getFromClubId())
                .transferFee(transferSaga.getTransferFee())
                .build();
        
        streamBridge.send("checkClubBudgetRequest-out-0", request);
    }

    // Fase 2: Verifica disponibilità giocatore
    private void checkPlayerAvailability(TransferSaga transferSaga) {
        log.info("Verifica disponibilità giocatore: {}", transferSaga.getPlayerId());
        
        CheckPlayerAvailabilityRequest request = CheckPlayerAvailabilityRequest.builder()
                .sagaId(transferSaga.getSagaId())
                .playerId(transferSaga.getPlayerId())
                .toClubId(transferSaga.getToClubId())
                .build();
        
        streamBridge.send("checkPlayerAvailabilityRequest-out-0", request);
    }

    // Fase 3: Aggiornamento club del giocatore
    private void updatePlayerClub(TransferSaga transferSaga) {
        log.info("Aggiornamento club del giocatore: {} al club: {}", 
                transferSaga.getPlayerId(), transferSaga.getToClubId());
        
        UpdatePlayerClubRequest request = UpdatePlayerClubRequest.builder()
                .sagaId(transferSaga.getSagaId())
                .playerId(transferSaga.getPlayerId())
                .newClubId(transferSaga.getToClubId())
                .build();
        
        streamBridge.send("updatePlayerClubRequest-out-0", request);
    }

    // Fase 4: Aggiornamento budget del club
    private void updateClubBudget(TransferSaga transferSaga) {
        log.info("Aggiornamento budget del club: {}", transferSaga.getFromClubId());
        
        UpdateClubBudgetRequest request = UpdateClubBudgetRequest.builder()
                .sagaId(transferSaga.getSagaId())
                .clubId(transferSaga.getFromClubId())
                .transferFee(transferSaga.getTransferFee())
                .build();
        
        streamBridge.send("updateClubBudgetRequest-out-0", request);
    }

    // Completamento del saga
    private void completeSaga(TransferSaga transferSaga) {
        log.info("Completamento saga di trasferimento: {}", transferSaga.getSagaId());
        transferSaga.setCurrentState(TransferSagaState.COMPLETED);
        transferSagaRepository.save(transferSaga);
    }

    // Gestione fallimento del saga
    private void failSaga(TransferSaga transferSaga, String errorMessage) {
        log.error("Fallimento saga di trasferimento: {}, errore: {}", transferSaga.getSagaId(), errorMessage);
        transferSaga.setCurrentState(TransferSagaState.FAILED);
        transferSaga.setErrorMessage(errorMessage);
        transferSagaRepository.save(transferSaga);
    }

    // Compensazione: Ripristino club del giocatore
    private void compensatePlayerClubUpdate(TransferSaga transferSaga) {
        log.info("Compensazione: ripristino club del giocatore: {} al club originale: {}", 
                transferSaga.getPlayerId(), transferSaga.getFromClubId());
        
        UpdatePlayerClubRequest request = UpdatePlayerClubRequest.builder()
                .sagaId(transferSaga.getSagaId())
                .playerId(transferSaga.getPlayerId())
                .newClubId(transferSaga.getFromClubId())
                .build();
        
        streamBridge.send("updatePlayerClubRequest-out-0", request);
    }

    // Gestori di risposta

    @Transactional
    public void handleCheckClubBudgetResponse(CheckClubBudgetResponse response) {
        Optional<TransferSaga> optionalSaga = transferSagaRepository.findBySagaId(response.getSagaId());
        
        if (optionalSaga.isEmpty()) {
            log.error("Saga non trovato con ID: {}", response.getSagaId());
            return;
        }
        
        TransferSaga saga = optionalSaga.get();
        
        if (response.isBudgetAvailable()) {
            log.info("Budget del club verificato con successo: {}", saga.getFromClubId());
            saga.setCurrentState(TransferSagaState.CLUB_BUDGET_CHECKED);
            transferSagaRepository.save(saga);
            checkPlayerAvailability(saga);
        } else {
            log.error("Budget del club non disponibile: {}", saga.getFromClubId());
            failSaga(saga, "Budget del club non disponibile: " + response.getErrorMessage());
        }
    }

    @Transactional
    public void handleCheckPlayerAvailabilityResponse(CheckPlayerAvailabilityResponse response) {
        Optional<TransferSaga> optionalSaga = transferSagaRepository.findBySagaId(response.getSagaId());
        
        if (optionalSaga.isEmpty()) {
            log.error("Saga non trovato con ID: {}", response.getSagaId());
            return;
        }
        
        TransferSaga saga = optionalSaga.get();
        
        if (response.isPlayerAvailable()) {
            log.info("Disponibilità giocatore verificata con successo: {}", saga.getPlayerId());
            saga.setCurrentState(TransferSagaState.PLAYER_AVAILABILITY_CHECKED);
            transferSagaRepository.save(saga);
            updatePlayerClub(saga);
        } else {
            log.error("Giocatore non disponibile: {}", saga.getPlayerId());
            failSaga(saga, "Giocatore non disponibile: " + response.getErrorMessage());
        }
    }

    @Transactional
    public void handleUpdatePlayerClubResponse(UpdatePlayerClubResponse response) {
        Optional<TransferSaga> optionalSaga = transferSagaRepository.findBySagaId(response.getSagaId());
        
        if (optionalSaga.isEmpty()) {
            log.error("Saga non trovato con ID: {}", response.getSagaId());
            return;
        }
        
        TransferSaga saga = optionalSaga.get();
        
        if (response.isUpdated()) {
            log.info("Club del giocatore aggiornato con successo: {}", saga.getPlayerId());
            saga.setCurrentState(TransferSagaState.PLAYER_CLUB_UPDATED);
            transferSagaRepository.save(saga);
            updateClubBudget(saga);
        } else {
            log.error("Impossibile aggiornare il club del giocatore: {}", saga.getPlayerId());
            failSaga(saga, "Impossibile aggiornare il club del giocatore: " + response.getErrorMessage());
            // Non è necessaria compensazione perché non ci sono operazioni precedenti da annullare
        }
    }

    @Transactional
    public void handleUpdateClubBudgetResponse(UpdateClubBudgetResponse response) {
        Optional<TransferSaga> optionalSaga = transferSagaRepository.findBySagaId(response.getSagaId());
        
        if (optionalSaga.isEmpty()) {
            log.error("Saga non trovato con ID: {}", response.getSagaId());
            return;
        }
        
        TransferSaga saga = optionalSaga.get();
        
        if (response.isUpdated()) {
            log.info("Budget del club aggiornato con successo: {}", saga.getFromClubId());
            saga.setCurrentState(TransferSagaState.CLUB_BUDGET_UPDATED);
            transferSagaRepository.save(saga);
            completeSaga(saga);
        } else {
            log.error("Impossibile aggiornare il budget del club: {}", saga.getFromClubId());
            failSaga(saga, "Impossibile aggiornare il budget del club: " + response.getErrorMessage());
            // Compensazione: ripristino club del giocatore
            compensatePlayerClubUpdate(saga);
        }
    }
}
