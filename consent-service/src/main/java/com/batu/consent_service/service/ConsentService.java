package com.batu.consent_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.shared.dto.request.ConnectionExchangeRequestDto;
import com.batu.shared.dto.request.ConnectionLinkTokenRequestDto;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

public interface ConsentService {

    LinkTokenResponseDto createLinkToken(ConnectionLinkTokenRequestDto request, Jwt principal);

    ConnectionResponseDto exchangeConnection(ConnectionExchangeRequestDto request, Jwt principal);

    ConnectionResponseDto mockConnection(String provider, Jwt principal);

    List<ConnectionResponseDto> listConnections(Jwt principal);

    ConnectionResponseDto getConnection(UUID connectionId, Jwt principal);

    ConnectionResponseDto updateConnection(UUID connectionId, ConnectionUpdateRequestDto request, Jwt principal);

    void refreshConnection(UUID connectionId, Jwt principal);

    void removeConnection(UUID connectionId, Jwt principal);
}
