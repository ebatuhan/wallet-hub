package com.batu.transaction_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.spec.TransactionSpecs;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransactionRepositoryIT.PostgreSqlTestcontainersConfiguration.class)
class TransactionRepositoryIT {

    private static final UUID TRANSACTION_ID = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TRANSACTION_ID = UUID.fromString("d1000000-0000-0000-0000-000000000002");
    private static final UUID FOREIGN_TRANSACTION_ID = UUID.fromString("d1000000-0000-0000-0000-000000000003");
    private static final UUID USER_ID = UUID.fromString("d1000000-0000-0000-0000-000000000004");
    private static final UUID OTHER_USER_ID = UUID.fromString("d1000000-0000-0000-0000-000000000005");
    private static final UUID ACCOUNT_ID = UUID.fromString("d1000000-0000-0000-0000-000000000006");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("d1000000-0000-0000-0000-000000000007");

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PrimaryCategoryRepository primaryCategoryRepository;

    @Autowired
    private DetailedCategoryRepository detailedCategoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByTransactionIdAndUserIdAndIsActiveTrue_whenTransactionIsOwnedAndActive_shouldReturnTransaction() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, true, "Coffee Shop");

        var result = transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(TRANSACTION_ID, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getTransactionName()).isEqualTo("Coffee Shop");
    }

    @Test
    void findByTransactionIdAndUserIdAndIsActiveTrue_whenTransactionIsInactiveOrForeign_shouldReturnEmpty() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, false, "Inactive");
        saveTransaction(FOREIGN_TRANSACTION_ID, OTHER_USER_ID, ACCOUNT_ID, category, true, "Foreign");

        assertThat(transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(TRANSACTION_ID, USER_ID)).isEmpty();
        assertThat(transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(FOREIGN_TRANSACTION_ID, USER_ID)).isEmpty();
    }

    @Test
    void findByTransactionIdAndUserIdWithCategory_whenTransactionExists_shouldFetchCategoryTree() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, true, "Coffee Shop");

        var result = transactionRepository.findByTransactionIdAndUserIdWithCategory(TRANSACTION_ID, USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getDetailedCategory().getCategoryCode()).isEqualTo("FOOD_AND_DRINK_COFFEE");
        assertThat(result.get().getDetailedCategory().getTransactionPrimaryCategory().getCategoryCode()).isEqualTo("FOOD_AND_DRINK");
    }

    @Test
    void findByAccountIdAndIsActiveTrue_whenTransactionsExist_shouldReturnOnlyActiveAccountTransactions() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, true, "Coffee Shop");
        saveTransaction(OTHER_TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, false, "Inactive");
        saveTransaction(FOREIGN_TRANSACTION_ID, USER_ID, OTHER_ACCOUNT_ID, category, true, "Other Account");

        var result = transactionRepository.findByAccountIdAndIsActiveTrue(ACCOUNT_ID);

        assertThat(result).extracting(Transaction::getTransactionId).containsExactly(TRANSACTION_ID);
    }

    @Test
    void deactivateActiveTransactionsByAccountId_whenTransactionsExist_shouldBulkDeactivateOnlyActiveMatchingTransactions() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, true, "Coffee Shop");
        saveTransaction(OTHER_TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, false, "Inactive");
        saveTransaction(FOREIGN_TRANSACTION_ID, USER_ID, OTHER_ACCOUNT_ID, category, true, "Other Account");

        int updated = transactionRepository.deactivateActiveTransactionsByAccountId(ACCOUNT_ID);
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(transactionRepository.findById(TRANSACTION_ID).orElseThrow().isActive()).isFalse();
        assertThat(transactionRepository.findById(OTHER_TRANSACTION_ID).orElseThrow().isActive()).isFalse();
        assertThat(transactionRepository.findById(FOREIGN_TRANSACTION_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    void findBySpecification_whenFiltersAreProvided_shouldReturnOnlyActiveOwnedMatchingTransactions() {
        TransactionDetailedCategory foodCategory = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        TransactionDetailedCategory travelCategory = saveCategory("TRAVEL", "TRAVEL_FLIGHTS");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, foodCategory, true, "Coffee Shop");
        saveTransaction(OTHER_TRANSACTION_ID, USER_ID, ACCOUNT_ID, travelCategory, true, "Flight");
        saveTransaction(UUID.fromString("d1000000-0000-0000-0000-000000000008"), USER_ID, ACCOUNT_ID, foodCategory, false, "Inactive");
        saveTransaction(FOREIGN_TRANSACTION_ID, OTHER_USER_ID, ACCOUNT_ID, foodCategory, true, "Foreign");

        var spec = TransactionSpecs.withDynamicFilters(USER_ID, ACCOUNT_ID, "FOOD_AND_DRINK");
        var result = transactionRepository.findBy(spec, query -> query
                .sortBy(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("transactionId")))
                .limit(10)
                .scroll(ScrollPosition.keyset()));

        assertThat(result.getContent()).extracting(Transaction::getTransactionId).containsExactly(TRANSACTION_ID);
    }

    @Test
    void upsertTransaction_whenTransactionIsNew_shouldInsertTransaction() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");

        Transaction result = transactionRepository.upsertTransaction(upsertRequest(TRANSACTION_ID, "Coffee Shop", true),
                category.getTransactionDetailedCategoryId());
        entityManager.clear();

        assertThat(result.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(TRANSACTION_ID, USER_ID)).isPresent();
    }

    @Test
    void upsertTransaction_whenTransactionExists_shouldUpdateFieldsAndActiveState() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, true, "Old Name");

        Transaction result = transactionRepository.upsertTransaction(upsertRequest(TRANSACTION_ID, "Updated Name", false),
                category.getTransactionDetailedCategoryId());
        entityManager.clear();

        assertThat(result.getTransactionName()).isEqualTo("Updated Name");
        assertThat(result.isActive()).isFalse();
        assertThat(transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(TRANSACTION_ID, USER_ID)).isEmpty();
    }

    private TransactionDetailedCategory saveCategory(String primaryCode, String detailedCode) {
        TransactionPrimaryCategory primaryCategory = primaryCategoryRepository.saveAndFlush(
                new TransactionPrimaryCategory(primaryCode, primaryCode.replace('_', ' '), primaryCode.toLowerCase() + ".svg"));
        TransactionDetailedCategory detailedCategory = detailedCategoryRepository.saveAndFlush(
                new TransactionDetailedCategory(detailedCode.replace('_', ' '), detailedCode, primaryCategory, "Description"));
        entityManager.clear();
        return detailedCategory;
    }

    private Transaction saveTransaction(
            UUID transactionId,
            UUID userId,
            UUID accountId,
            TransactionDetailedCategory category,
            boolean active,
            String name) {
        Transaction transaction = new Transaction(
                transactionId,
                userId,
                accountId,
                new BigDecimal("42.50"),
                "USD",
                name,
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                entityManager.merge(category),
                active);
        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);
        entityManager.clear();
        return savedTransaction;
    }

    private TransactionUpsertRequestDto upsertRequest(UUID transactionId, String name, boolean active) {
        return new TransactionUpsertRequestDto(
                transactionId,
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("84.00"),
                "USD",
                name,
                "place",
                LocalDate.of(2026, 5, 8),
                false,
                "in store",
                "FOOD_AND_DRINK_COFFEE",
                active);
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
