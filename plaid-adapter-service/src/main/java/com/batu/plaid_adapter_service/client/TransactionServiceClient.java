package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.request.AccountIdsRequestDto;
import com.batu.shared.dto.request.TransactionsUpsertRequestDto;

@FeignClient(name = "transactions", url = "${transactionclient.url}", configuration = OpenFeignConfiguration.class)

public interface TransactionServiceClient {

    @PostMapping("/sync/batch-save")
    ResponseEntity<Void> saveTransactionsBatch(@RequestBody TransactionsUpsertRequestDto request);

    @PostMapping("/sync/deactivate-accounts")
    ResponseEntity<Void> deactivateByAccountIds(@RequestBody AccountIdsRequestDto request);

}
