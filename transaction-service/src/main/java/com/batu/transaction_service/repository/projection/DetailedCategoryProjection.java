package com.batu.transaction_service.repository.projection;

public interface DetailedCategoryProjection {
    String getDisplayName();
    PrimaryCategoryProjection getTransactionPrimaryCategory();
}
