package com.batu.plaid_adapter_service.controller;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/plaid")
@Tag(name = "Plaid Adapter", description = "Plaid link-token, exchange, and connection management endpoints.")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "Create link token", description = "Creates a Plaid Link token for the authenticated user.")
    public ResponseEntity<LinkTokenResponseDto> createLinkToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) LinkTokenRequestDto request) {
        LinkTokenRequestDto safeRequest = request == null ? new LinkTokenRequestDto("US") : request;
        return ResponseEntity.ok(plaidIntegrationService.createLinkToken(safeRequest, userId(jwt)));
    }

    @PostMapping("/exchange")
    @Operation(summary = "Exchange public token", description = "Exchanges a Plaid public token and records the resulting connection.")
    public ResponseEntity<ExchangeTokenResponseDto> exchangeToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ExchangeTokenRequestDto request) {
        return ResponseEntity.ok(plaidIntegrationService.exchangeLinkToken(request, userId(jwt)));
    }

    @PostMapping("/mock")
    @Operation(summary = "Create mock connection", description = "Creates a mock Plaid connection for local/testing flows.")
    public ResponseEntity<ExchangeTokenResponseDto> mockToken(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(plaidIntegrationService.mockToken(userId(jwt)));
    }

    @GetMapping("/connections")
    @Operation(summary = "List connections", description = "Returns all Plaid connections for the authenticated user.")
    public ResponseEntity<List<ConnectionResponseDto>> listConnections(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(connectionService.readAllByUserId(userId(jwt)).stream()
                .map(connectionMapper::toResponse)
                .toList());
    }

    @GetMapping("/connections/{connectionId}")
    @Operation(summary = "Get connection", description = "Returns one Plaid connection owned by the authenticated user.")
    public ResponseEntity<ConnectionResponseDto> getConnection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(connectionMapper.toResponse(loadConnection(userId(jwt), connectionId)));
    }

    @PatchMapping("/connections/{connectionId}")
    @Operation(summary = "Update connection", description = "Updates editable metadata for a Plaid connection.")
    public ResponseEntity<ConnectionResponseDto> updateConnection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID connectionId,
            @RequestBody ConnectionUpdateRequestDto request) {
        Connection connection = loadConnection(userId(jwt), connectionId);
        connectionMapper.updateConnectionFromRequest(request, connection);
        return ResponseEntity.ok(connectionMapper.toResponse(connectionService.updateById(connectionId, connection)));
    }

    @PostMapping("/connections/{connectionId}/refresh")
    @Operation(summary = "Refresh connection", description = "Triggers synchronization for an existing Plaid connection.")
    public ResponseEntity<Void> refreshConnection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID connectionId) {
        loadConnection(userId(jwt), connectionId);
        plaidIntegrationService.syncConnection(connectionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/connections/{connectionId}")
    @Operation(summary = "Remove connection", description = "Removes a Plaid connection and related data for the authenticated user.")
    public ResponseEntity<Void> removeConnection(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID connectionId) {
        loadConnection(userId(jwt), connectionId);
        plaidIntegrationService.removeConnection(connectionId, "USER_REQUESTED_REMOVAL");
        return ResponseEntity.noContent().build();
    }

    private Connection loadConnection(UUID userId, UUID connectionId) {
        return connectionService.readByIdAndUserId(connectionId, userId);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
