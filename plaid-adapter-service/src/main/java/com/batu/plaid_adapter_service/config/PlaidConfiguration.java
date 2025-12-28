package com.batu.plaid_adapter_service.config;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;


@Configuration
public class PlaidConfiguration {
    @Value("${plaid.api}")
    private String clientId;

    @Value("${plaid.secret}")
    private String secret;

    @Value("${plaid.env}")
    private String environment;

    @Bean
    public PlaidApi plaidClient() {
        HashMap<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);

        ApiClient apiClient = new ApiClient(apiKeys);

        switch (environment.toLowerCase()) {
            case "production":
                apiClient.setPlaidAdapter(ApiClient.Production);
                break;
            case "sandbox":
            default:
                apiClient.setPlaidAdapter(ApiClient.Sandbox);
                break;
        }

        return apiClient.createService(PlaidApi.class);
    }

}
