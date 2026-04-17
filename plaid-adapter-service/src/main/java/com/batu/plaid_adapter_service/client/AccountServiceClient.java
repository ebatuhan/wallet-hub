package com.batu.plaid_adapter_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.request.AccountsUpsertRequestDto;

@FeignClient(name = "accounts", url = "${accountclient.url}", configuration = OpenFeignConfiguration.class)
public interface AccountServiceClient {

    @PostMapping("/sync/batch-save")
    ResponseEntity<Void> saveAccountsBatch(@RequestBody AccountsUpsertRequestDto request);

    @GetMapping("/sync/connections/{connectionId}/account-ids")
    ResponseEntity<List<UUID>> getAccountIdsByConnection(@PathVariable UUID connectionId);

    @PostMapping("/sync/connections/{connectionId}/deactivate")
    ResponseEntity<Void> deactivateAccountsByConnection(@PathVariable UUID connectionId);

}
