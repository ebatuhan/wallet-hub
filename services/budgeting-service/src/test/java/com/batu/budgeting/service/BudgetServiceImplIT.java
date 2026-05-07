package com.batu.budgeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.entity.BudgetPeriod;
import com.batu.budgeting.mapper.BudgetMapper;
import com.batu.budgeting.repository.BudgetRepository;
import com.batu.budgeting.service.impl.BudgetServiceImpl;
import com.batu.shared.cursor.CursorUtils;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ BudgetServiceImpl.class, BudgetMapper.class, BudgetServiceImplIT.ServiceTestConfiguration.class })
class BudgetServiceImplIT {

    private static final UUID USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("a0000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("a0000000-0000-0000-0000-000000000004");

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getBudgets_whenOtherUsersBudgetsExist_shouldReturnOnlyAuthenticatedUsersActiveBudgets() {
        Budget ownActiveBudget = saveBudget(USER_ID, CATEGORY_ID, true);
        saveBudget(OTHER_USER_ID, CATEGORY_ID, true);
        saveBudget(USER_ID, OTHER_CATEGORY_ID, false);

        var response = budgetService.getBudgets(USER_ID, null, 10, null, Sort.Direction.DESC);

        assertThat(response.getData()).extracting(com.batu.budgeting.dto.BudgetResponse::id)
                .containsExactly(ownActiveBudget.getId());
    }

    @Test
    void updateBudget_whenBudgetBelongsToAnotherUser_shouldReturnNotFoundAndLeaveBudgetUnchanged() {
        Budget otherUsersBudget = saveBudget(OTHER_USER_ID, CATEGORY_ID, true);
        CreateBudgetRequest request = new CreateBudgetRequest(
                OTHER_CATEGORY_ID,
                new BigDecimal("900.00"),
                "EUR",
                BudgetPeriod.WEEKLY,
                LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> budgetService.updateBudget(otherUsersBudget.getId(), request, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        Budget unchangedBudget = budgetRepository.findById(otherUsersBudget.getId()).orElseThrow();
        assertThat(unchangedBudget.getUserId()).isEqualTo(OTHER_USER_ID);
        assertThat(unchangedBudget.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(unchangedBudget.getIsoCurrencyCode()).isEqualTo("USD");
        assertThat(unchangedBudget.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
    }

    @Test
    void deactivateBudget_whenBudgetBelongsToAnotherUser_shouldReturnNotFoundAndLeaveBudgetActive() {
        Budget otherUsersBudget = saveBudget(OTHER_USER_ID, CATEGORY_ID, true);

        assertThatThrownBy(() -> budgetService.deactivateBudget(otherUsersBudget.getId(), USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        Budget unchangedBudget = budgetRepository.findById(otherUsersBudget.getId()).orElseThrow();
        assertThat(unchangedBudget.isActive()).isTrue();
    }

    private Budget saveBudget(UUID userId, UUID categoryId, boolean active) {
        Budget budget = new Budget(
                userId,
                categoryId,
                new BigDecimal("500.00"),
                "USD",
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1));
        budget.setActive(active);
        Budget savedBudget = budgetRepository.saveAndFlush(budget);
        entityManager.clear();
        return savedBudget;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ServiceTestConfiguration {

        @Bean
        CursorUtils cursorUtils() {
            return new CursorUtils(new JsonMapper());
        }

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }
}
