package com.batu.dashboard_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.batu.shared.dto.response.BudgetResponseDto;

@FeignClient(name = "dashboardBudgeting", url = "${budgetingclient.url}")
public interface BudgetingClient {

    @GetMapping
    ResponseEntity<List<BudgetResponseDto>> getBudgets(@RequestHeader("Authorization") String authorization);
}
