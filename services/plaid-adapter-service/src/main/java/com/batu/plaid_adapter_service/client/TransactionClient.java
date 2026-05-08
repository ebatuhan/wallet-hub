package com.batu.plaid_adapter_service.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.TransactionUpsertResponseDto;

@FeignClient(name = "plaidTransactionClient", url = "${transactionclient.url}")
public interface TransactionClient {
    @PostMapping("/internal/upsert")
    TransactionUpsertResponseDto upsertTransaction(@RequestBody TransactionUpsertRequestDto request);
}
