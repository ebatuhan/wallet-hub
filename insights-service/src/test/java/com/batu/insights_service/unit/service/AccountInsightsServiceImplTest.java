package com.batu.insights_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.repository.AccountInsightRepository;
import com.batu.insights_service.service.impl.AccountInsightsServiceImpl;
import com.batu.shared.messaging.event.AccountRemoved;

@ExtendWith(MockitoExtension.class)
class AccountInsightsServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("85000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("85000000-0000-0000-0000-000000000002");

    @Mock
    private AccountInsightRepository accountInsightRepository;

    @InjectMocks
    private AccountInsightsServiceImpl service;

    @Test
    void save_whenRowProvided_shouldDelegateWithoutMutation() {
        AccountBalanceDataPointRow row = row("2026-04-17", "125.50");

        service.save(row);

        verify(accountInsightRepository).save(row);
    }

    @Test
    void remove_whenAccountRemovedEventProvided_shouldDeleteByAccountAndUser() {
        AccountRemoved event = new AccountRemoved(ACCOUNT_ID, USER_ID, UUID.fromString("85000000-0000-0000-0000-000000000003"));

        service.remove(event);

        verify(accountInsightRepository).deleteByAccount(ACCOUNT_ID, USER_ID);
    }

    @Test
    void getAccountBalanceHistory_whenRowsExist_shouldMapRowsInRepositoryOrder() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        when(accountInsightRepository.getAccountBalanceHistory(ACCOUNT_ID, USER_ID, from, to))
                .thenReturn(List.of(row("2026-04-01", "100.00"), row("2026-04-02", "110.00")));

        var result = service.getAccountBalanceHistory(ACCOUNT_ID, from, to, jwt());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(point -> point.date())
                .containsExactly(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2));
        assertThat(result.get(0).accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.get(0).userId()).isEqualTo(USER_ID);
        assertThat(result.get(0).balance()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).isoCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void getAccountBalanceHistory_whenRepositoryReturnsEmptyRows_shouldReturnEmptyList() {
        LocalDate from = LocalDate.of(2026, 4, 1);
        LocalDate to = LocalDate.of(2026, 4, 30);
        when(accountInsightRepository.getAccountBalanceHistory(ACCOUNT_ID, USER_ID, from, to)).thenReturn(List.of());

        var result = service.getAccountBalanceHistory(ACCOUNT_ID, from, to, USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getAccountBalanceHistory_whenJwtSubjectMalformed_shouldThrowAndNotQueryRepository() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("bad-subject").build();

        assertThatThrownBy(() -> service.getAccountBalanceHistory(
                ACCOUNT_ID,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                jwt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static AccountBalanceDataPointRow row(String date, String balance) {
        return new AccountBalanceDataPointRow(ACCOUNT_ID, USER_ID, new BigDecimal(balance), "USD", LocalDate.parse(date));
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token").header("alg", "none").subject(USER_ID.toString()).build();
    }
}
