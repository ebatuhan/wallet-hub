package com.batu.ai_assistant.unit.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.batu.ai_assistant.client.DashboardClient;
import com.batu.ai_assistant.dto.client.AccountDashboardSummaryResponseDto;
import com.batu.ai_assistant.dto.client.UserDashboardSummaryResponseDto;
import com.batu.ai_assistant.tools.DashboardTools;

@ExtendWith(MockitoExtension.class)
class DashboardToolsTest {

    private static final UUID USER_ID = UUID.fromString("93000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("93000000-0000-0000-0000-000000000002");

    @Mock
    private DashboardClient dashboardClient;

    @Test
    void getDashboardSummary_whenDatesProvided_shouldDelegateAndWrapBody() {
        UserDashboardSummaryResponseDto body = userSummary();
        when(dashboardClient.getSummary("2026-05-01", "2026-05-31")).thenReturn(ResponseEntity.ok(body));

        var response = new DashboardTools(dashboardClient).getDashboardSummary("2026-05-01", "2026-05-31");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Dashboard summary loaded.");
        assertThat(response.data()).isSameAs(body);
        verify(dashboardClient).getSummary("2026-05-01", "2026-05-31");
    }

    @Test
    void getDashboardSummary_whenDownstreamBodyIsNull_shouldReturnFailureInsteadOfSuccessfulNullData() {
        when(dashboardClient.getSummary(null, null)).thenReturn(ResponseEntity.ok(null));

        var response = new DashboardTools(dashboardClient).getDashboardSummary(null, null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Dashboard summary unavailable");
        assertThat(response.data()).isNull();
    }

    @Test
    void getAccountDashboardSummary_whenOptionalParamsProvided_shouldDelegateAllParamsAndWrapBody() {
        AccountDashboardSummaryResponseDto body = accountSummary();
        when(dashboardClient.getAccountSummary(ACCOUNT_ID, "2026-05-01", "2026-05-31", 20, "cursor-1"))
                .thenReturn(ResponseEntity.ok(body));

        var response = new DashboardTools(dashboardClient)
                .getAccountDashboardSummary(ACCOUNT_ID, "2026-05-01", "2026-05-31", 20, "cursor-1");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Account dashboard summary loaded.");
        assertThat(response.data()).isSameAs(body);
        verify(dashboardClient).getAccountSummary(ACCOUNT_ID, "2026-05-01", "2026-05-31", 20, "cursor-1");
    }

    @Test
    void getAccountDashboardSummary_whenDownstreamBodyIsNull_shouldReturnFailureInsteadOfSuccessfulNullData() {
        when(dashboardClient.getAccountSummary(ACCOUNT_ID, null, null, null, null)).thenReturn(ResponseEntity.ok(null));

        var response = new DashboardTools(dashboardClient)
                .getAccountDashboardSummary(ACCOUNT_ID, null, null, null, null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Account dashboard summary unavailable");
        assertThat(response.data()).isNull();
    }

    private static UserDashboardSummaryResponseDto userSummary() {
        return new UserDashboardSummaryResponseDto(
                USER_ID,
                new UserDashboardSummaryResponseDto.PeriodDto(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new UserDashboardSummaryResponseDto.AccountSummaryDto(1, List.of()),
                new UserDashboardSummaryResponseDto.IncomeSectionDto(List.of()),
                new UserDashboardSummaryResponseDto.RecentTransactionsDto(List.of(), false, null),
                new UserDashboardSummaryResponseDto.SpendingSectionDto(List.of(), null));
    }

    private static AccountDashboardSummaryResponseDto accountSummary() {
        return new AccountDashboardSummaryResponseDto(
                USER_ID,
                new AccountDashboardSummaryResponseDto.PeriodDto(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                new AccountDashboardSummaryResponseDto.AccountDto(
                        ACCOUNT_ID,
                        "Bank",
                        "Checking",
                        "DEPOSITORY",
                        "CHECKING",
                        "0000",
                        new BigDecimal("100.00"),
                        new BigDecimal("90.00"),
                        "USD"),
                List.of(),
                new AccountDashboardSummaryResponseDto.SpendingSectionDto(List.of(), null),
                new AccountDashboardSummaryResponseDto.RecentTransactionsDto(List.of(), false, null));
    }
}
