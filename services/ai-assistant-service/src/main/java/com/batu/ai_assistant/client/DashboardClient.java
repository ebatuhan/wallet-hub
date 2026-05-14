package com.batu.ai_assistant.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.ai_assistant.dto.client.AccountDashboardSummaryResponseDto;
import com.batu.ai_assistant.dto.client.UserDashboardSummaryResponseDto;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;

@FeignClient(name = "assistantDashboard", url = "${dashboardclient.url}")
public interface DashboardClient {

    @GetMapping("/summary")
    ResponseEntity<UserDashboardSummaryResponseDto> getSummary(
            @RequestParam(value = "from", required = false) String from);

    @GetMapping("/budgets")
    ResponseEntity<CursorResponse<BudgetResponseDto>> getBudgets(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "direction", required = false) String direction);

    @GetMapping("/accounts/{accountId}/summary")
    ResponseEntity<AccountDashboardSummaryResponseDto> getAccountSummary(
            @PathVariable("accountId") UUID accountId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor);
}
