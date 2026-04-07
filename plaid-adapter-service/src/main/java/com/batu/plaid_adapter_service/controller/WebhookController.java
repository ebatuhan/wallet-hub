package com.batu.plaid_adapter_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.plaid_adapter_service.strategy.WebhookStrategy;
import com.batu.shared.dto.request.PlaidWebhookDto;

@RestController
@RequestMapping("/api/plaid/webhook")
public class WebhookController {

    private final WebhookStrategy webhookFactory;

    public WebhookController(WebhookStrategy webhookTypeFactory) {
        this.webhookFactory = webhookTypeFactory;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody PlaidWebhookDto payload) {

        webhookFactory.handle(payload);

        return ResponseEntity.ok().build();
    }
}
