package com.batu.dashboard_service.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.shared.dto.response.AccountBalanceDataPointDto;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

@FeignClient(name = "dashboardInsights", url = "${insightsclient.url}")
public interface InsightsClient {

    @GetMapping("/spendings")
    ResponseEntity<SpendingPerCategoryResponseDto> getSpendingByCategory(
            @RequestParam("from") String from);

    @GetMapping("/spendings/graph")
    ResponseEntity<SpendingGraphResponseDto> getSpendingGraph(
            @RequestParam("from") String from);

    @GetMapping("/income")
    ResponseEntity<IncomeSummaryResponseDto> getIncome(
            @RequestParam("from") String from,
            @RequestParam("to") String to);

    @GetMapping("/spendings/{accountId}")
    ResponseEntity<SpendingPerCategoryByAccountResponseDto> getSpendingByCategoryByAccount(
            @PathVariable("accountId") UUID accountId,
            @RequestParam("from") String from);

    @GetMapping("/spendings/graph/{accountId}")
    ResponseEntity<SpendingGraphResponseDto> getSpendingGraphByAccount(
            @PathVariable("accountId") UUID accountId,
            @RequestParam("from") String from);

    @GetMapping("/accounts/{accountId}/balance-history")
    ResponseEntity<List<AccountBalanceDataPointDto>> getAccountBalanceHistory(
            @PathVariable("accountId") UUID accountId,
            @RequestParam("from") String from,
            @RequestParam("to") String to);
}
