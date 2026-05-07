package com.batu.plaid_adapter_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.factory.WebhookFactory;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/plaid/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookFactory webhookFactory;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody PlaidWebhookDto payload) {

        webhookFactory.execute(payload);

        return ResponseEntity.ok().build();
    }
}
