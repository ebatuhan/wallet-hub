package com.batu.plaid_adapter_service.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.plaid_adapter_service.dto.LinkTokenRequestDto;
import com.batu.plaid_adapter_service.dto.LinkTokenResponseDto;

public interface LinkService {
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto linkTokenRequestDto, Jwt principal);

}
