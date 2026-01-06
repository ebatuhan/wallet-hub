package com.batu.plaid_adapter_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.client.AccountServiceClient;
import com.batu.plaid_adapter_service.client.TransactionServiceClient;
import com.batu.plaid_adapter_service.service.LinkService;
import com.batu.shared.dto.ExchangeTokenRequestDto;
import com.batu.shared.dto.ExhcangetokenResponseDto;
import com.batu.shared.dto.LinkTokenRequestDto;
import com.batu.shared.dto.LinkTokenResponseDto;

@RestController
@RequestMapping("/api/plaid")
public class LinkController {
    private final LinkService linkService;
    
    private final AccountServiceClient c1;
    private final TransactionServiceClient c2;

    public LinkController(LinkService linkService, AccountServiceClient c1, TransactionServiceClient c2) {
        this.linkService = linkService;
        this.c1 = c1;
        this.c2 = c2;
    }

    @PostMapping("/link")
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto request, @AuthenticationPrincipal Jwt jwt){
        return linkService.createLinkToken(request, jwt);
    }

    @PostMapping("/exchange")
    ExhcangetokenResponseDto exchangeToken(ExchangeTokenRequestDto request, @AuthenticationPrincipal Jwt jwt){
        return linkService.exchangeToken(request, jwt);
    }


}
