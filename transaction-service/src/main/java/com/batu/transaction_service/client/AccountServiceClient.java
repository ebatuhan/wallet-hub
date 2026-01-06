package com.batu.transaction_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.shared.dto.AccountNameRequestDto;
import com.batu.shared.dto.AccountNameResponseDto;
import com.batu.transaction_service.config.ClientCredentialsFeignConfiguration;

@FeignClient(name = "accounts", url = "${accountclient.url}", fallback = AccountServiceClientFallback.class, configuration = ClientCredentialsFeignConfiguration.class)
public interface AccountServiceClient {
    @PostMapping("/batch")
    ResponseEntity<List<AccountNameResponseDto>> getAccountNames(
            @RequestBody AccountNameRequestDto request);

}
