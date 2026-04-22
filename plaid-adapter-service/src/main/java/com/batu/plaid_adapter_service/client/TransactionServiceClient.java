package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.request.TransactionRequestDto;

@FeignClient(name = "transactions", url = "${transactionclient.url}", configuration = OpenFeignConfiguration.class)

public interface TransactionServiceClient {

    @PostMapping("/internal")
    ResponseEntity<Void> create(@RequestBody TransactionRequestDto request);

    @PutMapping("/internal/{transactionId}")
    ResponseEntity<Void> update(@PathVariable java.util.UUID transactionId, @RequestBody TransactionRequestDto request);

    @PostMapping("/internal/accounts/{accountId}/deactivate")
    ResponseEntity<Void> deactivateByAccountId(@PathVariable java.util.UUID accountId);

}
