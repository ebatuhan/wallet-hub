package com.batu.transaction_service.repository.projection;

import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionProjection {
    UUID getTransactionId();
    BigDecimal getAmount();
    String getTransactionName();
    String getIsoCurrencyCode();

    // SpEL to flatten the nested relationship
    @Value("#{target.detailedCategory.displayName}")
    String getDetailedCategoryName();

    // Deeply nested relationship
    @Value("#{target.detailedCategory.transactionPrimaryCategory.displayName}")
    String getCategoryDisplayName();
}