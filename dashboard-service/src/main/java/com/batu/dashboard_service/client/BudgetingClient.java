package com.batu.dashboard_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;

@FeignClient(name = "dashboardBudgeting", url = "${budgetingclient.url}")
public interface BudgetingClient {

    @GetMapping
    ResponseEntity<CursorResponse<BudgetResponseDto>> getBudgets(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "direction", required = false) String direction);
}
