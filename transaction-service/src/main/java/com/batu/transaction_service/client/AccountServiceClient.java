package com.batu.transaction_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.transaction_service.config.ClientCredentialsFeignConfiguration;
import com.batu.transaction_service.dto.AccountInformationRequestDto;
import com.batu.transaction_service.dto.AccountInformationResponseDto;

@FeignClient(name = "accounts", url = "${accountclient.url}",fallback = AccountServiceClientFallback.class,  configuration = ClientCredentialsFeignConfiguration.class)
public interface AccountServiceClient {
    @PostMapping("/batch")
    ResponseEntity<List<AccountInformationResponseDto>> getAccountInformations(
            @RequestBody AccountInformationRequestDto request);

}
