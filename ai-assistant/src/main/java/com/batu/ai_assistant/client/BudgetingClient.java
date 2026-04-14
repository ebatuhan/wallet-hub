package com.batu.ai_assistant.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.batu.ai_assistant.dto.client.BudgetResponseDto;
import com.batu.ai_assistant.dto.client.CreateBudgetRequestDto;

@FeignClient(name = "assistantBudgeting", url = "${budgetingclient.url}")
public interface BudgetingClient {

    @GetMapping
    ResponseEntity<List<BudgetResponseDto>> getBudgets(@RequestHeader("Authorization") String authorization);

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
