package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.batu.plaid_adapter_service.TestSupportConfiguration;
import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.repository.AccountRegistryRepository;
import com.batu.plaid_adapter_service.service.AccountRegistryService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSupportConfiguration.class)
class RegistryServiceConcurrencyIntegrationTest {

    @Autowired
    private AccountRegistryService accountRegistryService;

    @Autowired
    private AccountRegistryRepository accountRegistryRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        accountRegistryRepository.deleteAll();
    }

    @Test
    void registerAccount_reusesExistingRegistryRow() {
        UUID connectionId = UUID.randomUUID();
        UUID accountId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        AccountRegistry firstResult = accountRegistryService.registerAccount(connectionId, accountId, "fingerprint-1");
        AccountRegistry secondResult = accountRegistryService.registerAccount(connectionId, accountId, "fingerprint-1");

        assertEquals(firstResult.getAccountRegistryId(), secondResult.getAccountRegistryId());
        assertEquals(1, accountRegistryRepository.count());
    }
}
