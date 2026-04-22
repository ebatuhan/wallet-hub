package com.batu.dashboard_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

@FeignClient(name = "dashboardInsights", url = "${insightsclient.url}")
public interface InsightsClient {

    @GetMapping("/spendings")
    ResponseEntity<SpendingPerCategoryResponseDto> getSpendingByCategory(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("from") String from,
            @RequestParam("to") String to);

    @GetMapping("/income")
    ResponseEntity<IncomeSummaryResponseDto> getIncome(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("from") String from,
            @RequestParam("to") String to);

    @GetMapping("/spendings/{accountId}")
    ResponseEntity<SpendingPerCategoryByAccountResponseDto> getSpendingByCategoryByAccount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("accountId") UUID accountId,
            @RequestParam("from") String from,
            @RequestParam("to") String to);

    @GetMapping("/accounts/{accountId}/balance-history")
    ResponseEntity<List<AccountBalanceDataPointDto>> getAccountBalanceHistory(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("accountId") UUID accountId,
            @RequestParam("from") String from,
            @RequestParam("to") String to);
}
