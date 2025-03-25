package com.example.transferservice.messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckClubBudgetResponse {
    private String sagaId;
    private Long clubId;
    private boolean budgetAvailable;
    private String errorMessage;
}
