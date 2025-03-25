package com.example.transferservice.service;

import com.example.transferservice.entity.TransferSaga;
import com.example.transferservice.repository.TransferSagaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private TransferSagaRepository transferSagaRepository;

    @Mock
    private TransferSagaManager transferSagaManager;

    @InjectMocks
    private TransferService transferService;

    @Test
    public void testInitiateTransfer() {
        // Given
        Long playerId = 1L;
        Long fromClubId = 2L;
        Long toClubId = 3L;
        BigDecimal transferFee = new BigDecimal("1000000");
        
        when(transferSagaRepository.save(any(TransferSaga.class))).thenAnswer(invocation -> {
            TransferSaga saga = invocation.getArgument(0);
            return saga;
        });
        
        doNothing().when(transferSagaManager).startTransferSaga(any(TransferSaga.class));
        
        // When
        String sagaId = transferService.initiateTransfer(playerId, fromClubId, toClubId, transferFee);
        
        // Then
        verify(transferSagaRepository, times(1)).save(any(TransferSaga.class));
        verify(transferSagaManager, times(1)).startTransferSaga(any(TransferSaga.class));
        
        // Verifica che l'ID del saga non sia nullo
        assertEquals(36, sagaId.length()); // UUID standard length
    }
}
