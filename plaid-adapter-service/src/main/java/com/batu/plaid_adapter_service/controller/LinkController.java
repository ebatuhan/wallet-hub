package com.batu.plaid_adapter_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.service.PlaidIntegrationService;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;
import com.batu.shared.dto.request.LinkTokenRequestDto;
import com.batu.shared.dto.response.ExchangeTokenResponseDto;
import com.batu.shared.dto.response.LinkTokenResponseDto;

@RestController
@RequestMapping("/api/plaid")
public class LinkController {
    private final PlaidIntegrationService linkService;

    public LinkController(PlaidIntegrationService linkService) {
        this.linkService = linkService;

    }

    @PostMapping("/link")
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        return linkService.createLinkToken(request, jwt);
    }

    @PostMapping("/exchange")
    ExchangeTokenResponseDto exchangeToken(ExchangeTokenRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        return linkService.exchangeToken(request, jwt);
    }

    @PostMapping("/mock")
    ExchangeTokenResponseDto mockToken(@AuthenticationPrincipal Jwt principal) {

        return linkService.mockToken(principal);
    }

}
