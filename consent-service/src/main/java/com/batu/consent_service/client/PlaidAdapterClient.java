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
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.consent_service.config.OpenFeignConfiguration;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

@FeignClient(name = "plaid-adapter", url = "${plaidadapterclient.url}", configuration = OpenFeignConfiguration.class)
public interface PlaidAdapterClient {

    @PostMapping("/internal/plaid/link-token")
    ResponseEntity<LinkTokenResponseDto> createLinkToken(@RequestParam UUID userId, @RequestBody LinkTokenRequestDto request);

    @PostMapping("/internal/plaid/exchange")
    ResponseEntity<ExchangeTokenResponseDto> exchangeToken(@RequestParam UUID userId,
            @RequestBody ExchangeTokenRequestDto request);

    @PostMapping("/internal/plaid/mock")
    ResponseEntity<ExchangeTokenResponseDto> mockToken(@RequestParam UUID userId);

    @GetMapping("/internal/plaid/connections")
    ResponseEntity<List<ConnectionResponseDto>> listConnections(@RequestParam UUID userId);

    @GetMapping("/internal/plaid/connections/{connectionId}")
    ResponseEntity<ConnectionResponseDto> getConnection(@RequestParam UUID userId, @PathVariable UUID connectionId);

    @PatchMapping("/internal/plaid/connections/{connectionId}")
    ResponseEntity<ConnectionResponseDto> updateConnection(@RequestParam UUID userId,
            @PathVariable UUID connectionId,
            @RequestBody ConnectionUpdateRequestDto request);

    @PostMapping("/internal/plaid/connections/{connectionId}/refresh")
    ResponseEntity<Void> refreshConnection(@RequestParam UUID userId, @PathVariable UUID connectionId);

    @DeleteMapping("/internal/plaid/connections/{connectionId}")
    ResponseEntity<Void> removeConnection(@RequestParam UUID userId, @PathVariable UUID connectionId);
}
