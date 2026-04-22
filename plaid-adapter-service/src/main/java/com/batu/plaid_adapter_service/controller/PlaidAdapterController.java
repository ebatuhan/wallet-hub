package com.batu.plaid_adapter_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

@RestController
@RequestMapping("/internal/plaid")
@PreAuthorize("hasAuthority('ROLE_SERVICE')")
public class PlaidAdapterController {

    private final PlaidIntegrationService plaidIntegrationService;
    private final ConnectionService connectionService;
    private final ConnectionMapper connectionMapper;

    public PlaidAdapterController(PlaidIntegrationService plaidIntegrationService,
            ConnectionService connectionService,
            ConnectionMapper connectionMapper) {
        this.plaidIntegrationService = plaidIntegrationService;
        this.connectionService = connectionService;
        this.connectionMapper = connectionMapper;
    }

    @PostMapping("/link-token")
    public ResponseEntity<LinkTokenResponseDto> createLinkToken(@RequestParam UUID userId,
            @RequestBody(required = false) LinkTokenRequestDto request) {
        LinkTokenRequestDto safeRequest = request == null ? new LinkTokenRequestDto() : request;
        return ResponseEntity.ok(plaidIntegrationService.createLinkToken(safeRequest, userId));
    }

    @PostMapping("/exchange")
    public ResponseEntity<ExchangeTokenResponseDto> exchangeToken(@RequestParam UUID userId,
            @RequestBody ExchangeTokenRequestDto request) {
        return ResponseEntity.ok(plaidIntegrationService.exchangeLinkToken(request, userId));
    }

    @PostMapping("/mock")
    public ResponseEntity<ExchangeTokenResponseDto> mockToken(@RequestParam UUID userId) {
        return ResponseEntity.ok(plaidIntegrationService.mockToken(userId));
    }

    @GetMapping("/connections")
    public ResponseEntity<List<ConnectionResponseDto>> listConnections(@RequestParam UUID userId) {
        return ResponseEntity.ok(connectionService.readAllByUserId(userId).stream()
                .map(connectionMapper::toResponse)
                .toList());
    }

    @GetMapping("/connections/{connectionId}")
    public ResponseEntity<ConnectionResponseDto> getConnection(@RequestParam UUID userId,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(connectionMapper.toResponse(loadConnection(userId, connectionId)));
    }

    @PatchMapping("/connections/{connectionId}")
    public ResponseEntity<ConnectionResponseDto> updateConnection(@RequestParam UUID userId,
            @PathVariable UUID connectionId,
            @RequestBody ConnectionUpdateRequestDto request) {
        Connection connection = loadConnection(userId, connectionId);
        connectionMapper.updateConnectionFromRequest(request, connection);
        return ResponseEntity.ok(connectionMapper.toResponse(connectionService.updateById(connectionId, connection)));
    }

    @PostMapping("/connections/{connectionId}/refresh")
    public ResponseEntity<Void> refreshConnection(@RequestParam UUID userId,
            @PathVariable UUID connectionId) {
        loadConnection(userId, connectionId);
        plaidIntegrationService.syncConnection(connectionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/connections/{connectionId}")
    public ResponseEntity<Void> removeConnection(@RequestParam UUID userId,
            @PathVariable UUID connectionId) {
        loadConnection(userId, connectionId);
        plaidIntegrationService.removeConnection(connectionId, "USER_REQUESTED_REMOVAL");
        return ResponseEntity.noContent().build();
    }

    private Connection loadConnection(UUID userId, UUID connectionId) {
        return connectionService.readByIdAndUserId(connectionId, userId);
    }
}
