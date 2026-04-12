package com.batu.transaction_service.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.exception.ResourceNotFoundException;
import com.batu.transaction_service.exception.SyncStateException;
import com.batu.transaction_service.repository.DetailedCategoryRepository;
import com.batu.transaction_service.service.DetailedCategoryService;

@Service
public class DetailedCategoryServiceImpl implements DetailedCategoryService {

    private static final String FALLBACK_CATEGORY_CODE = "OTHER_OTHER";

    private final DetailedCategoryRepository detailedCategoryRepository;
    private final Map<String, TransactionDetailedCategory> categoryCache = new ConcurrentHashMap<>();

    public DetailedCategoryServiceImpl(DetailedCategoryRepository detailedCategoryRepository) {
        this.detailedCategoryRepository = detailedCategoryRepository;
    }

    @Override
    public TransactionDetailedCategory getByCategoryCode(String categoryCode) {
        return categoryCache.computeIfAbsent(categoryCode, this::loadCategory);
    }

    @Override
    public TransactionDetailedCategory getById(UUID detailedCategoryId) {
        return detailedCategoryRepository.findById(detailedCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Detailed category with id " + detailedCategoryId + " not found"));
    }

    private TransactionDetailedCategory loadCategory(String categoryCode) {
        return detailedCategoryRepository.findByCategoryCodeWithPrimaryCategory(categoryCode)
                .orElseGet(() -> detailedCategoryRepository
                        .findByCategoryCodeWithPrimaryCategory(FALLBACK_CATEGORY_CODE)
                        .orElseThrow(() -> new SyncStateException(
                                "Default detailed category OTHER_OTHER is missing")));
    }

}
