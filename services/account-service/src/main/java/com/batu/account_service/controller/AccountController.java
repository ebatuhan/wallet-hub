package com.batu.account_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "User account lookup, summaries, and paginated account views.")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account", description = "Returns one account owned by the authenticated user.")
    public ResponseEntity<AccountResponseDto> getAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getAccount(accountId, jwt));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get account summary", description = "Returns balance totals and account summary data for the authenticated user.")
    public ResponseEntity<AccountSummaryResponseDto> getAccountSummary(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getAccountSummary(jwt));
    }

    @PostMapping("/batch")
    @Operation(summary = "Resolve account names", description = "Returns account names for the supplied account IDs.")
    public ResponseEntity<List<AccountNameResponseDto>> getAccountsByGivenIds(@Valid @RequestBody AccountNameRequestDto request) {
        return ResponseEntity.ok(accountService.getAccountsByGivenIds(request));
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Returns a cursor-paginated account view with optional filters and sorting.")
    public ResponseEntity<CursorResponse<AccountViewDto>> getAllAccounts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String accountSubtype,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") int limit,
            @RequestParam(required = false) AccountSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        CursorResponse<AccountViewDto> response = accountService.getAccountsViewPaginated(
                jwt,
                accountName,
                institutionName,
                accountType,
                accountSubtype,
                cursor,
                limit,
                sortBy,
                direction);

        return ResponseEntity.ok(response);
    }
}
