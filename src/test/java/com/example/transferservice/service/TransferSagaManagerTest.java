package com.example.transferservice.service;

import com.example.transferservice.entity.TransferSaga;
import com.example.transferservice.entity.TransferSaga.TransferSagaState;
import com.example.transferservice.messages.*;
import com.example.transferservice.repository.TransferSagaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferSagaManagerTest {

    @Mock
    private TransferSagaRepository transferSagaRepository;

    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private TransferSagaManager transferSagaManager;

    @Test
    public void testStartTransferSaga() {
        // Given
        TransferSaga saga = TransferSaga.builder()
                .sagaId("test-saga-id")
                .playerId(1L)
                .fromClubId(2L)
                .toClubId(3L)
                .transferFee(new BigDecimal("1000000"))
                .currentState(TransferSagaState.STARTED)
                .build();
        
        when(streamBridge.send(eq("checkClubBudgetRequest-out-0"), any(CheckClubBudgetRequest.class))).thenReturn(true);
        
        // When
        transferSagaManager.startTransferSaga(saga);
        
        // Then
        verify(streamBridge, times(1)).send(eq("checkClubBudgetRequest-out-0"), any(CheckClubBudgetRequest.class));
    }

    @Test
    public void testHandleCheckClubBudgetResponse_Success() {
        // Given
        String sagaId = "test-saga-id";
        TransferSaga saga = TransferSaga.builder()
                .sagaId(sagaId)
                .playerId(1L)
                .fromClubId(2L)
                .toClubId(3L)
                .transferFee(new BigDecimal("1000000"))
                .currentState(TransferSagaState.STARTED)
                .build();
        
        CheckClubBudgetResponse response = CheckClubBudgetResponse.builder()
                .sagaId(sagaId)
                .clubId(2L)
                .budgetAvailable(true)
                .build();
        
        when(transferSagaRepository.findBySagaId(sagaId)).thenReturn(Optional.of(saga));
        when(transferSagaRepository.save(any(TransferSaga.class))).thenReturn(saga);
        when(streamBridge.send(eq("checkPlayerAvailabilityRequest-out-0"), any(CheckPlayerAvailabilityRequest.class))).thenReturn(true);
        
        // When
        transferSagaManager.handleCheckClubBudgetResponse(response);
        
        // Then
        verify(transferSagaRepository, times(1)).findBySagaId(sagaId);
        verify(transferSagaRepository, times(1)).save(any(TransferSaga.class));
        verify(streamBridge, times(1)).send(eq("checkPlayerAvailabilityRequest-out-0"), any(CheckPlayerAvailabilityRequest.class));
        assertEquals(TransferSagaState.CLUB_BUDGET_CHECKED, saga.getCurrentState());
    }

    @Test
    public void testHandleCheckClubBudgetResponse_Failure() {
        // Given
        String sagaId = "test-saga-id";
        TransferSaga saga = TransferSaga.builder()
                .sagaId(sagaId)
                .playerId(1L)
                .fromClubId(2L)
                .toClubId(3L)
                .transferFee(new BigDecimal("1000000"))
                .currentState(TransferSagaState.STARTED)
                .build();
        
        CheckClubBudgetResponse response = CheckClubBudgetResponse.builder()
                .sagaId(sagaId)
                .clubId(2L)
                .budgetAvailable(false)
                .errorMessage("Budget insufficiente")
                .build();
        
        when(transferSagaRepository.findBySagaId(sagaId)).thenReturn(Optional.of(saga));
        when(transferSagaRepository.save(any(TransferSaga.class))).thenReturn(saga);
        
        // When
        transferSagaManager.handleCheckClubBudgetResponse(response);
        
        // Then
        verify(transferSagaRepository, times(1)).findBySagaId(sagaId);
        verify(transferSagaRepository, times(1)).save(any(TransferSaga.class));
        verify(streamBridge, never()).send(eq("checkPlayerAvailabilityRequest-out-0"), any(CheckPlayerAvailabilityRequest.class));
        assertEquals(TransferSagaState.FAILED, saga.getCurrentState());
    }

    @Test
    public void testHandleUpdateClubBudgetResponse_WithCompensation() {
        // Given
        String sagaId = "test-saga-id";
        TransferSaga saga = TransferSaga.builder()
                .sagaId(sagaId)
                .playerId(1L)
                .fromClubId(2L)
                .toClubId(3L)
                .transferFee(new BigDecimal("1000000"))
                .currentState(TransferSagaState.PLAYER_CLUB_UPDATED)
                .build();
        
        UpdateClubBudgetResponse response = UpdateClubBudgetResponse.builder()
                .sagaId(sagaId)
                .clubId(2L)
                .updated(false)
                .errorMessage("Errore nell'aggiornamento del budget")
                .build();
        
        when(transferSagaRepository.findBySagaId(sagaId)).thenReturn(Optional.of(saga));
        when(transferSagaRepository.save(any(TransferSaga.class))).thenReturn(saga);
        when(streamBridge.send(eq("updatePlayerClubRequest-out-0"), any(UpdatePlayerClubRequest.class))).thenReturn(true);
        
        // When
        transferSagaManager.handleUpdateClubBudgetResponse(response);
        
        // Then
        verify(transferSagaRepository, times(1)).findBySagaId(sagaId);
        verify(transferSagaRepository, times(1)).save(any(TransferSaga.class));
        verify(streamBridge, times(1)).send(eq("updatePlayerClubRequest-out-0"), any(UpdatePlayerClubRequest.class));
        assertEquals(TransferSagaState.FAILED, saga.getCurrentState());
    }
}
