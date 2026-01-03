package com.batu.plaid_adapter_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.factory.WebhookFactory;

@RestController
@RequestMapping("/api/plaid/webhook")
public class WebhookController {

    private final WebhookFactory webhookFactory;

    public WebhookController(WebhookFactory webhookFactory) {
        this.webhookFactory = webhookFactory;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody PlaidWebhookDto payload) {
        webhookFactory.executeHandling(payload);
        return ResponseEntity.ok().build();
    }
}
