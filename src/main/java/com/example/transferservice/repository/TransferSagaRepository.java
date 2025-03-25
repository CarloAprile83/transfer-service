package com.example.transferservice.repository;

import com.example.transferservice.entity.TransferSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferSagaRepository extends JpaRepository<TransferSaga, Long> {
    Optional<TransferSaga> findBySagaId(String sagaId);
}
