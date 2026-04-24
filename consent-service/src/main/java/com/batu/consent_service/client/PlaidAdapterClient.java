package com.batu.consent_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

@FeignClient(name = "plaid-adapter", url = "${plaidadapterclient.url}")
public interface PlaidAdapterClient {

    @PostMapping("/plaid/link-token")
    ResponseEntity<LinkTokenResponseDto> createLinkToken(@RequestBody LinkTokenRequestDto request);

    @PostMapping("/plaid/exchange")
    ResponseEntity<ExchangeTokenResponseDto> exchangeToken(@RequestBody ExchangeTokenRequestDto request);

    @PostMapping("/plaid/mock")
    ResponseEntity<ExchangeTokenResponseDto> mockToken();

    @GetMapping("/plaid/connections")
    ResponseEntity<List<ConnectionResponseDto>> listConnections();

    @GetMapping("/plaid/connections/{connectionId}")
    ResponseEntity<ConnectionResponseDto> getConnection(@PathVariable("connectionId") UUID connectionId);

    @PatchMapping("/plaid/connections/{connectionId}")
    ResponseEntity<ConnectionResponseDto> updateConnection(
            @PathVariable("connectionId") UUID connectionId,
            @RequestBody ConnectionUpdateRequestDto request);

    @PostMapping("/plaid/connections/{connectionId}/refresh")
    ResponseEntity<Void> refreshConnection(@PathVariable("connectionId") UUID connectionId);

    @DeleteMapping("/plaid/connections/{connectionId}")
    ResponseEntity<Void> removeConnection(@PathVariable("connectionId") UUID connectionId);
}
