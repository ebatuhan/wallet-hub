package com.batu.account_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.batu.account_service.entity.Account;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.messaging.OutboxDomainEventPublisher;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.AccountRepository.AccountCurrencyTotalProjection;
import com.batu.account_service.service.impl.AccountServiceImpl;
import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("a3000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("a3000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("a3000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_USER_ID = UUID.fromString("a3000000-0000-0000-0000-000000000004");
    private static final UUID CONNECTION_ID = UUID.fromString("a3000000-0000-0000-0000-000000000005");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CursorUtils cursorUtils;

    @Mock
    private OutboxDomainEventPublisher eventPublisher;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, cursorUtils, eventPublisher);
    }

    @Test
    void getAccountsViewPaginated_whenNoCursorAndNoSortField_shouldReturnMappedAccountsWithoutNextCursor() {
        Account account = account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", true);
        mockWindow(Window.from(List.of(account), index -> ScrollPosition.keyset(), false));

        var response = accountService.getAccountsViewPaginated(
                jwt(USER_ID),
                null,
                null,
                null,
                null,
                null,
                10,
                null,
                Sort.Direction.DESC);

        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.getData()).singleElement().satisfies(view -> {
            assertThat(view.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(view.getInstitutionName()).isEqualTo("Bank");
            assertThat(view.getAccountName()).isEqualTo("Checking");
            assertThat(view.getCurrentBalance()).isEqualByComparingTo("100.00");
            assertThat(view.getAvailableBalance()).isEqualByComparingTo("90.00");
            assertThat(view.getIsoCurrencyCode()).isEqualTo("USD");
        });
        verify(cursorUtils, never()).decode(any());
        verify(cursorUtils, never()).encode(any());
    }

    @Test
    void getAccountsViewPaginated_whenCursorAndMoreResultsExist_shouldDecodeCursorAndEncodeNextCursor() {
        Account account = account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", true);
        ScrollPosition decodedPosition = ScrollPosition.keyset();
        when(cursorUtils.decode("cursor-1")).thenReturn(decodedPosition);
        when(cursorUtils.encode(any(ScrollPosition.class))).thenReturn("cursor-2");
        mockWindow(Window.from(List.of(account), index -> ScrollPosition.forward(MapFactory.keys(ACCOUNT_ID)), true));

        var response = accountService.getAccountsViewPaginated(
                jwt(USER_ID),
                "check",
                "bank",
                "depository",
                "checking",
                "cursor-1",
                20,
                AccountSortField.CURRENT_BALANCE,
                Sort.Direction.ASC);

        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo("cursor-2");
        verify(cursorUtils).decode("cursor-1");
        verify(cursorUtils).encode(any(ScrollPosition.class));
    }

    @Test
    void getAccountsViewPaginated_whenRepositoryReturnsEmptyWindow_shouldReturnEmptyCursorResponse() {
        mockWindow(Window.from(List.of(), index -> ScrollPosition.keyset(), false));

        var response = accountService.getAccountsViewPaginated(
                jwt(USER_ID),
                "",
                " ",
                null,
                null,
                null,
                1,
                AccountSortField.ACCOUNT_NAME,
                Sort.Direction.ASC);

        assertThat(response.getData()).isEmpty();
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        verify(cursorUtils, never()).encode(any());
    }

    @Test
    void getAccount_whenAccountBelongsToUser_shouldReturnAccountResponse() {
        when(accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", true)));

        var response = accountService.getAccount(ACCOUNT_ID, jwt(USER_ID));

        assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getAccountName()).isEqualTo("Checking");
        assertThat(response.getCurrentBalance()).isEqualByComparingTo("100.00");
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("90.00");
    }

    @ParameterizedTest
    @MethodSource("inaccessibleAccountCases")
    void getAccount_whenAccountIsMissingInactiveOrForeign_shouldThrowNotFound(String caseName, UUID accountId, UUID userId) {
        when(accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(accountId, jwt(userId)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("This account is not exists, or access restricted.");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-a-uuid", "", "a3000000-0000-0000-0000-not-a-uuid" })
    void getAccount_whenJwtSubjectIsMalformed_shouldThrowIllegalArgumentExceptionAndNotQueryRepository(String subject) {
        Jwt principal = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();

        assertThatThrownBy(() -> accountService.getAccount(ACCOUNT_ID, principal))
                .isInstanceOf(IllegalArgumentException.class);
        verify(accountRepository, never()).findByAccountIdAndUserIdAndIsActiveTrue(any(), any());
    }

    @Test
    void getAccountSummary_whenActiveAccountsExist_shouldReturnCountAndCurrencyTotals() {
        when(accountRepository.summarizeActiveBalancesByCurrency(USER_ID)).thenReturn(List.of(
                total("USD", "150.00", "125.00"),
                total("EUR", "80.00", "70.00")));
        when(accountRepository.countByUserIdAndIsActiveTrue(USER_ID)).thenReturn(3L);

        var response = accountService.getAccountSummary(jwt(USER_ID));

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getActiveAccountCount()).isEqualTo(3L);
        assertThat(response.getTotalsByCurrency()).extracting("isoCurrencyCode").containsExactly("USD", "EUR");
        assertThat(response.getTotalsByCurrency().getFirst().getCurrentBalanceTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void getAccountSummary_whenNoActiveAccountsExist_shouldReturnZeroCountAndEmptyTotals() {
        when(accountRepository.summarizeActiveBalancesByCurrency(USER_ID)).thenReturn(List.of());
        when(accountRepository.countByUserIdAndIsActiveTrue(USER_ID)).thenReturn(0L);

        var response = accountService.getAccountSummary(jwt(USER_ID));

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getActiveAccountCount()).isZero();
        assertThat(response.getTotalsByCurrency()).isEmpty();
    }

    @Test
    void getAccountsByGivenIds_shouldDelegateToRepositoryAndReturnNames() {
        AccountNameRequestDto request = new AccountNameRequestDto(Set.of(ACCOUNT_ID, OTHER_ACCOUNT_ID));
        when(accountRepository.findByAccountIdIn(request.getAccountIds()))
                .thenReturn(List.of(
                        new com.batu.shared.dto.response.AccountNameResponseDto(ACCOUNT_ID, "Checking"),
                        new com.batu.shared.dto.response.AccountNameResponseDto(OTHER_ACCOUNT_ID, "Savings")));

        var response = accountService.getAccountsByGivenIds(request);

        assertThat(response).hasSize(2);
        assertThat(response).extracting("accountName").containsExactly("Checking", "Savings");
        verify(accountRepository).findByAccountIdIn(request.getAccountIds());
    }

    @Test
    void getAccountsByGivenIds_whenRequestIsEmpty_shouldReturnEmptyList() {
        AccountNameRequestDto request = new AccountNameRequestDto(Set.of());
        when(accountRepository.findByAccountIdIn(request.getAccountIds())).thenReturn(List.of());

        var response = accountService.getAccountsByGivenIds(request);

        assertThat(response).isEmpty();
        verify(accountRepository).findByAccountIdIn(request.getAccountIds());
    }

    @Test
    void findAccountsByConnectionId_shouldReturnActiveAccountsForConnection() {
        when(accountRepository.findByConnectionIdAndIsActiveTrueOrderByCreatedAtDesc(CONNECTION_ID))
                .thenReturn(List.of(account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", true)));

        var response = accountService.findAccountsByConnectionId(CONNECTION_ID);

        assertThat(response).singleElement().satisfies(account -> {
            assertThat(account.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(account.getAccountName()).isEqualTo("Checking");
        });
    }

    @Test
    void findAccountsByConnectionId_whenNoActiveAccountsExist_shouldReturnEmptyList() {
        when(accountRepository.findByConnectionIdAndIsActiveTrueOrderByCreatedAtDesc(CONNECTION_ID))
                .thenReturn(List.of());

        var response = accountService.findAccountsByConnectionId(CONNECTION_ID);

        assertThat(response).isEmpty();
    }

    @Test
    void upsertAccount_whenRepositoryReturnsAccount_shouldPublishRecordedEventAndReturnResponse() {
        AccountUpsertRequestDto request = upsertRequest(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking");
        Account savedAccount = account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", true);
        when(accountRepository.upsertAccount(request)).thenReturn(savedAccount);
        ArgumentCaptor<AccountRecorded> eventCaptor = ArgumentCaptor.forClass(AccountRecorded.class);

        var response = accountService.upsertAccount(request);

        verify(eventPublisher).publishAccountRecorded(eventCaptor.capture());
        AccountRecorded event = eventCaptor.getValue();
        assertThat(event.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.getUserId()).isEqualTo(USER_ID);
        assertThat(event.getConnectionId()).isEqualTo(CONNECTION_ID);
        assertThat(event.getAccountName()).isEqualTo("Checking");
        assertThat(event.isActive()).isTrue();
        assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void upsertAccount_whenRepositoryReturnsInactiveAccount_shouldPublishInactiveRecordedEventAndReturnInactiveResponse() {
        AccountUpsertRequestDto request = upsertRequest(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Closed Checking");
        Account savedAccount = account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Closed Checking", false);
        when(accountRepository.upsertAccount(request)).thenReturn(savedAccount);
        ArgumentCaptor<AccountRecorded> eventCaptor = ArgumentCaptor.forClass(AccountRecorded.class);

        var response = accountService.upsertAccount(request);

        verify(eventPublisher).publishAccountRecorded(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isActive()).isFalse();
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void upsertAccount_whenRepositoryFails_shouldNotPublishRecordedEvent() {
        AccountUpsertRequestDto request = upsertRequest(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking");
        RuntimeException failure = new RuntimeException("database unavailable");
        when(accountRepository.upsertAccount(request)).thenThrow(failure);

        assertThatThrownBy(() -> accountService.upsertAccount(request)).isSameAs(failure);
        verify(eventPublisher, never()).publishAccountRecorded(any(AccountRecorded.class));
    }

    @Test
    void deactivateAccountsByConnection_whenActiveAccountsExist_shouldDeactivateSaveAndPublishRemovedEvents() {
        Account checking = account(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", true);
        Account savings = account(OTHER_ACCOUNT_ID, USER_ID, CONNECTION_ID, "Savings", true);
        when(accountRepository.findByConnectionIdAndIsActiveTrue(CONNECTION_ID)).thenReturn(List.of(checking, savings));
        when(accountRepository.saveAll(List.of(checking, savings))).thenReturn(List.of(checking, savings));
        ArgumentCaptor<AccountRemoved> eventCaptor = ArgumentCaptor.forClass(AccountRemoved.class);

        var response = accountService.deactivateAccountsByConnection(CONNECTION_ID);

        assertThat(checking.isActive()).isFalse();
        assertThat(savings.isActive()).isFalse();
        assertThat(response).hasSize(2).allSatisfy(account -> assertThat(account.isActive()).isFalse());
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishAccountRemoved(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(AccountRemoved::getAccountId)
                .containsExactly(ACCOUNT_ID, OTHER_ACCOUNT_ID);
    }

    @Test
    void deactivateAccountsByConnection_whenNoAccountsExist_shouldReturnEmptyListAndNotPublishEvents() {
        when(accountRepository.findByConnectionIdAndIsActiveTrue(CONNECTION_ID)).thenReturn(List.of());
        when(accountRepository.saveAll(List.of())).thenReturn(List.of());

        var response = accountService.deactivateAccountsByConnection(CONNECTION_ID);

        assertThat(response).isEmpty();
        verify(eventPublisher, never()).publishAccountRemoved(any(AccountRemoved.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void mockWindow(Window<Account> window) {
        doReturn(window).when(accountRepository).findBy(
                any(Specification.class),
                any(Function.class));
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }

    private static Stream<Arguments> inaccessibleAccountCases() {
        return Stream.of(
                Arguments.of("missing", ACCOUNT_ID, USER_ID),
                Arguments.of("inactive", ACCOUNT_ID, USER_ID),
                Arguments.of("foreign", ACCOUNT_ID, OTHER_USER_ID));
    }

    private Account account(UUID accountId, UUID userId, UUID connectionId, String accountName, boolean active) {
        return new Account(
                accountId,
                userId,
                connectionId,
                "Bank",
                accountName,
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                active);
    }

    private AccountUpsertRequestDto upsertRequest(UUID accountId, UUID userId, UUID connectionId, String accountName) {
        return new AccountUpsertRequestDto(
                accountId,
                userId,
                connectionId,
                "Bank",
                accountName,
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD");
    }

    private AccountCurrencyTotalProjection total(String currency, String currentBalance, String availableBalance) {
        return new AccountCurrencyTotalProjection() {
            @Override
            public String getIsoCurrencyCode() {
                return currency;
            }

            @Override
            public BigDecimal getCurrentBalanceTotal() {
                return new BigDecimal(currentBalance);
            }

            @Override
            public BigDecimal getAvailableBalanceTotal() {
                return new BigDecimal(availableBalance);
            }
        };
    }

    private static final class MapFactory {
        private MapFactory() {
        }

        static java.util.Map<String, Object> keys(UUID accountId) {
            return java.util.Map.of("accountId", accountId);
        }
    }
}
