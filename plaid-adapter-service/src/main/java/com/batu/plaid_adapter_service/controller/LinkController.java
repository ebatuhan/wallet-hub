package com.batu.plaid_adapter_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.service.LinkService;
import com.batu.shared.dto.ExchangeTokenRequestDto;
import com.batu.shared.dto.ExhcangetokenResponseDto;
import com.batu.shared.dto.LinkTokenRequestDto;
import com.batu.shared.dto.LinkTokenResponseDto;

@RestController
@RequestMapping("/api/plaid")
public class LinkController {
    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;

    }

    @PostMapping("/link")
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        return linkService.createLinkToken(request, jwt);
    }

    @PostMapping("/exchange")
    ExhcangetokenResponseDto exchangeToken(ExchangeTokenRequestDto request, @AuthenticationPrincipal Jwt jwt) {
        return linkService.exchangeToken(request, jwt);
    }

    @PostMapping("/mock")
    ExhcangetokenResponseDto mockToken(@AuthenticationPrincipal Jwt principal) {

        return linkService.mockToken(principal);
    }

}
