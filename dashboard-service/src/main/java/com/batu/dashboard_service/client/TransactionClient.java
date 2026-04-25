package com.batu.dashboard_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.shared.dto.request.PrimaryCategoryIdsRequestDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

@FeignClient(name = "dashboardTransactions", url = "${transactionclient.url}")
public interface TransactionClient {

    @GetMapping
    ResponseEntity<CursorResponse<TransactionViewResponseDto>> getTransactions(
            @RequestParam(value = "accountId", required = false) UUID accountId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor);

    @PostMapping("/categories/primary/by-ids")
    ResponseEntity<List<TransactionPrimaryCategoryDto>> getPrimaryCategoriesByIds(
            @RequestBody PrimaryCategoryIdsRequestDto request);

    @GetMapping("/categories/primary/{id}")
    ResponseEntity<TransactionPrimaryCategoryDto> getPrimaryCategoryById(
            @PathVariable("id") UUID id);
}
