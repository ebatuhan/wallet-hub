package com.batu.budgeting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.entity.BudgetPeriod;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BudgetRepositoryIT.PostgreSqlTestcontainersConfiguration.class)
class BudgetRepositoryIT {

    private static final UUID USER_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("70000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("70000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("70000000-0000-0000-0000-000000000004");

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByIdAndUserId_whenBudgetBelongsToUser_shouldReturnBudget() {
        Budget budget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));

        Optional<Budget> result = budgetRepository.findByIdAndUserId(budget.getId(), USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void findByIdAndUserId_whenBudgetBelongsToDifferentUser_shouldReturnEmpty() {
        Budget budget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));

        Optional<Budget> result = budgetRepository.findByIdAndUserId(budget.getId(), OTHER_USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndActiveTrue_whenBudgetsExist_shouldReturnOnlyActiveBudgetsForUser() {
        Budget activeBudget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        Budget inactiveBudget = saveBudget(USER_ID, OTHER_CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 6, 1));
        inactiveBudget.setActive(false);
        budgetRepository.saveAndFlush(inactiveBudget);
        saveBudget(OTHER_USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));

        List<Budget> result = budgetRepository.findByUserIdAndActiveTrue(USER_ID);

        assertThat(result).extracting(Budget::getId).containsExactly(activeBudget.getId());
    }

    @Test
    void findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue_whenFilteringForOverlapCandidates_shouldMatchOnlySameUserCategoryCurrencyAndActive() {
        Budget matchingBudget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        Budget inactiveBudget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 6, 1));
        inactiveBudget.setActive(false);
        budgetRepository.saveAndFlush(inactiveBudget);
        saveBudget(USER_ID, OTHER_CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        saveBudget(USER_ID, CATEGORY_ID, "EUR", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        saveBudget(OTHER_USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));

        List<Budget> result = budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(
                USER_ID,
                CATEGORY_ID,
                "USD");

        assertThat(result).extracting(Budget::getId).containsExactly(matchingBudget.getId());
    }

    @Test
    void findCandidates_whenTransactionDateIsInsideActiveBudgetPeriod_shouldReturnMatchingBudget() {
        Budget matchingBudget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        saveBudget(USER_ID, CATEGORY_ID, "EUR", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        saveBudget(USER_ID, OTHER_CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));

        List<Budget> result = budgetRepository.findCandidates(
                USER_ID,
                CATEGORY_ID,
                "USD",
                LocalDate.of(2026, 5, 15));

        assertThat(result).extracting(Budget::getId).containsExactly(matchingBudget.getId());
    }

    @Test
    void findCandidates_whenMatchingBudgetIsInactive_shouldReturnEmpty() {
        Budget inactiveBudget = saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        inactiveBudget.setActive(false);
        budgetRepository.saveAndFlush(inactiveBudget);
        entityManager.clear();

        List<Budget> result = budgetRepository.findCandidates(
                USER_ID,
                CATEGORY_ID,
                "USD",
                LocalDate.of(2026, 5, 15));

        assertThat(result).isEmpty();
    }

    @Test
    void findCandidates_whenBudgetStartsAfterTransactionDate_shouldReturnEmpty() {
        saveBudget(USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 6, 1));

        List<Budget> result = budgetRepository.findCandidates(
                USER_ID,
                CATEGORY_ID,
                "USD",
                LocalDate.of(2026, 5, 31));

        assertThat(result).isEmpty();
    }

    private Budget saveBudget(UUID userId, UUID categoryId, String currency, BudgetPeriod period, LocalDate periodStart) {
        Budget budget = new Budget(userId, categoryId, new BigDecimal("500.0000"), currency, period, periodStart);
        Budget savedBudget = budgetRepository.saveAndFlush(budget);
        entityManager.clear();
        return savedBudget;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSqlTestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }
}
