package com.example.transferservice.messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckClubBudgetRequest {
    private String sagaId;
    private Long clubId;
    private BigDecimal transferFee;
}
