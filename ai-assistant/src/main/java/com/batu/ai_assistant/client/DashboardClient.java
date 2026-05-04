package com.batu.ai_assistant.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.ai_assistant.dto.client.AccountDashboardSummaryResponseDto;
import com.batu.ai_assistant.dto.client.UserDashboardSummaryResponseDto;
import com.batu.shared.dto.response.BudgetResponseDto;

@FeignClient(name = "assistantDashboard", url = "${dashboardclient.url}")
public interface DashboardClient {

    @GetMapping("/summary")
    ResponseEntity<UserDashboardSummaryResponseDto> getSummary(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to);

    @GetMapping("/budgets")
    ResponseEntity<List<BudgetResponseDto>> getBudgets();

    @GetMapping("/accounts/{accountId}/summary")
    ResponseEntity<AccountDashboardSummaryResponseDto> getAccountSummary(
            @PathVariable("accountId") UUID accountId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor);
}
