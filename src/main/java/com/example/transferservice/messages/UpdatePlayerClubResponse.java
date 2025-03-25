package com.example.transferservice.messages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlayerClubResponse {
    private String sagaId;
    private Long playerId;
    private boolean updated;
    private String errorMessage;
}
