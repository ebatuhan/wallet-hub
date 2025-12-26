package com.batu.plaid_adapter_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello(@AuthenticationPrincipal Jwt user){
        return "Hello! " + user.getSubject();
    }
}
