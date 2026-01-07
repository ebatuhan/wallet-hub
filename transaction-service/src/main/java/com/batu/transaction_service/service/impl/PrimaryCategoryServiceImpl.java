package com.batu.transaction_service.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.PrimaryCategoryRepository;
import com.batu.transaction_service.service.PrimaryCategoryService;

@Service
public class PrimaryCategoryServiceImpl implements PrimaryCategoryService {
    private final PrimaryCategoryRepository primaryCategoryRepository;

    public PrimaryCategoryServiceImpl(PrimaryCategoryRepository primaryCategoryRepository) {
        this.primaryCategoryRepository = primaryCategoryRepository;
    }

    @Override
    public TransactionPrimaryCategory getByCategoryCode(String categoryCode) {
        return primaryCategoryRepository.findByCategoryCode(categoryCode).orElseThrow();
    }

    @Override
    public TransactionPrimaryCategory getById(UUID primaryCategoryId) {
        return primaryCategoryRepository.findById(primaryCategoryId).orElseThrow();
    }
}
