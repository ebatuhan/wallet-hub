package com.batu.plaid_adapter_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountUpsertResponseDto;

@FeignClient(name = "plaidAccountClient", url = "${accountclient.url}")
public interface AccountClient {
    @PostMapping("/internal/upsert")
    AccountUpsertResponseDto upsertAccount(@RequestBody AccountUpsertRequestDto request);

    @GetMapping("/internal/by-connection/{connectionId}")
    List<AccountResponseDto> findAccountsByConnectionId(@PathVariable("connectionId") UUID connectionId);

    @PutMapping("/internal/deactivate-by-connection/{connectionId}")
    List<AccountUpsertResponseDto> deactivateAccountsByConnection(
            @PathVariable("connectionId") UUID connectionId);
}
