package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.AccountsUpsertRequestDto;
import com.batu.shared.dto.AccountsUpsertResponseDto;

@FeignClient(name = "accounts", url = "${accountclient.url}", configuration = OpenFeignConfiguration.class)
public interface AccountServiceClient {

    @PostMapping("/batch-upsert")
    ResponseEntity<AccountsUpsertResponseDto> upsertAccountsBatch(AccountsUpsertRequestDto request);

}
