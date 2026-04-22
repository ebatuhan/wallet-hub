package com.batu.consent_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.consent_service.service.ConsentService;
import com.batu.shared.dto.request.ConnectionExchangeRequestDto;
import com.batu.shared.dto.request.ConnectionLinkTokenRequestDto;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
public class ConnectionController {

    private final ConsentService consentService;

    @PostMapping("/link-token")
    public ResponseEntity<LinkTokenResponseDto> createLinkToken(@RequestBody ConnectionLinkTokenRequestDto request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(consentService.createLinkToken(request, principal));
    }

    @PostMapping("/exchange")
    public ResponseEntity<ConnectionResponseDto> exchangeConnection(@RequestBody ConnectionExchangeRequestDto request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(consentService.exchangeConnection(request, principal));
    }

    @PostMapping("/mock")
    public ResponseEntity<ConnectionResponseDto> mockConnection(@RequestParam String provider,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(consentService.mockConnection(provider, principal));
    }

    @GetMapping
    public ResponseEntity<List<ConnectionResponseDto>> listConnections(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(consentService.listConnections(principal));
    }

    @GetMapping("/{connectionId}")
    public ResponseEntity<ConnectionResponseDto> getConnection(@PathVariable UUID connectionId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(consentService.getConnection(connectionId, principal));
    }

    @PatchMapping("/{connectionId}")
    public ResponseEntity<ConnectionResponseDto> updateConnection(@PathVariable UUID connectionId,
            @RequestBody ConnectionUpdateRequestDto request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(consentService.updateConnection(connectionId, request, principal));
    }

    @PostMapping("/{connectionId}/refresh")
    public ResponseEntity<Void> refreshConnection(@PathVariable UUID connectionId,
            @AuthenticationPrincipal Jwt principal) {
        consentService.refreshConnection(connectionId, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> removeConnection(@PathVariable UUID connectionId,
            @AuthenticationPrincipal Jwt principal) {
        consentService.removeConnection(connectionId, principal);
        return ResponseEntity.noContent().build();
    }
}
