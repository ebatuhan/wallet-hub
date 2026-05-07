package com.batu.plaid_adapter_service.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountSubtype;
import com.plaid.client.model.AccountType;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.Transaction;

class PlaidRequestMapperTest {

    private static final UUID USER_ID = UUID.fromString("74000000-0000-0000-0000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("74000000-0000-0000-0000-000000000002");
    private static final UUID TRANSACTION_ID = UUID.fromString("74000000-0000-0000-0000-000000000003");

    private final PlaidRequestMapper mapper = new PlaidRequestMapper();

    @Test
    void toAccountUpsertRequest_whenPlaidAccountHasFullFields_shouldMapAccountFields() {
        Connection connection = connection();
        AccountBase account = account(AccountSubtype.CHECKING, "0000", 125.50, 100.25);

        var request = mapper.toAccountUpsertRequest(connection, ACCOUNT_ID, account);

        assertThat(request.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(request.getUserId()).isEqualTo(USER_ID);
        assertThat(request.getConnectionId()).isEqualTo(connection.getConnectionId());
        assertThat(request.getInstitutionName()).isEqualTo("Test Bank");
        assertThat(request.getAccountName()).isEqualTo("Primary Checking");
        assertThat(request.getAccountType()).isEqualTo("depository");
        assertThat(request.getAccountSubtype()).isEqualTo("checking");
        assertThat(request.getAccountMask()).isEqualTo("0000");
        assertThat(request.getCurrentBalance()).isEqualByComparingTo("125.50");
        assertThat(request.getAvailableBalance()).isEqualByComparingTo("100.25");
        assertThat(request.getIsoCurrencyCode()).isEqualTo("USD");
    }

    @ParameterizedTest
    @MethodSource("optionalAccountFieldCases")
    void toAccountUpsertRequest_whenOptionalFieldsAreMissing_shouldMapDefaults(
            AccountSubtype subtype,
            String mask,
            Double available,
            String expectedSubtype,
            String expectedMask,
            String expectedAvailable) {
        var request = mapper.toAccountUpsertRequest(connection(), ACCOUNT_ID, account(subtype, mask, 125.50, available));

        assertThat(request.getAccountSubtype()).isEqualTo(expectedSubtype);
        assertThat(request.getAccountMask()).isEqualTo(expectedMask);
        assertThat(request.getAvailableBalance()).isEqualByComparingTo(expectedAvailable);
    }

    @Test
    void toTransactionUpsertRequest_whenPlaidTransactionHasFullFields_shouldMapTransactionFields() {
        Transaction transaction = transaction(new PersonalFinanceCategory().detailed("FOOD_AND_DRINK_COFFEE"));

        var request = mapper.toTransactionUpsertRequest(connection(), TRANSACTION_ID, ACCOUNT_ID, transaction);

        assertThat(request.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(request.getUserId()).isEqualTo(USER_ID);
        assertThat(request.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(request.getAmount()).isEqualByComparingTo("15.75");
        assertThat(request.getIsoCurrencyCode()).isEqualTo("USD");
        assertThat(request.getTransactionName()).isEqualTo("Coffee Shop");
        assertThat(request.getTransactionType()).isEqualTo("place");
        assertThat(request.getDate()).isEqualTo(LocalDate.of(2026, 4, 17));
        assertThat(request.getPending()).isFalse();
        assertThat(request.getPaymentChannel()).isEqualTo("in store");
        assertThat(request.getDetailedCategoryCode()).isEqualTo("FOOD_AND_DRINK_COFFEE");
        assertThat(request.isActive()).isTrue();
    }

    @Test
    void toTransactionUpsertRequest_whenPersonalFinanceCategoryMissing_shouldUseOtherCategory() {
        var request = mapper.toTransactionUpsertRequest(connection(), TRANSACTION_ID, ACCOUNT_ID, transaction(null));

        assertThat(request.getDetailedCategoryCode()).isEqualTo("OTHER_OTHER");
    }

    private static Stream<Arguments> optionalAccountFieldCases() {
        return Stream.of(
                Arguments.of(null, null, null, "", "", "0"),
                Arguments.of(AccountSubtype.SAVINGS, "1234", 80.00, "savings", "1234", "80.0"));
    }

    private Connection connection() {
        return new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank");
    }

    private AccountBase account(AccountSubtype subtype, String mask, Double current, Double available) {
        return new AccountBase()
                .accountId("plaid-account-1")
                .name("Primary Checking")
                .type(AccountType.DEPOSITORY)
                .subtype(subtype)
                .mask(mask)
                .balances(new AccountBalance()
                        .current(current)
                        .available(available)
                        .isoCurrencyCode("USD"));
    }

    private Transaction transaction(PersonalFinanceCategory category) {
        return new Transaction()
                .transactionId("plaid-transaction-1")
                .accountId("plaid-account-1")
                .amount(15.75)
                .isoCurrencyCode("USD")
                .name("Coffee Shop")
                .transactionType(Transaction.TransactionTypeEnum.PLACE)
                .date(LocalDate.of(2026, 4, 17))
                .pending(false)
                .paymentChannel(Transaction.PaymentChannelEnum.IN_STORE)
                .personalFinanceCategory(category);
    }
}
