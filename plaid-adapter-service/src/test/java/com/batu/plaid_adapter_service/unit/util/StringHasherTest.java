package com.batu.plaid_adapter_service.unit.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.util.StringHasher;

class StringHasherTest {

    private final StringHasher stringHasher = new StringHasher();

    @Test
    void sha256_whenValueProvided_shouldReturnExpectedLowercaseHexDigest() {
        assertThat(stringHasher.sha256("wallet-hub"))
                .isEqualTo("d71386a7e6bc534f8edceaec4dc9d177db6fa86a02ee753f214856137d71300b");
    }

    @Test
    void sha256_whenCalledRepeatedly_shouldReturnStableDigest() {
        assertThat(stringHasher.sha256("same-value")).isEqualTo(stringHasher.sha256("same-value"));
    }
}
