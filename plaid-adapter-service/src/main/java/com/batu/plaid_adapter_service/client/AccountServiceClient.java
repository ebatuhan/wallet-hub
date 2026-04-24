package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.shared.dto.request.AccountRequestDto;

@FeignClient(name = "accounts", url = "${accountclient.url}")
public interface AccountServiceClient {

    @PostMapping
    ResponseEntity<Void> create(@RequestBody AccountRequestDto request);

    @PutMapping("/{accountId}")
    ResponseEntity<Void> update(@PathVariable("accountId") java.util.UUID accountId, @RequestBody AccountRequestDto request);

    @DeleteMapping("/{accountId}")
    ResponseEntity<Void> deactivate(@PathVariable("accountId") java.util.UUID accountId);
}
