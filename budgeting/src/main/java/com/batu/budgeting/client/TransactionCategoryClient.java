package com.batu.budgeting.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.batu.budgeting.config.ClientCredentialsFeignConfiguration;
import com.batu.shared.dto.request.PrimaryCategoryIdsRequestDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

@FeignClient(name = "transactionCategories", url = "${transactionclient.url}", configuration = ClientCredentialsFeignConfiguration.class)
public interface TransactionCategoryClient {

    @GetMapping("/{id}")
    ResponseEntity<TransactionPrimaryCategoryDto> getPrimaryCategoryById(@PathVariable UUID id);

    @PostMapping("/by-ids")
    ResponseEntity<List<TransactionPrimaryCategoryDto>> getPrimaryCategoriesByIds(
            @RequestBody PrimaryCategoryIdsRequestDto request);
}
