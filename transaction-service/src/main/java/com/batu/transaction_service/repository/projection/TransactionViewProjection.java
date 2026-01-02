package com.batu.transaction_service.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface TransactionViewProjection {
    UUID getTransactionId();

    BigDecimal getAmount();

    String getTransactionName();

    String getIsoCurrencyCode();

    DetailedCategoryProjection getDetailedCategory();
}
