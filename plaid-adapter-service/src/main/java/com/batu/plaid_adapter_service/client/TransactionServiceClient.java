package com.batu.plaid_adapter_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.batu.plaid_adapter_service.config.OpenFeignConfiguration;

@FeignClient(name = "transactions", url = "${transactionclient.url}", configuration = OpenFeignConfiguration.class)

public interface TransactionServiceClient {

    @GetMapping("/hello")
    String hello();

}
