package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.shared.dto.request.TransactionRequestDto;

@FeignClient(name = "transactions", url = "${transactionclient.url}")
public interface TransactionServiceClient {

    @PostMapping
    ResponseEntity<Void> create(@RequestBody TransactionRequestDto request);

    @PutMapping("/{transactionId}")
    ResponseEntity<Void> update(@PathVariable("transactionId") java.util.UUID transactionId, @RequestBody TransactionRequestDto request);

    @DeleteMapping("/accounts/{accountId}")
    ResponseEntity<Void> deactivateByAccountId(@PathVariable("accountId") java.util.UUID accountId);
}
