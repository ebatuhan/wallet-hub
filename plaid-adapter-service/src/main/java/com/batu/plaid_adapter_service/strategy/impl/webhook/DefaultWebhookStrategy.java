package com.batu.plaid_adapter_service.strategy.impl.webhook;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.dto.PlaidWebhookDto;
import com.batu.plaid_adapter_service.strategy.WebhookStrategy;

@Component("DEFAULT")
public class DefaultWebhookStrategy implements WebhookStrategy{

    @Override
    public void handle(PlaidWebhookDto dto) {} //Do nothing...

}
