package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.AccountsUpsertRequestDto;
import com.batu.shared.dto.AccountsUpsertResponseDto;
import com.batu.shared.dto.TransactionsUpsertRequestDto;

@FeignClient(name = "transactions", url = "${transactionclient.url}", configuration = OpenFeignConfiguration.class)

public interface TransactionServiceClient {

    @PostMapping("/batch-upsert")
    ResponseEntity<Boolean> upsertTransactionsBatch(TransactionsUpsertRequestDto request);

}
