package com.example.transferservice.service;

import com.example.transferservice.dto.TransferStatusResponse;
import com.example.transferservice.entity.TransferSaga;
import com.example.transferservice.repository.TransferSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferSagaRepository transferSagaRepository;
    private final TransferSagaManager transferSagaManager;

    @Transactional
    public String initiateTransfer(Long playerId, Long fromClubId, Long toClubId, BigDecimal transferFee) {
        log.info("Inizializzazione trasferimento: giocatore={}, da club={}, a club={}, importo={}",
                playerId, fromClubId, toClubId, transferFee);
        
        // Genera un ID univoco per il saga
        String sagaId = UUID.randomUUID().toString();
        
        // Crea e salva l'entità TransferSaga
        TransferSaga transferSaga = TransferSaga.builder()
                .sagaId(sagaId)
                .playerId(playerId)
                .fromClubId(fromClubId)
                .toClubId(toClubId)
                .transferFee(transferFee)
                .currentState(TransferSaga.TransferSagaState.STARTED)
                .build();
        
        transferSagaRepository.save(transferSaga);
        log.info("Creato nuovo TransferSaga con ID: {}", sagaId);
        
        // Avvia il processo di orchestrazione del saga
        transferSagaManager.startTransferSaga(transferSaga);
        
        return sagaId;
    }

    public TransferStatusResponse getTransferStatus(String sagaId) {
    TransferSaga saga = transferSagaRepository.findBySagaId(sagaId)
        .orElseThrow(() -> new RuntimeException("Transfer saga not found with ID: " + sagaId));
        
    return TransferStatusResponse.builder()
        .sagaId(saga.getSagaId())
        .playerId(saga.getPlayerId())
        .fromClubId(saga.getFromClubId())
        .toClubId(saga.getToClubId())
        .transferFee(saga.getTransferFee())
        .state(saga.getCurrentState().name())
        .errorMessage(saga.getErrorMessage())
        .createdAt(saga.getCreatedAt())
        .updatedAt(saga.getUpdatedAt())
        .build();
    }
}
