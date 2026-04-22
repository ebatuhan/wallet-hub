package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.request.AccountRequestDto;

@FeignClient(name = "accounts", url = "${accountclient.url}", configuration = OpenFeignConfiguration.class)
public interface AccountServiceClient {

    @PostMapping("/internal")
    ResponseEntity<Void> create(@RequestBody AccountRequestDto request);

    @PutMapping("/internal/{accountId}")
    ResponseEntity<Void> update(@PathVariable java.util.UUID accountId, @RequestBody AccountRequestDto request);

    @PostMapping("/internal/{accountId}/deactivate")
    ResponseEntity<Void> deactivate(@PathVariable java.util.UUID accountId);

}
