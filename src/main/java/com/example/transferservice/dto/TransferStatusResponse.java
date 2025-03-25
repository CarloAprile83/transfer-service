package com.example.transferservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO per rappresentare la risposta con lo stato di un trasferimento
 */
@Data
@Builder
public class TransferStatusResponse {
    private String sagaId;
    private Long playerId;
    private Long fromClubId;
    private Long toClubId;
    private BigDecimal transferFee;
    private String state;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}