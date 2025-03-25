package com.example.transferservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_saga")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sagaId;
    private Long playerId;
    private Long fromClubId;
    private Long toClubId;
    private BigDecimal transferFee;
    
    @Enumerated(EnumType.STRING)
    private TransferSagaState currentState;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorMessage;
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TransferSagaState {
        STARTED,
        CLUB_BUDGET_CHECKED,
        PLAYER_AVAILABILITY_CHECKED,
        PLAYER_CLUB_UPDATED,
        CLUB_BUDGET_UPDATED,
        COMPLETED,
        FAILED
    }
}
