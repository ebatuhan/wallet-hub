package com.batu.plaid_adapter_service.unit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.util.DeterministicIdGenerator;

class DeterministicIdGeneratorTest {

    private static final UUID USER_ID = UUID.fromString("72000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("72000000-0000-0000-0000-000000000002");

    private final DeterministicIdGenerator generator = new DeterministicIdGenerator();

    @Test
    void accountId_whenSameInputsProvided_shouldReturnStableUuid() {
        UUID first = generator.accountId(USER_ID, "plaid-account-1");
        UUID second = generator.accountId(USER_ID, "plaid-account-1");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void accountId_whenUserOrExternalIdDiffers_shouldReturnDifferentUuid() {
        UUID base = generator.accountId(USER_ID, "plaid-account-1");

        assertThat(generator.accountId(OTHER_USER_ID, "plaid-account-1")).isNotEqualTo(base);
        assertThat(generator.accountId(USER_ID, "plaid-account-2")).isNotEqualTo(base);
    }

    @Test
    void transactionId_whenSameInputsProvided_shouldReturnStableUuid() {
        UUID first = generator.transactionId(USER_ID, "plaid-account-1", "plaid-transaction-1");
        UUID second = generator.transactionId(USER_ID, "plaid-account-1", "plaid-transaction-1");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void transactionId_whenTransactionIdDiffers_shouldReturnDifferentUuid() {
        UUID base = generator.transactionId(USER_ID, "plaid-account-1", "plaid-transaction-1");

        assertThat(generator.transactionId(USER_ID, "plaid-account-1", "plaid-transaction-2")).isNotEqualTo(base);
    }
}
