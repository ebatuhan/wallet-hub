package com.batu.dashboard_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

@FeignClient(name = "dashboardAccounts", url = "${accountclient.url}")
public interface AccountClient {

    @GetMapping("/summary")
    ResponseEntity<AccountSummaryResponseDto> getAccountSummary(@RequestHeader("Authorization") String authorization);

    @GetMapping("/{accountId}")
    ResponseEntity<AccountResponseDto> getAccount(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("accountId") java.util.UUID accountId);

    @GetMapping
    ResponseEntity<CursorResponse<AccountViewDto>> getAccounts(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "direction", required = false) String direction);
}
