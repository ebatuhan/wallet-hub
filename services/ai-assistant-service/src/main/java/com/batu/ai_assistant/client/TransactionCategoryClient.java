package com.batu.ai_assistant.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

@FeignClient(name = "assistantTransactionCategories", url = "${transactionclient.url}")
public interface TransactionCategoryClient {

    @GetMapping
    ResponseEntity<List<TransactionPrimaryCategoryDto>> getAllPrimaryCategories();
}
