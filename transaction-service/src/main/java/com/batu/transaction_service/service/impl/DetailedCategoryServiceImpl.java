package com.batu.transaction_service.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.repository.DetailedCategoryRepository;
import com.batu.transaction_service.service.DetailedCategoryService;

@Service
public class DetailedCategoryServiceImpl implements DetailedCategoryService {

    private final DetailedCategoryRepository detailedCategoryRepository;

    public DetailedCategoryServiceImpl(DetailedCategoryRepository detailedCategoryRepository) {
        this.detailedCategoryRepository = detailedCategoryRepository;
    }

    @Override
    public TransactionDetailedCategory getByCategoryCode(String categoryCode) {
        return detailedCategoryRepository.findByCategoryCode(categoryCode)
                .orElseGet(() -> detailedCategoryRepository
                        .findByCategoryCode("OTHER_OTHER")
                        .orElseThrow(() -> new IllegalStateException(
                                "Default detailed category OTHER_OTHER is missing")));
    }

    @Override
    public TransactionDetailedCategory getById(UUID detailedCategoryId) {
        return detailedCategoryRepository.findById(detailedCategoryId).orElseThrow();
    }

}
