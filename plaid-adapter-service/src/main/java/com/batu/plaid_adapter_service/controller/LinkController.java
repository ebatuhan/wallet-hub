package com.batu.plaid_adapter_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.dto.LinkTokenRequestDto;
import com.batu.plaid_adapter_service.dto.LinkTokenResponseDto;
import com.batu.plaid_adapter_service.service.LinkService;

@RestController
@RequestMapping("/api/plaid")
public class LinkController {
    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping
    LinkTokenResponseDto createLinkToken(LinkTokenRequestDto request, @AuthenticationPrincipal Jwt jwt){
        return linkService.createLinkToken(request, jwt);
    }
}
