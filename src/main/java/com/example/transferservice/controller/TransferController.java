package com.example.transferservice.controller;

import com.example.transferservice.dto.TransferStatusResponse;
import com.example.transferservice.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> initiateTransfer(@RequestBody TransferRequest request) {
        log.info("Ricevuta richiesta di trasferimento: {}", request);
        String sagaId = transferService.initiateTransfer(
                request.getPlayerId(),
                request.getFromClubId(),
                request.getToClubId(),
                request.getTransferFee()
        );
        return ResponseEntity.accepted().body(new TransferResponse(sagaId, "Trasferimento avviato"));
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<TransferStatusResponse> getTransferStatus(@PathVariable String sagaId) {
        log.info("Ricevuta richiesta di stato per il trasferimento: {}", sagaId);
        TransferStatusResponse status = transferService.getTransferStatus(sagaId);
        return ResponseEntity.ok(status);
    }   
}
