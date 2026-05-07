package com.batu.transaction_service.service;

import java.util.UUID;

import com.batu.transaction_service.entity.TransactionDetailedCategory;

public interface DetailedCategoryService {

    TransactionDetailedCategory getByCategoryCode(String categoryCode);
    TransactionDetailedCategory getById(UUID detailedCategoryId);

}
