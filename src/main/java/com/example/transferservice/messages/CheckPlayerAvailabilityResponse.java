package com.example.transferservice.messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckPlayerAvailabilityResponse {
    private String sagaId;
    private Long playerId;
    private boolean playerAvailable;
    private String errorMessage;
}
