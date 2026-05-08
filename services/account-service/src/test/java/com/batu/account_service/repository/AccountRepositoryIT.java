package com.batu.account_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.account_service.entity.Account;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.shared.dto.request.AccountUpsertRequestDto;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountRepositoryIT.PostgreSqlTestcontainersConfiguration.class)
class AccountRepositoryIT {

    private static final UUID ACCOUNT_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    private static final UUID SAVINGS_ACCOUNT_ID = UUID.fromString("b1000000-0000-0000-0000-000000000002");
    private static final UUID FOREIGN_ACCOUNT_ID = UUID.fromString("b1000000-0000-0000-0000-000000000003");
    private static final UUID USER_ID = UUID.fromString("b1000000-0000-0000-0000-000000000004");
    private static final UUID OTHER_USER_ID = UUID.fromString("b1000000-0000-0000-0000-000000000005");
    private static final UUID CONNECTION_ID = UUID.fromString("b1000000-0000-0000-0000-000000000006");
    private static final UUID OTHER_CONNECTION_ID = UUID.fromString("b1000000-0000-0000-0000-000000000007");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByAccountIdAndUserIdAndIsActiveTrue_whenAccountIsOwnedAndActive_shouldReturnAccount() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "Bank", "depository", "checking", "100.00", "90.00", "USD", true);

        var result = accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getAccountName()).isEqualTo("Checking");
    }

    @Test
    void findByAccountIdAndUserIdAndIsActiveTrue_whenAccountIsForeignOrInactive_shouldReturnEmpty() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "Bank", "depository", "checking", "100.00", "90.00", "USD", false);
        saveAccount(FOREIGN_ACCOUNT_ID, OTHER_USER_ID, CONNECTION_ID, "Foreign", "Bank", "depository", "checking", "200.00", "180.00", "USD", true);

        assertThat(accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID)).isEmpty();
        assertThat(accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(FOREIGN_ACCOUNT_ID, USER_ID)).isEmpty();
    }

    @Test
    void summarizeActiveBalancesByCurrency_whenMultipleCurrenciesExist_shouldGroupActiveOwnedTotalsOnly() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "Bank", "depository", "checking", "100.25", "90.25", "USD", true);
        saveAccount(SAVINGS_ACCOUNT_ID, USER_ID, CONNECTION_ID, "Savings", "Bank", "depository", "savings", "50.75", "40.75", "USD", true);
        saveAccount(UUID.fromString("b1000000-0000-0000-0000-000000000008"), USER_ID, CONNECTION_ID, "Euro", "Euro Bank", "depository", "checking", "20.00", "15.00", "EUR", true);
        saveAccount(UUID.fromString("b1000000-0000-0000-0000-000000000009"), USER_ID, CONNECTION_ID, "Inactive", "Bank", "depository", "checking", "999.00", "999.00", "USD", false);
        saveAccount(FOREIGN_ACCOUNT_ID, OTHER_USER_ID, CONNECTION_ID, "Foreign", "Bank", "depository", "checking", "999.00", "999.00", "USD", true);

        var result = accountRepository.summarizeActiveBalancesByCurrency(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(total -> {
            assertThat(total.getIsoCurrencyCode()).isEqualTo("USD");
            assertThat(total.getCurrentBalanceTotal()).isEqualByComparingTo("151.00");
            assertThat(total.getAvailableBalanceTotal()).isEqualByComparingTo("131.00");
        });
        assertThat(result).anySatisfy(total -> {
            assertThat(total.getIsoCurrencyCode()).isEqualTo("EUR");
            assertThat(total.getCurrentBalanceTotal()).isEqualByComparingTo("20.00");
            assertThat(total.getAvailableBalanceTotal()).isEqualByComparingTo("15.00");
        });
        assertThat(accountRepository.countByUserIdAndIsActiveTrue(USER_ID)).isEqualTo(3L);
    }

    @Test
    void findByAccountIdIn_whenMatchingIdsExist_shouldReturnAccountNames() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "Bank", "depository", "checking", "100.00", "90.00", "USD", true);
        saveAccount(SAVINGS_ACCOUNT_ID, USER_ID, CONNECTION_ID, "Savings", "Bank", "depository", "savings", "50.00", "40.00", "USD", true);

        var result = accountRepository.findByAccountIdIn(Set.of(ACCOUNT_ID, SAVINGS_ACCOUNT_ID));

        assertThat(result).extracting("accountName").containsExactlyInAnyOrder("Checking", "Savings");
    }

    @Test
    void findBySpecification_whenFiltersAreProvided_shouldReturnOnlyActiveOwnedMatchingAccounts() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Everyday Checking", "Acme Bank", "depository", "checking", "100.00", "90.00", "USD", true);
        saveAccount(SAVINGS_ACCOUNT_ID, USER_ID, CONNECTION_ID, "Long Term Savings", "Acme Bank", "depository", "savings", "50.00", "40.00", "USD", true);
        saveAccount(UUID.fromString("b1000000-0000-0000-0000-000000000010"), USER_ID, CONNECTION_ID, "Closed Checking", "Acme Bank", "depository", "checking", "25.00", "20.00", "USD", false);
        saveAccount(FOREIGN_ACCOUNT_ID, OTHER_USER_ID, CONNECTION_ID, "Foreign Checking", "Acme Bank", "depository", "checking", "999.00", "999.00", "USD", true);

        var spec = AccountSpecification.filter(USER_ID, "check", "acme", "depository", "checking");
        var result = accountRepository.findBy(spec, query -> query
                .sortBy(Sort.by(Sort.Direction.ASC, "accountName").and(Sort.by("accountId")))
                .limit(10)
                .scroll(ScrollPosition.keyset()));

        assertThat(result.getContent()).extracting(Account::getAccountId).containsExactly(ACCOUNT_ID);
    }

    @Test
    void findActiveAccountRemovalsByConnectionId_whenAccountsExist_shouldReturnOnlyActiveMatchingAccounts() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "Bank", "depository", "checking", "100.00", "90.00", "USD", true);
        saveAccount(SAVINGS_ACCOUNT_ID, USER_ID, CONNECTION_ID, "Savings", "Bank", "depository", "savings", "50.00", "40.00", "USD", false);
        saveAccount(FOREIGN_ACCOUNT_ID, OTHER_USER_ID, OTHER_CONNECTION_ID, "Foreign", "Bank", "depository", "checking", "200.00", "180.00", "USD", true);

        var result = accountRepository.findActiveAccountRemovalsByConnectionId(CONNECTION_ID);

        assertThat(result).singleElement().satisfies(account -> {
            assertThat(account.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(account.getUserId()).isEqualTo(USER_ID);
            assertThat(account.getConnectionId()).isEqualTo(CONNECTION_ID);
        });
    }

    @Test
    void deactivateActiveAccountsByConnectionId_whenAccountsExist_shouldBulkDeactivateOnlyActiveMatchingAccounts() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "Bank", "depository", "checking", "100.00", "90.00", "USD", true);
        saveAccount(SAVINGS_ACCOUNT_ID, USER_ID, CONNECTION_ID, "Savings", "Bank", "depository", "savings", "50.00", "40.00", "USD", false);
        saveAccount(FOREIGN_ACCOUNT_ID, OTHER_USER_ID, OTHER_CONNECTION_ID, "Foreign", "Bank", "depository", "checking", "200.00", "180.00", "USD", true);

        int updated = accountRepository.deactivateActiveAccountsByConnectionId(CONNECTION_ID);
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(accountRepository.findById(ACCOUNT_ID).orElseThrow().isActive()).isFalse();
        assertThat(accountRepository.findById(SAVINGS_ACCOUNT_ID).orElseThrow().isActive()).isFalse();
        assertThat(accountRepository.findById(FOREIGN_ACCOUNT_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    void upsertAccount_whenAccountIsNew_shouldInsertActiveAccount() {
        var request = upsertRequest(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Checking", "100.00", "90.00");

        Account result = accountRepository.upsertAccount(request);
        entityManager.clear();

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID)).isPresent();
    }

    @Test
    void upsertAccount_whenAccountExistsInactive_shouldUpdateFieldsAndReactivate() {
        saveAccount(ACCOUNT_ID, USER_ID, CONNECTION_ID, "Old Checking", "Old Bank", "depository", "checking", "10.00", "5.00", "USD", false);

        Account result = accountRepository.upsertAccount(upsertRequest(ACCOUNT_ID, USER_ID, OTHER_CONNECTION_ID, "Updated Checking", "200.00", "150.00"));
        entityManager.clear();

        assertThat(result.isActive()).isTrue();
        assertThat(result.getConnectionId()).isEqualTo(OTHER_CONNECTION_ID);
        assertThat(result.getAccountName()).isEqualTo("Updated Checking");
        assertThat(result.getCurrentBalance()).isEqualByComparingTo("200.00");
        assertThat(accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(ACCOUNT_ID, USER_ID)).isPresent();
    }

    private Account saveAccount(
            UUID accountId,
            UUID userId,
            UUID connectionId,
            String accountName,
            String institutionName,
            String accountType,
            String accountSubtype,
            String currentBalance,
            String availableBalance,
            String currency,
            boolean active) {
        Account account = new Account(
                accountId,
                userId,
                connectionId,
                institutionName,
                accountName,
                accountType,
                accountSubtype,
                "1234",
                new BigDecimal(currentBalance),
                new BigDecimal(availableBalance),
                currency,
                active);
        Account savedAccount = accountRepository.saveAndFlush(account);
        entityManager.clear();
        return savedAccount;
    }

    private AccountUpsertRequestDto upsertRequest(
            UUID accountId,
            UUID userId,
            UUID connectionId,
            String accountName,
            String currentBalance,
            String availableBalance) {
        return new AccountUpsertRequestDto(
                accountId,
                userId,
                connectionId,
                "Bank",
                accountName,
                "depository",
                "checking",
                "1234",
                new BigDecimal(currentBalance),
                new BigDecimal(availableBalance),
                "USD");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSqlTestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }
}
