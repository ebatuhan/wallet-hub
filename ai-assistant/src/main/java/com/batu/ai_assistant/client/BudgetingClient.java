package com.batu.ai_assistant.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.batu.ai_assistant.dto.client.CreateBudgetRequestDto;
import com.batu.shared.dto.response.BudgetResponseDto;

@FeignClient(name = "assistantBudgeting", url = "${budgetingclient.url}")
public interface BudgetingClient {

    @PostMapping
    ResponseEntity<BudgetResponseDto> createBudget(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CreateBudgetRequestDto request);

    @PutMapping("/{budgetId}")
    ResponseEntity<BudgetResponseDto> updateBudget(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("budgetId") UUID budgetId,
            @RequestBody CreateBudgetRequestDto request);

    @DeleteMapping("/{budgetId}")
    ResponseEntity<Void> deactivateBudget(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("budgetId") UUID budgetId);
}
