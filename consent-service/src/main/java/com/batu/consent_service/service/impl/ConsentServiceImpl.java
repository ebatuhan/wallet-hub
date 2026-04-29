package com.batu.consent_service.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import feign.FeignException;

import com.batu.consent_service.entity.ConsentConnection;
import com.batu.consent_service.exception.ResourceNotFoundException;
import com.batu.consent_service.mapper.ConsentConnectionMapper;
import com.batu.consent_service.provider.ProviderConnectionGateway;
import com.batu.consent_service.repository.ConsentConnectionRepository;
import com.batu.consent_service.service.ConsentService;
import com.batu.shared.dto.request.ConnectionExchangeRequestDto;
import com.batu.shared.dto.request.ConnectionLinkTokenRequestDto;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

@Service
public class ConsentServiceImpl implements ConsentService {

    private final Map<String, ProviderConnectionGateway> gateways;
    private final ConsentConnectionRepository consentConnectionRepository;
    private final ConsentConnectionMapper consentConnectionMapper;

    public ConsentServiceImpl(List<ProviderConnectionGateway> gateways,
            ConsentConnectionRepository consentConnectionRepository,
            ConsentConnectionMapper consentConnectionMapper) {
        this.gateways = gateways.stream().collect(Collectors.toMap(ProviderConnectionGateway::provider, Function.identity()));
        this.consentConnectionRepository = consentConnectionRepository;
        this.consentConnectionMapper = consentConnectionMapper;
    }

    @Override
    public LinkTokenResponseDto createLinkToken(ConnectionLinkTokenRequestDto request, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        return resolveGateway(request.getProvider()).createLinkToken(userId, request.getCountry());
    }

    @Override
    @Transactional
    public ConnectionResponseDto exchangeConnection(ConnectionExchangeRequestDto request, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        ExchangeTokenResponseDto response = resolveGateway(request.getProvider()).exchangeConnection(userId, request);

        ConsentConnection connection = consentConnectionRepository
                .findByProviderAndProviderConnectionId(request.getProvider(), response.getConnectionId())
                .orElseGet(() -> new ConsentConnection(
                        userId,
                        request.getProvider(),
                        response.getConnectionId(),
                        response.getInstitutionId(),
                        response.getInstitutionName(),
                        response.getInstitutionName(),
                        "ACTIVE",
                        null));

        connection.setUserId(userId);
        connection.setProvider(request.getProvider());
        connection.setProviderConnectionId(response.getConnectionId());
        connection.setInstitutionId(response.getInstitutionId());
        connection.setInstitutionName(response.getInstitutionName());
        if (connection.getDisplayName() == null || connection.getDisplayName().isBlank()) {
            connection.setDisplayName(response.getInstitutionName());
        }
        connection.setStatus("ACTIVE");

        return consentConnectionMapper.toResponse(consentConnectionRepository.save(connection));
    }

    @Override
    @Transactional
    public ConnectionResponseDto mockConnection(String provider, Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        ExchangeTokenResponseDto response = resolveGateway(provider).mockConnection(userId);

        ConsentConnection connection = consentConnectionRepository
                .findByProviderAndProviderConnectionId(provider, response.getConnectionId())
                .orElseGet(() -> new ConsentConnection(
                        userId,
                        provider,
                        response.getConnectionId(),
                        response.getInstitutionId(),
                        response.getInstitutionName(),
                        response.getInstitutionName(),
                        "ACTIVE",
                        null));

        connection.setUserId(userId);
        connection.setProvider(provider);
        connection.setProviderConnectionId(response.getConnectionId());
        connection.setInstitutionId(response.getInstitutionId());
        connection.setInstitutionName(response.getInstitutionName());
        if (connection.getDisplayName() == null || connection.getDisplayName().isBlank()) {
            connection.setDisplayName(response.getInstitutionName());
        }
        connection.setStatus("ACTIVE");

        return consentConnectionMapper.toResponse(consentConnectionRepository.save(connection));
    }

    @Override
    public List<ConnectionResponseDto> listConnections(Jwt principal) {
        UUID userId = UUID.fromString(principal.getSubject());
        return consentConnectionRepository.findByUserId(userId).stream()
                .map(consentConnectionMapper::toResponse)
                .toList();
    }

    @Override
    public ConnectionResponseDto getConnection(UUID connectionId, Jwt principal) {
        return consentConnectionMapper.toResponse(loadConnection(connectionId, principal));
    }

    @Override
    @Transactional
    public ConnectionResponseDto updateConnection(UUID connectionId, ConnectionUpdateRequestDto request, Jwt principal) {
        ConsentConnection connection = loadConnection(connectionId, principal);
        ConnectionResponseDto providerResponse = resolveGateway(connection.getProvider())
                .updateConnection(UUID.fromString(principal.getSubject()), connection.getProviderConnectionId(), request);
        consentConnectionMapper.updateFromRequest(request, connection);
        consentConnectionMapper.updateFromProvider(providerResponse, connection);
        return consentConnectionMapper.toResponse(consentConnectionRepository.save(connection));
    }

    @Override
    @Transactional
    public void refreshConnection(UUID connectionId, Jwt principal) {
        ConsentConnection connection = loadConnection(connectionId, principal);
        resolveGateway(connection.getProvider())
                .refreshConnection(UUID.fromString(principal.getSubject()), connection.getProviderConnectionId());
        connection.setStatus("SYNCING");
        consentConnectionRepository.save(connection);
    }

    @Override
    @Transactional
    public void removeConnection(UUID connectionId, Jwt principal) {
        ConsentConnection connection = loadConnection(connectionId, principal);
        connection.setStatus("REMOVING");
        consentConnectionRepository.save(connection);

        try {
            resolveGateway(connection.getProvider())
                    .removeConnection(UUID.fromString(principal.getSubject()), connection.getProviderConnectionId());
        } catch (FeignException.NotFound ignored) {
            // Provider data can be wiped independently in local/dev environments.
        }

        consentConnectionRepository.delete(connection);
    }

    private ProviderConnectionGateway resolveGateway(String provider) {
        ProviderConnectionGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        return gateway;
    }

    private ConsentConnection loadConnection(UUID connectionId, Jwt principal) {
        return consentConnectionRepository.findByConnectionIdAndUserId(connectionId, UUID.fromString(principal.getSubject()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Connection with id " + connectionId + " not found"));
    }
}
