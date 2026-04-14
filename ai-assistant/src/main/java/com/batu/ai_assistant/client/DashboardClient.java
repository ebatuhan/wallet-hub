package com.batu.ai_assistant.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.ai_assistant.dto.client.AccountDashboardSummaryResponseDto;
import com.batu.ai_assistant.dto.client.UserDashboardSummaryResponseDto;

@FeignClient(name = "assistantDashboard", url = "${dashboardclient.url}")
public interface DashboardClient {

    @GetMapping("/summary")
    ResponseEntity<UserDashboardSummaryResponseDto> getSummary(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to);

    @GetMapping("/accounts/{accountId}/summary")
    ResponseEntity<AccountDashboardSummaryResponseDto> getAccountSummary(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("accountId") UUID accountId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to);
}
