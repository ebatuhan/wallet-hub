package com.batu.transaction_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.event.TransactionRemoved;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.mapper.TransactionSyncMapper;
import com.batu.transaction_service.messaging.OutboxDomainEventPublisher;
import com.batu.transaction_service.repository.DetailedCategoryRepository;
import com.batu.transaction_service.repository.PrimaryCategoryRepository;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.service.impl.DetailedCategoryServiceImpl;
import com.batu.transaction_service.service.impl.TransactionServiceImpl;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TransactionServiceImpl.class,
        DetailedCategoryServiceImpl.class,
        TransactionSyncMapper.class,
        TransactionServiceImplIT.ServiceTestConfiguration.class
})
class TransactionServiceImplIT {

    private static final UUID TRANSACTION_ID = UUID.fromString("d2000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TRANSACTION_ID = UUID.fromString("d2000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("d2000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_USER_ID = UUID.fromString("d2000000-0000-0000-0000-000000000004");
    private static final UUID ACCOUNT_ID = UUID.fromString("d2000000-0000-0000-0000-000000000005");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("d2000000-0000-0000-0000-000000000006");

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PrimaryCategoryRepository primaryCategoryRepository;

    @Autowired
    private DetailedCategoryRepository detailedCategoryRepository;

    @Autowired
    private DetailedCategoryServiceImpl detailedCategoryService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private OutboxDomainEventPublisher eventPublisher;

    @BeforeEach
    void clearDetailedCategoryCache() {
        ((java.util.Map<?, ?>) ReflectionTestUtils.getField(detailedCategoryService, "categoryCache")).clear();
    }

    @Test
    void transactions_whenOtherUsersAndInactiveRowsExist_shouldReturnOnlyActiveOwnedMatchingTransactions() {
        TransactionDetailedCategory foodCategory = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        TransactionDetailedCategory travelCategory = saveCategory("TRAVEL", "TRAVEL_FLIGHTS");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, foodCategory, true, "Coffee Shop");
        saveTransaction(OTHER_TRANSACTION_ID, USER_ID, ACCOUNT_ID, travelCategory, true, "Flight");
        saveTransaction(UUID.fromString("d2000000-0000-0000-0000-000000000007"), USER_ID, ACCOUNT_ID, foodCategory, false, "Inactive");
        saveTransaction(UUID.fromString("d2000000-0000-0000-0000-000000000008"), OTHER_USER_ID, ACCOUNT_ID, foodCategory, true, "Foreign");

        var response = transactionService.transactions(USER_ID, "FOOD_AND_DRINK", ACCOUNT_ID, null, 10);

        assertThat(response.getData()).extracting("transactionId").containsExactly(TRANSACTION_ID);
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    void getTransactionById_whenTransactionIsInactive_shouldThrowNotFound() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, false, "Inactive");

        assertThatThrownBy(() -> transactionService.getTransactionById(USER_ID, TRANSACTION_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void upsertTransaction_whenRequestIsActive_shouldPersistAndPublishRecordedEvent() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");

        var response = transactionService.upsertTransaction(upsertRequest(TRANSACTION_ID, ACCOUNT_ID, "Coffee Shop", true));
        entityManager.clear();

        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.isActive()).isTrue();
        assertThat(response.getDetailedCategoryCode()).isEqualTo(category.getCategoryCode());
        assertThat(transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(TRANSACTION_ID, USER_ID)).isPresent();
        verify(eventPublisher).publishTransactionRecorded(any(TransactionRecorded.class));
    }

    @Test
    void upsertTransaction_whenRequestIsInactive_shouldPersistAndPublishRemovedEvent() {
        saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");

        var response = transactionService.upsertTransaction(upsertRequest(TRANSACTION_ID, ACCOUNT_ID, "Coffee Shop", false));
        entityManager.clear();

        assertThat(response.isActive()).isFalse();
        assertThat(transactionRepository.findById(TRANSACTION_ID)).isPresent();
        verify(eventPublisher).publishTransactionRemoved(any(TransactionRemoved.class));
    }

    @Test
    void deactivateTransactionsByAccountId_whenActiveTransactionsExist_shouldBulkDeactivateWithoutPublishingRemovedEvents() {
        TransactionDetailedCategory category = saveCategory("FOOD_AND_DRINK", "FOOD_AND_DRINK_COFFEE");
        saveTransaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, category, true, "Coffee Shop");
        saveTransaction(OTHER_TRANSACTION_ID, USER_ID, OTHER_ACCOUNT_ID, category, true, "Other Account");

        transactionService.deactivateTransactionsByAccountId(ACCOUNT_ID);
        transactionRepository.flush();
        entityManager.clear();

        assertThat(transactionRepository.findById(TRANSACTION_ID).orElseThrow().isActive()).isFalse();
        assertThat(transactionRepository.findById(OTHER_TRANSACTION_ID).orElseThrow().isActive()).isTrue();
        verify(eventPublisher, never()).publishTransactionRemoved(any(TransactionRemoved.class));
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

    private TransactionUpsertRequestDto upsertRequest(UUID transactionId, UUID accountId, String name, boolean active) {
        return new TransactionUpsertRequestDto(
                transactionId,
                USER_ID,
                accountId,
                new BigDecimal("42.50"),
                "USD",
                name,
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                "FOOD_AND_DRINK_COFFEE",
                active);
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
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }
}
